package org.sabha.common;

import java.util.Set;

/**
 * Actions whose authority the Authorization Engine arbitrates (ADR-0001).
 *
 * <p>{@link #SABHA_SHAPING_ACTIONS} is the set restricted to the Sanchalak of a
 * Sabha (Sah-Sanchalak explicitly excluded): deciding whether and when a Sabha
 * Occurrence happens, and where. Day-of marking/directory actions are shared
 * with the Sah-Sanchalak and are not part of this set.</p>
 */
public enum AuthorizedAction {
    CANCEL,
    RESCHEDULE,
    VENUE_OVERRIDE,
    SCHEDULE_CHANGE;

    public static final Set<AuthorizedAction> SABHA_SHAPING_ACTIONS =
            Set.of(CANCEL, RESCHEDULE, VENUE_OVERRIDE, SCHEDULE_CHANGE);
}
