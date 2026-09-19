package org.sabha.common;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Cross-context read port (ADR-0019, ADR-0033) over the sabha-owned
 * {@code sabhas} table: one table, one value type, three windows on it.
 *
 * <p>Replaces the four ports that each held a projection of the same row —
 * {@code StructuralHierarchyLookup}'s scope and soft-retire reads,
 * {@code SabhaShapeLookup}, {@code SabhaScheduleLookup} and
 * {@code WeeklySabhaCatalog}. Because all three windows return {@link SabhaFact},
 * ADR-0027 §1's warning about mismatched shapes in one interface does not reach
 * this port; see ADR-0033 for the argument and for the reversal condition, which
 * is precisely that the windows stop sharing a type.</p>
 *
 * <p><strong>This port is keyed by a target, not by the caller.</strong> It is
 * deliberately <em>not</em> resolved at the request edge the way
 * {@code CallerAuthority} is (ADR-0032): one request may touch many Sabhas or
 * none, so there is nothing to pre-load. See ADR-0033's opening section.</p>
 *
 * <p>The implementation reads only sabha-owned tables and lives in
 * {@code sabha-data-access}.</p>
 */
public interface SabhaFacts {

    /** The Sabha with the given id; empty when no such Sabha exists. */
    Optional<SabhaFact> of(UUID sabhaId);

    /**
     * Every {@code WEEKLY_RECURRING} Sabha, each with its standing slot present.
     * The attendance context's weekly materialization cron (ADR-0012) iterates
     * these to roll Occurrences forward; monthly-ad-hoc Sabhas are excluded so it
     * never materializes for them.
     */
    List<SabhaFact> allWeekly();

    /**
     * The Sabha of the given {@code (demographic, track)} kind within the Kshetra,
     * if one exists. Used by the BSS/YSS selection workflow (ADR-0006) to resolve
     * the selective Sabha a nominee would additionally join, from their Regular
     * Sabha's Kshetra and demographic.
     */
    Optional<SabhaFact> selectiveIn(UUID kshetraId, String demographic, String track);
}
