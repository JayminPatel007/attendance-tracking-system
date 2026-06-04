package org.sabha.common;

import java.util.Set;

/**
 * Actions whose authority the Authorization Engine arbitrates.
 *
 * <p>{@link #SABHA_SHAPING_ACTIONS} (ADR-0001) is the set restricted to the
 * Sanchalak of a Sabha (Sah-Sanchalak explicitly excluded): deciding whether and
 * when a Sabha Occurrence happens, and where — including manually creating a
 * monthly-ad-hoc Sabha's Occurrence (ADR-0012). Day-of marking/directory actions
 * are shared with the Sah-Sanchalak and are not part of this set.</p>
 *
 * <p>The structural-creation actions (ADR-0009) name the tier each create acts
 * on; their authority is arbitrated by the sabha context's structural
 * Authorization Engine (MK for City/Zone/Sabha-Kind; the Zone's Sanyojak for
 * Kshetra).</p>
 */
public enum AuthorizedAction {
    CANCEL,
    RESCHEDULE,
    VENUE_OVERRIDE,
    SCHEDULE_CHANGE,
    CREATE_OCCURRENCE,
    CREATE_CITY,
    CREATE_ZONE,
    CREATE_SABHA_KIND,
    CREATE_KSHETRA,
    CREATE_SABHA,
    APPOINT_ROLE,
    REOPEN;

    public static final Set<AuthorizedAction> SABHA_SHAPING_ACTIONS =
            Set.of(CANCEL, RESCHEDULE, VENUE_OVERRIDE, SCHEDULE_CHANGE, CREATE_OCCURRENCE);
}
