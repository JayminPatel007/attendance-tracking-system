package org.sabha.sabha.domain;

/**
 * The schedule-shape discriminator of a {@link Sabha} (ADR-0012).
 *
 * <ul>
 *   <li>{@link #WEEKLY_RECURRING} — a fixed {@code (dayOfWeek, startTime, endTime)}
 *       slot; the system auto-materializes future Occurrences on a rolling window.</li>
 *   <li>{@link #MONTHLY_AD_HOC} — no standing day/time; the Sanchalak creates each
 *       Occurrence manually (one per calendar month).</li>
 * </ul>
 */
public enum ScheduleShape {
    WEEKLY_RECURRING,
    MONTHLY_AD_HOC
}
