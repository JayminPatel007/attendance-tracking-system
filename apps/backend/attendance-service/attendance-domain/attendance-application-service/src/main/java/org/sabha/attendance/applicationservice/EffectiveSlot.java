package org.sabha.attendance.applicationservice;

import java.time.Instant;

/**
 * An Occurrence's <em>Effective Slot</em> (see CONTEXT.md): when it actually
 * starts and ends, in absolute time, after per-Occurrence overrides have been
 * applied over the Sabha's standing schedule.
 */
public record EffectiveSlot(Instant startsAt, Instant endsAt) {
}
