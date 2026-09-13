package org.sabha.common;

import java.util.Optional;
import java.util.UUID;

/**
 * Cross-context read port (ADR-0019): a Sabha's schedule-shape token
 * ({@code WEEKLY_RECURRING} or {@code MONTHLY_AD_HOC}). The attendance context
 * consults it to guard that manual Occurrence creation targets a monthly-ad-hoc
 * Sabha, and to drive the compliance nudge (ADR-0012). Empty when no such Sabha
 * exists. The implementation reads the sabha-owned {@code sabhas} table and lives
 * in {@code sabha-data-access}.
 */
public interface SabhaShapeLookup {

    Optional<String> scheduleShapeOf(UUID sabhaId);
}
