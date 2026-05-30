package org.sabha.attendance.applicationservice;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.sabha.attendance.domain.OccurrenceState;

/**
 * Read-side projection for the Sanchalak's mobile occurrence-control screen: the
 * Occurrence they can currently shape (cancel / reschedule / venue-override per
 * ADR-0001), along with any shaping already applied. Unlike {@link CurrentRoster}
 * — which surfaces the {@code OPEN_FOR_MARKING} Occurrence for day-of marking —
 * this surfaces a {@code SCHEDULED} / {@code RESCHEDULED} / {@code CANCELLED}
 * Occurrence, the states in which shaping (or revert) is still possible.
 */
public record CurrentOccurrence(
        UUID id,
        UUID sabhaId,
        LocalDate date,
        OccurrenceState state,
        String venueOverride,
        LocalDate rescheduledDate,
        LocalTime rescheduledStartTime,
        LocalTime rescheduledEndTime) {
}
