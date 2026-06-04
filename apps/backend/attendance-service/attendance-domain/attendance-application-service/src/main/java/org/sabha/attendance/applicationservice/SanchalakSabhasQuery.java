package org.sabha.attendance.applicationservice;

import java.util.List;
import java.util.UUID;

/**
 * Read-side port: the monthly-ad-hoc Sabhas a given Sanchalak ({@code users.id})
 * presides over. The mobile needs this to discover which Sabha to create this
 * month's Occurrence against — a monthly Sabha with no Occurrence yet has no
 * roster or current Occurrence to derive its id from (ADR-0012). The adapter
 * lives in {@code attendance-data-access} and joins {@code role_assignments}
 * (identity) with {@code sabhas} — a legitimate cross-context <i>read</i>
 * projection (ADR-0019).
 */
public interface SanchalakSabhasQuery {

    List<MonthlyAdHocSabha> monthlyAdHocFor(UUID sanchalakUserId);

    /** A monthly-ad-hoc Sabha the caller presides over; {@code sabhaKind} is the denormalized token. */
    record MonthlyAdHocSabha(UUID sabhaId, String sabhaKind, String standingVenue) {
    }
}
