package org.sabha.attendance.applicationservice;

import java.time.LocalDate;
import java.util.UUID;

/**
 * One Occurrence of a proxied Sabha that a Nirikshak may shape (Slice 14):
 * its effective (possibly rescheduled) date, current state, and effective
 * (possibly overridden) venue. The web toolkit lists these so the Nirikshak can
 * cancel, reschedule, or set a venue override on a chosen Occurrence.
 */
public record ProxyOccurrenceItem(
        UUID id,
        LocalDate effectiveDate,
        String state,
        String venue) {
}
