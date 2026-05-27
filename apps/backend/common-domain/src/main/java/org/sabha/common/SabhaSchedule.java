package org.sabha.common;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Cross-context value carrying a Sabha's standing weekly schedule (ADR-0019).
 *
 * <p>Owned by the sabha context; consumed by the attendance context's cron
 * scanners to compute each Occurrence's scheduled start/end Instant. The
 * authoritative store remains the Sabha aggregate in sabha-domain-core; this
 * record is the wire shape returned by {@link SabhaScheduleLookup}.</p>
 */
public record SabhaSchedule(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
}
