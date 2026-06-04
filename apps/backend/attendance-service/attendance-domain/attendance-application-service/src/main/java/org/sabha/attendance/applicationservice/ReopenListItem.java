package org.sabha.attendance.applicationservice;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A row in the web Occurrence-reopen list (Slice 13): an Occurrence sitting in a
 * Kshetra/demographic the caller may reopen, with enough context to render the
 * two-pane screen. {@code reopened} and {@code lastReopenReason} are derived from
 * the state-transition audit log (the "reopened" badge has no denormalized
 * column); {@code state} is the {@link org.sabha.attendance.domain.OccurrenceState}
 * name and {@code date} is the effective (rescheduled-or-standing) date.
 */
public record ReopenListItem(
        UUID occurrenceId,
        LocalDate date,
        String state,
        String kshetraName,
        String sabhaKind,
        String venue,
        boolean reopened,
        String lastReopenReason) {
}
