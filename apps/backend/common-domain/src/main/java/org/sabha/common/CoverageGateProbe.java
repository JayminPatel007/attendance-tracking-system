package org.sabha.common;

/**
 * THROWAWAY: temporary probe to prove the new-code coverage gate fails on
 * uncovered lines. Deleted in the same PR once validated.
 */
public final class CoverageGateProbe {

    private CoverageGateProbe() {
    }

    public static int uncoveredSum(int a, int b) {
        int total = a + b;
        if (total > 10) {
            total = total * 2;
        } else {
            total = total - 1;
        }
        return total;
    }
}
