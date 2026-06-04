package org.sabha.attendance.applicationservice;

import java.util.UUID;

/**
 * A monthly-ad-hoc Sabha the calling Sanchalak presides over, paired with the
 * compliance nudge flag (ADR-0012): {@code needsOccurrence} is true when this
 * month has no Occurrence yet and the month is past its midpoint. Drives the
 * mobile's Occurrence-create entry point and its soft compliance warning.
 */
public record MonthlySabha(UUID sabhaId, String sabhaKind, String standingVenue, boolean needsOccurrence) {
}
