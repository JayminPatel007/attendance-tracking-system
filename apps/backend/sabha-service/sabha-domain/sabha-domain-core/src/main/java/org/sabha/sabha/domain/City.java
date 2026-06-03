package org.sabha.sabha.domain;

import java.util.UUID;

/**
 * A City in the geographic hierarchy (CONTEXT § Geographic hierarchy). Created
 * by a Madhyastha Karyalaya member within the (single, per ADR-0005) State; the
 * State is implicit so a City carries only its name and a {@code createdBy}
 * audit field (ADR-0009).
 */
public record City(UUID id, String name, UUID createdBy) {

    public static City create(String name, UUID createdBy) {
        return new City(UUID.randomUUID(), StructuralNames.require(name, "City"), createdBy);
    }
}
