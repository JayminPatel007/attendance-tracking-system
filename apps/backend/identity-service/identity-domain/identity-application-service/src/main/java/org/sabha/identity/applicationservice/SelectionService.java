package org.sabha.identity.applicationservice;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;

import org.sabha.common.CallerResolver;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.Role;
import org.sabha.common.RoleAssignmentLookup;
import org.sabha.common.SabhaScope;
import org.sabha.common.StructuralHierarchyLookup;
import org.sabha.identity.domain.NominationNotFoundException;
import org.sabha.identity.domain.NoSelectiveSabhaException;
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
 */
@Service
public class SelectionService {

    private final CallerResolver callerResolver;
    private final RoleAssignmentLookup roleAssignments;
    private final SelectionRoster roster;
    private final StructuralHierarchyLookup hierarchy;
    private final AppointerAuthorityLookup authority;
    private final SelectionRepository nominations;
    private final DomainEventPublisher events;
    private final Clock clock;

    public SelectionService(
            CallerResolver callerResolver,
            RoleAssignmentLookup roleAssignments,
            SelectionRoster roster,
            StructuralHierarchyLookup hierarchy,
            AppointerAuthorityLookup authority,
            SelectionRepository nominations,
            DomainEventPublisher events,
            Clock clock) {
        this.callerResolver = callerResolver;
        this.roleAssignments = roleAssignments;
        this.roster = roster;
        this.hierarchy = hierarchy;
        this.authority = authority;
        this.nominations = nominations;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public UUID nominate(UUID keycloakSubject, UUID personId, UUID regularSabhaId) {
        UUID nominatorUserId = resolveCaller(keycloakSubject);

        Set<Role> roles = roleAssignments.rolesForUserOnSabha(nominatorUserId, regularSabhaId);
        if (!roles.contains(Role.SANCHALAK) && !roles.contains(Role.SAH_SANCHALAK)) {
            throw new NominationNotAuthorizedException(nominatorUserId, regularSabhaId);
        }

        if (!roster.isOnRoster(personId, regularSabhaId)) {
            throw new PersonNotOnRosterException(personId, regularSabhaId);
        }

        SabhaScope scope = hierarchy.sabhaScope(regularSabhaId).orElseThrow();
        String selectiveTrack = SelectiveTrack.forDemographic(scope.demographic());
        UUID selectiveSabhaId = hierarchy
                .selectiveSabhaIn(scope.kshetraId(), scope.demographic(), selectiveTrack)
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
    public void approve(UUID keycloakSubject, UUID nominationId) {
        UUID deciderUserId = resolveCaller(keycloakSubject);
        SelectionNomination nomination = nominations.findById(nominationId)
                .orElseThrow(() -> new NominationNotFoundException(nominationId));
        requireNirdeshak(deciderUserId, nomination.kshetraId(), nomination.demographic());

        nomination.approve(deciderUserId, clock.instant());
        roster.addHomeSabha(nomination.personId(), nomination.selectiveSabhaId());
        nominations.save(nomination);
        events.publishAll(nomination.pullDomainEvents());
    }

    @Transactional
    public void reject(UUID keycloakSubject, UUID nominationId, String reason) {
        UUID deciderUserId = resolveCaller(keycloakSubject);
        SelectionNomination nomination = nominations.findById(nominationId)
                .orElseThrow(() -> new NominationNotFoundException(nominationId));
        requireNirdeshak(deciderUserId, nomination.kshetraId(), nomination.demographic());

        nomination.reject(deciderUserId, reason, clock.instant());
        nominations.save(nomination);
        events.publishAll(nomination.pullDomainEvents());
    }

    @Transactional
    public void deselect(UUID keycloakSubject, UUID personId, UUID selectiveSabhaId) {
        UUID deciderUserId = resolveCaller(keycloakSubject);
        SabhaScope scope = hierarchy.sabhaScope(selectiveSabhaId).orElseThrow();
        requireNirdeshak(deciderUserId, scope.kshetraId(), scope.demographic());

        SelectionNomination nomination = nominations.findApproved(personId, selectiveSabhaId)
                .orElseThrow(() -> new NotSelectedException(personId, selectiveSabhaId));
        nomination.deselect(deciderUserId, clock.instant());
        roster.removeHomeSabha(personId, selectiveSabhaId);
        nominations.save(nomination);
        events.publishAll(nomination.pullDomainEvents());
    }

    private UUID resolveCaller(UUID keycloakSubject) {
        return callerResolver.resolveUserId(keycloakSubject)
                .orElseThrow(() -> new CallerUnknownException(keycloakSubject));
    }

    /**
     * The demographic Nirdeshak is the sole decision authority for a selection,
     * track-shared across Regular and BSS/YSS (ADR-0006). Approve/reject resolve
     * the (Kshetra, demographic) from the nomination; deselect resolves it from
     * the selective Sabha's scope.
     */
    private void requireNirdeshak(UUID deciderUserId, UUID kshetraId, String demographic) {
        if (!authority.holdsNirdeshak(deciderUserId, kshetraId, demographic)) {
            throw new SelectionDecisionNotAuthorizedException(deciderUserId);
        }
    }
}
