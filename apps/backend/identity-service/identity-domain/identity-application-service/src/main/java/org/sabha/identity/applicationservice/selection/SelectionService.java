package org.sabha.identity.applicationservice.selection;

import java.time.Clock;
import java.util.UUID;

import org.sabha.common.CallerAuthority;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.SabhaScope;
import org.sabha.common.SabhaFact;
import org.sabha.common.SabhaFacts;
import org.sabha.identity.domain.NoSelectiveSabhaException;
import org.sabha.identity.domain.NominationNotFoundException;
import org.sabha.identity.domain.NotSelectedException;
import org.sabha.identity.domain.PersonNotOnRosterException;
import org.sabha.identity.domain.SelectionNomination;
import org.sabha.identity.domain.SelectiveTrack;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BSS/YSS selection workflow orchestrator (ADR-0006) — the deep module behind the
 * nominate → approve/reject → deselect lifecycle:
 *
 * <pre>
 *   nominate(subject, personId, regularSabhaId) -> nominationId   (Regular Sanchalak)
 * </pre>
 *
 * <p>It hides the selective-Sabha derivation, the Roster/authority gates, and the
 * audit-bearing nomination record. The Person's Regular Home Sabha is never
 * touched — selection is additive (ADR-0006).</p>
 *
 * <p>Both of its authority reads come off the caller parameter since ADR-0032.
 * The nomination gate is {@link CallerAuthority#runsSabha(UUID)}, the same single
 * call {@code HomeSabhaTransferService.initiate} makes — those two were byte-for-
 * byte duplicates of each other. The decision gate is {@link
 * CallerAuthority#holdsNirdeshakIn(UUID, String)}, which is also what the
 * appointment engine and the Sabha define/delete paths ask: four sites that
 * reached two different ports now reach one method, so their agreement is no
 * longer an invariant anybody has to assert.</p>
 */
@Service
public class SelectionService {

    private final SelectionRoster roster;
    private final SabhaFacts sabhas;
    private final SelectionRepository nominations;
    private final DomainEventPublisher events;
    private final Clock clock;

    public SelectionService(
            SelectionRoster roster,
            SabhaFacts sabhas,
            SelectionRepository nominations,
            DomainEventPublisher events,
            Clock clock) {
        this.roster = roster;
        this.sabhas = sabhas;
        this.nominations = nominations;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public UUID nominate(CallerAuthority caller, UUID personId, UUID regularSabhaId) {
        UUID nominatorUserId = caller.userId().value();

        if (!caller.runsSabha(regularSabhaId)) {
            throw new NominationNotAuthorizedException(nominatorUserId, regularSabhaId);
        }

        if (!roster.isOnRoster(personId, regularSabhaId)) {
            throw new PersonNotOnRosterException(personId, regularSabhaId);
        }

        SabhaScope scope = sabhas.of(regularSabhaId).orElseThrow().scope();
        String selectiveTrack = SelectiveTrack.forDemographic(scope.demographic());
        UUID selectiveSabhaId = sabhas
                .selectiveIn(scope.kshetraId(), scope.demographic(), selectiveTrack)
                .map(SabhaFact::sabhaId)
                .orElseThrow(() -> new NoSelectiveSabhaException(
                        scope.kshetraId(), scope.demographic(), selectiveTrack));

        if (roster.isOnRoster(personId, selectiveSabhaId)) {
            throw new AlreadySelectedException(personId, selectiveSabhaId);
        }
        if (nominations.hasPendingFor(personId, selectiveTrack)) {
            throw new DuplicateNominationException(personId, selectiveTrack);
        }

        SelectionNomination nomination = SelectionNomination.nominate(
                UUID.randomUUID(), personId, regularSabhaId, selectiveSabhaId,
                scope.kshetraId(), scope.demographic(), selectiveTrack, nominatorUserId,
                clock.instant());
        nominations.save(nomination);
        events.publishAll(nomination.pullDomainEvents());
        return nomination.id();
    }

    @Transactional
    public void approve(CallerAuthority caller, UUID nominationId) {
        UUID deciderUserId = caller.userId().value();
        SelectionNomination nomination = nominations.findById(nominationId)
                .orElseThrow(() -> new NominationNotFoundException(nominationId));
        requireNirdeshak(caller, nomination.kshetraId(), nomination.demographic());

        nomination.approve(deciderUserId, clock.instant());
        roster.addHomeSabha(nomination.personId(), nomination.selectiveSabhaId());
        nominations.save(nomination);
        events.publishAll(nomination.pullDomainEvents());
    }

    @Transactional
    public void reject(CallerAuthority caller, UUID nominationId, String reason) {
        UUID deciderUserId = caller.userId().value();
        SelectionNomination nomination = nominations.findById(nominationId)
                .orElseThrow(() -> new NominationNotFoundException(nominationId));
        requireNirdeshak(caller, nomination.kshetraId(), nomination.demographic());

        nomination.reject(deciderUserId, reason, clock.instant());
        nominations.save(nomination);
        events.publishAll(nomination.pullDomainEvents());
    }

    @Transactional
    public void deselect(CallerAuthority caller, UUID personId, UUID selectiveSabhaId) {
        UUID deciderUserId = caller.userId().value();
        SabhaScope scope = sabhas.of(selectiveSabhaId).orElseThrow().scope();
        requireNirdeshak(caller, scope.kshetraId(), scope.demographic());

        SelectionNomination nomination = nominations.findApproved(personId, selectiveSabhaId)
                .orElseThrow(() -> new NotSelectedException(personId, selectiveSabhaId));
        nomination.deselect(deciderUserId, clock.instant());
        roster.removeHomeSabha(personId, selectiveSabhaId);
        nominations.save(nomination);
        events.publishAll(nomination.pullDomainEvents());
    }

    /**
     * The demographic Nirdeshak is the sole decision authority for a selection,
     * track-shared across Regular and BSS/YSS (ADR-0006). Approve/reject resolve
     * the (Kshetra, demographic) from the nomination; deselect resolves it from
     * the selective Sabha's scope.
     */
    private void requireNirdeshak(CallerAuthority caller, UUID kshetraId, String demographic) {
        if (!caller.holdsNirdeshakIn(kshetraId, demographic)) {
            throw new SelectionDecisionNotAuthorizedException(caller.userId().value());
        }
    }
}
