package org.sabha.common;

import java.util.Optional;
import java.util.UUID;

/**
 * One row of the sabha-owned {@code sabhas} table, as seen across the
 * bounded-context seam (ADR-0019, ADR-0033). The single value type returned by
 * every read on {@link SabhaFacts} — by id, by {@code (Kshetra, kind)}, and by
 * the weekly catalog — so no window on the table forces a caller to over-fetch
 * to serve another window's shape.
 *
 * <p>Scope, demographic and track are carried as the existing {@link SabhaScope}
 * string tokens rather than the sabha context's enums, and the schedule shape as
 * its plain column token, so sabha's {@code ScheduleShape} enum still does not
 * cross the seam.</p>
 *
 * <p>{@code standingSlot} is present exactly when {@code scheduleShape} is
 * {@code WEEKLY_RECURRING}; a monthly-ad-hoc Sabha has no standing slot, and its
 * Occurrences carry their own (ADR-0012).</p>
 *
 * @param kindRetired whether this Sabha's {@code (demographic, track)} kind has
 *                    been soft-retired (ADR-0026). {@code false} for the seed
 *                    Sabhas that carry only the denormalized kind token and no
 *                    {@code sabha_kind_id}, which is why the adapter reads the
 *                    registry through a LEFT JOIN.
 */
public record SabhaFact(
        UUID sabhaId,
        SabhaScope scope,
        String scheduleShape,
        Optional<SabhaSchedule> standingSlot,
        boolean kindRetired) {

    /** The {@code schedule_shape} token of a Sabha whose Occurrences follow a standing weekly slot. */
    public static final String WEEKLY_RECURRING = "WEEKLY_RECURRING";

    /** The {@code schedule_shape} token of a Sabha whose Occurrences each carry their own slot. */
    public static final String MONTHLY_AD_HOC = "MONTHLY_AD_HOC";

    /**
     * Enforces the one invariant that lets the three windows on {@link SabhaFacts}
     * share this type: the standing slot is present exactly when the shape is
     * weekly. Without it the record would carry emptiness meaning two different
     * things to two different callers, which is the fat-interface failure ADR-0027
     * §1 warns about and ADR-0033 records as its reversal condition.
     */
    public SabhaFact {
        if (standingSlot == null) {
            throw new IllegalArgumentException("standingSlot must be an Optional, never null");
        }
        if (standingSlot.isPresent() != WEEKLY_RECURRING.equals(scheduleShape)) {
            throw new IllegalArgumentException(
                    "a standing slot is present exactly for " + WEEKLY_RECURRING
                            + " Sabhas; got shape " + scheduleShape
                            + " with slot " + standingSlot);
        }
    }

    /** A weekly-recurring Sabha, which always has a standing slot. */
    public static SabhaFact weekly(UUID sabhaId, SabhaScope scope, SabhaSchedule standingSlot,
                                   boolean kindRetired) {
        return new SabhaFact(sabhaId, scope, WEEKLY_RECURRING, Optional.of(standingSlot), kindRetired);
    }

    /** A monthly-ad-hoc Sabha, which never has one. */
    public static SabhaFact monthlyAdHoc(UUID sabhaId, SabhaScope scope, boolean kindRetired) {
        return new SabhaFact(sabhaId, scope, MONTHLY_AD_HOC, Optional.empty(), kindRetired);
    }

    /** Whether this Sabha is monthly-ad-hoc — the shape that admits manual Occurrence creation. */
    public boolean isMonthlyAdHoc() {
        return MONTHLY_AD_HOC.equals(scheduleShape);
    }
}
