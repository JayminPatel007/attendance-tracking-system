package org.sabha.sabha.domain;

import java.util.UUID;

/**
 * A Kshetra — a sub-region within a Zone hosting ~10–20 Sabhas (CONTEXT §
 * Kshetra). Created by the Zone's Sanyojak (ADR-0009). Carries its parent Zone,
 * name, and a {@code createdBy} audit field.
 */
public record Kshetra(UUID id, UUID zoneId, String name, UUID createdBy) {

    public static Kshetra create(UUID zoneId, String name, UUID createdBy) {
        return new Kshetra(UUID.randomUUID(), zoneId, StructuralNames.require(name, "Kshetra"), createdBy);
    }
}
