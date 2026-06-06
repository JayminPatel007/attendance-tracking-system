package org.sabha.analytics.applicationservice;

import java.util.List;
import java.util.UUID;

/**
 * The Sabha analytics tree (section C, ADR-0010): Zone → Kshetra → Sabha with a
 * candidate count at every level, scoped to the caller. A Kshetra with no Zone
 * (the tracer seed) surfaces under a Zone node with a null id.
 */
public record SabhaTree(List<Zone> zones) {

    public record Zone(UUID zoneId, String zoneName, int candidateCount, List<Kshetra> kshetras) {
    }

    public record Kshetra(UUID kshetraId, String kshetraName, int candidateCount, List<Sabha> sabhas) {
    }

    public record Sabha(UUID sabhaId, String sabhaKind, int candidateCount) {
    }
}
