package org.sabha.analytics.domain;

import java.util.Set;
import java.util.UUID;

/**
 * The set of Home Sabhas a single Calculator run covers (ADR-0010). A Scope answers
 * the one question the Calculator needs — whether a given Home Sabha falls inside it.
 *
 * <p>The dashboard's per-tier roll-up (Sanchalak through Madhyastha Karyalaya) is
 * <em>not</em> done here: it filters the candidate projection at read time. The
 * production scanner always runs {@link Everything}; {@link OfSabhas} is the seam
 * for a future incremental refresh (recompute just the Sabhas whose attendance
 * changed) and bounds Calculator/adapter tests to known data.</p>
 */
public sealed interface Scope permits Scope.OfSabhas, Scope.Everything {

    boolean includes(UUID sabhaId);

    /** A concrete set of Home Sabhas — targeted recompute and test fixtures resolve to this. */
    record OfSabhas(Set<UUID> sabhaIds) implements Scope {
        @Override
        public boolean includes(UUID sabhaId) {
            return sabhaIds.contains(sabhaId);
        }
    }

    /** Every Home Sabha — what the State-wide projection refresh runs. */
    record Everything() implements Scope {
        @Override
        public boolean includes(UUID sabhaId) {
            return true;
        }
    }
}
