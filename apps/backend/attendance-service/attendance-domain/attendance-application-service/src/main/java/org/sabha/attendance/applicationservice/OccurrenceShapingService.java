package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import org.sabha.attendance.domain.Occurrence;
import org.sabha.common.AuthorizedAction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sanchalak-only Sabha-shaping operations on an Occurrence (Slice 5 / ADR-0001):
 * cancel, revert, reschedule, and venue-override.
 *
 * <p>This service owns only the shaping vocabulary and its preconditions — the
 * cancel reason requirement and the revert grace window. The load-authorize-
 * mutate-save-audit-publish orchestration (shared with reopen and the cron) lives
 * in {@link OccurrenceWriter}; the Sanchalak-vs-Sah-Sanchalak authority is the
 * {@link AuthorizationEngine}'s call.</p>
 */
@Service
public class OccurrenceShapingService {

    private final OccurrenceWriter writer;
    private final EffectiveSlotResolver slotResolver;
    private final Clock clock;
    private final Duration revertGrace;

    public OccurrenceShapingService(
            OccurrenceWriter writer,
            EffectiveSlotResolver slotResolver,
            Clock clock,
            @Value("${sabha.attendance.revert.grace:PT24H}") Duration revertGrace) {
        this.writer = writer;
        this.slotResolver = slotResolver;
        this.clock = clock;
        this.revertGrace = revertGrace;
    }

    @Transactional
    public void cancel(UUID keycloakSubject, UUID occurrenceId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new CancellationReasonRequiredException(occurrenceId);
        }
        writer.transition(occurrenceId, TransitionActor.user(keycloakSubject, AuthorizedAction.CANCEL),
                OccurrenceAction.CANCEL, reason, Occurrence::cancel);
    }

    @Transactional
    public void revert(UUID keycloakSubject, UUID occurrenceId) {
        writer.transition(occurrenceId, TransitionActor.user(keycloakSubject, AuthorizedAction.CANCEL),
                OccurrenceAction.REVERT, null, occurrence -> {
                    requireWithinRevertWindow(occurrence);
                    occurrence.revert();
                });
    }

    @Transactional
    public void reschedule(UUID keycloakSubject, UUID occurrenceId,
                           LocalDate newDate, LocalTime newStartTime, LocalTime newEndTime) {
        writer.transition(occurrenceId, TransitionActor.user(keycloakSubject, AuthorizedAction.RESCHEDULE),
                OccurrenceAction.RESCHEDULE, null,
                occurrence -> occurrence.reschedule(newDate, newStartTime, newEndTime));
    }

    @Transactional
    public void overrideVenue(UUID keycloakSubject, UUID occurrenceId, String venue) {
        writer.transition(occurrenceId, TransitionActor.user(keycloakSubject, AuthorizedAction.VENUE_OVERRIDE),
                OccurrenceAction.OVERRIDE_VENUE, null,
                occurrence -> occurrence.overrideVenue(venue));
    }

    /**
     * The revert grace window closes {@code revertGrace} after the Occurrence's
     * Effective Slot ends. An Occurrence with no resolvable slot has no cutoff to
     * measure against, so the revert is allowed.
     */
    private void requireWithinRevertWindow(Occurrence occurrence) {
        Optional<Instant> cutoff = slotResolver.resolve(occurrence)
                .map(slot -> slot.endsAt().plus(revertGrace));
        if (cutoff.isPresent() && clock.instant().isAfter(cutoff.get())) {
            throw new RevertWindowExpiredException(occurrence.id(), cutoff.get());
        }
    }
}
