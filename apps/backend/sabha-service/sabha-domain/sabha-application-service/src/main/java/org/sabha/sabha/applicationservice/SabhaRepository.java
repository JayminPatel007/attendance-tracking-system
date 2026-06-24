package org.sabha.sabha.applicationservice;

import java.util.UUID;

/**
 * Driven port for the Sabha-deletion path (ADR-0026). Sabha <em>creation</em>
 * lives behind the cross-context {@link org.sabha.common.SabhaProvisioning} seam
 * (driven from identity's definition flow); deletion is a sabha-context concern,
 * so its emptiness count and row removal live on this sabha-owned port.
 */
public interface SabhaRepository {

    /** The number of recorded Occurrences of the Sabha — its block-if-non-empty count. */
    int occurrenceCount(UUID sabhaId);

    /** Hard-deletes the (empty) Sabha row; never cascades, so attendance history is never destroyed. */
    void deleteById(UUID sabhaId);
}
