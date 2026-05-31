package org.sabha.identity.domain;

import java.util.List;
import java.util.UUID;

/**
 * Pure domain service (ADR-0020) selecting which of a Person's current Home
 * Sabhas a Verified Home Sabha Transfer replaces: the one whose {@code sabha_kind}
 * matches the destination's, so only the affected demographic+track moves.
 */
public final class HomeSabhaSwap {

    private HomeSabhaSwap() {
    }

    /**
     * The Home Sabha to remove for a transfer into {@code destinationKind}, or
     * {@link NoMatchingHomeSabhaException} when the Person holds none of that kind.
     */
    public static UUID selectPrevious(List<HomeSabhaRef> currentHomeSabhas, String destinationKind) {
        return currentHomeSabhas.stream()
                .filter(ref -> destinationKind.equals(ref.kind()))
                .map(HomeSabhaRef::sabhaId)
                .findFirst()
                .orElseThrow(() -> new NoMatchingHomeSabhaException(destinationKind));
    }
}
