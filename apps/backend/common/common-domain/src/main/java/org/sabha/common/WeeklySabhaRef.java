package org.sabha.common;

/**
 * A weekly-recurring Sabha and its standing schedule (ADR-0012, ADR-0019). The
 * wire shape returned by {@link WeeklySabhaCatalog} for the attendance context's
 * materialization cron to iterate.
 */
public record WeeklySabhaRef(java.util.UUID sabhaId, SabhaSchedule schedule) {
}
