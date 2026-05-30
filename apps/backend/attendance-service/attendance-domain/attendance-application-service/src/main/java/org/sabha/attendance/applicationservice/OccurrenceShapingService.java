package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.sabha.attendance.domain.Occurrence;
import org.sabha.attendance.domain.OccurrenceState;
import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.AuthorizedAction;
import org.sabha.common.CallerResolver;
import org.sabha.common.ConcurrentModificationException;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.OptimisticLockException;
import org.sabha.common.SabhaSchedule;
import org.sabha.common.SabhaScheduleLookup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sanchalak-only Sabha-shaping operations on an Occurrence (Slice 5 / ADR-0001):
 * cancel, revert, reschedule, and venue-override.
 *
 * <p>Each operation resolves the calling user, asks the {@link
 * AuthorizationEngine} whether they may shape this Sabha (Sanchalak yes,
 * Sah-Sanchalak no), applies the mutation to the aggregate, persists it,
 * appends a row to the state-transition audit log (Slice 3), and publishes any
 * registered domain events. The load-mutate-save cycle is retried on
 * optimistic-lock conflict, mirroring {@link OccurrenceStateMachine}.</p>
 */
@Service
public class OccurrenceShapingService {

    private static final int MAX_OPTIMISTIC_LOCK_ATTEMPTS = 3;

    private final CallerResolver callerResolver;
    private final AuthorizationEngine authorization;
    private final OccurrenceRepository occurrences;
    private final OccurrenceStateTransitionRepository transitions;
    private final DomainEventPublisher events;
    private final SabhaScheduleLookup scheduleLookup;
    private final Clock clock;
    private final Duration revertGrace;

    public OccurrenceShapingService(
            CallerResolver callerResolver,
            AuthorizationEngine authorization,
            OccurrenceRepository occurrences,
            OccurrenceStateTransitionRepository transitions,
            DomainEventPublisher events,
            SabhaScheduleLookup scheduleLookup,
            Clock clock,
            @Value("${sabha.attendance.revert.grace:PT24H}") Duration revertGrace) {
        this.callerResolver = callerResolver;
        this.authorization = authorization;
        this.occurrences = occurrences;
        this.transitions = transitions;
        this.events = events;
        this.scheduleLookup = scheduleLookup;
        this.clock = clock;
        this.revertGrace = revertGrace;
    }

    @Transactional
    public void cancel(UUID keycloakSubject, UUID occurrenceId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new CancellationReasonRequiredException(occurrenceId);
        }
        shape(keycloakSubject, occurrenceId, AuthorizedAction.CANCEL,
                OccurrenceAction.CANCEL, reason, Occurrence::cancel);
    }

    @Transactional
    public void revert(UUID keycloakSubject, UUID occurrenceId) {
        shape(keycloakSubject, occurrenceId, AuthorizedAction.CANCEL,
                OccurrenceAction.REVERT, null, occurrence -> {
                    requireWithinRevertWindow(occurrence);
                    occurrence.revert();
                });
    }

    @Transactional
    public void reschedule(UUID keycloakSubject, UUID occurrenceId,
                           LocalDate newDate, LocalTime newStartTime, LocalTime newEndTime) {
        shape(keycloakSubject, occurrenceId, AuthorizedAction.RESCHEDULE,
                OccurrenceAction.RESCHEDULE, null,
                occurrence -> occurrence.reschedule(newDate, newStartTime, newEndTime));
    }

    @Transactional
    public void overrideVenue(UUID keycloakSubject, UUID occurrenceId, String venue) {
        shape(keycloakSubject, occurrenceId, AuthorizedAction.VENUE_OVERRIDE,
                OccurrenceAction.OVERRIDE_VENUE, null,
                occurrence -> occurrence.overrideVenue(venue));
    }

    private void shape(UUID keycloakSubject, UUID occurrenceId, AuthorizedAction authAction,
                       OccurrenceAction auditAction, String reason, Consumer<Occurrence> mutation) {
        UUID userId = callerResolver.resolveUserId(keycloakSubject)
                .orElseThrow(() -> new CallerUnknownException(keycloakSubject));

        OptimisticLockException lastConflict = null;
        for (int attempt = 0; attempt < MAX_OPTIMISTIC_LOCK_ATTEMPTS; attempt++) {
            Occurrence occurrence = occurrences.findById(occurrenceId)
                    .orElseThrow(() -> new OccurrenceNotFoundException(occurrenceId));

            if (!authorization.canUserDo(userId, authAction, occurrence.sabhaId())) {
                throw new AuthorizationDeniedException(userId, authAction);
            }

            OccurrenceState from = occurrence.state();
            mutation.accept(occurrence);
            OccurrenceState to = occurrence.state();

            try {
                occurrences.save(occurrence);
            } catch (OptimisticLockException retry) {
                lastConflict = retry;
                continue;
            }

            transitions.append(new OccurrenceStateTransition(
                    UUID.randomUUID(), occurrenceId, from, to, auditAction,
                    ActorKind.USER, userId, reason, clock.instant()));
            events.publishAll(occurrence.pullDomainEvents());
            return;
        }
        throw new ConcurrentModificationException(occurrenceId, lastConflict);
    }

    private void requireWithinRevertWindow(Occurrence occurrence) {
        Optional<SabhaSchedule> schedule = scheduleLookup.findSchedule(occurrence.sabhaId());
        if (schedule.isEmpty()) {
            return;
        }
        Instant scheduledEnd = ZonedDateTime.of(
                occurrence.date(), schedule.get().endTime(), clock.getZone()).toInstant();
        Instant cutoff = scheduledEnd.plus(revertGrace);
        if (clock.instant().isAfter(cutoff)) {
            throw new RevertWindowExpiredException(occurrence.id(), cutoff);
        }
    }
}
