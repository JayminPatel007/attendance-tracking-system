package org.sabha.common;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Cross-context port (ADR-0019) for creating a Sabha from the identity context's
 * single-transaction Sabha-definition flow (Slice 12 / ADR-0012). The Sabha
 * aggregate and its schedule-shape invariants are owned by the sabha context, so
 * the implementation lives in {@code sabha-data-access}; this port lives in
 * common-domain so identity can provision a Sabha (and then appoint its Sanchalak
 * in the same transaction) without depending on sabha's domain types.
 *
 * <p>Schedule-shape stays explicit through two create methods rather than a shared
 * discriminator, keeping this seam free of sabha's {@code ScheduleShape} enum.</p>
 *
 * <p><strong>This is a command, not a lookup, and that is why it kept its name and
 * its shape when the sabha-owned reads were re-partitioned by subject into
 * {@link SabhaFacts} and {@link StructuralParentage} (ADR-0033).</strong> Commands
 * do not consolidate with reads. Its two reads — {@link #demographicOfKind} and
 * {@link #isKindRetired} — key on a Sabha <em>Kind</em>, the thing being created
 * <em>by</em>, not on a Sabha; the mirror-image question "is <em>this Sabha's</em>
 * kind retired" belongs to {@code SabhaFacts} and is honestly a different question
 * from a different caller (ADR-0026, ADR-0033 fact 3).</p>
 */
public interface SabhaProvisioning {

    /**
     * The demographic token of a registered Sabha Kind, used by the definition
     * flow to authorize the caller as Nirdeshak over (Kshetra, demographic).
     * Empty when no such kind exists.
     */
    Optional<String> demographicOfKind(UUID sabhaKindId);

    /**
     * Whether the given Sabha Kind has been soft-retired (ADR-0026). The
     * definition flow rejects creating a new Sabha of a retired kind. Defaulted to
     * {@code false} so test fakes that predate soft-retire need not implement it;
     * the JDBC adapter overrides.
     */
    default boolean isKindRetired(UUID sabhaKindId) {
        return false;
    }

    /** Creates a weekly-recurring Sabha with its standing slot; returns its id. */
    UUID createWeekly(UUID kshetraId, UUID sabhaKindId, DayOfWeek dayOfWeek,
                      LocalTime startTime, LocalTime endTime, String standingVenue, UUID createdBy);

    /** Creates a monthly-ad-hoc Sabha (no standing slot); returns its id. */
    UUID createMonthlyAdHoc(UUID kshetraId, UUID sabhaKindId, String standingVenue, UUID createdBy);
}
