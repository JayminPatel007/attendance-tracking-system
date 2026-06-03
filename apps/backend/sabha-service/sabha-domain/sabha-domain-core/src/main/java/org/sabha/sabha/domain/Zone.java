package org.sabha.sabha.domain;

import java.util.UUID;

/**
 * A Zone within a City (CONTEXT § Geographic hierarchy). Created by a Madhyastha
 * Karyalaya member; Sanyojak roles attach per Zone × Sabha-kind. Carries its
 * parent City, name, and a {@code createdBy} audit field (ADR-0009).
 */
public record Zone(UUID id, UUID cityId, String name, UUID createdBy) {

    public static Zone create(UUID cityId, String name, UUID createdBy) {
        return new Zone(UUID.randomUUID(), cityId, StructuralNames.require(name, "Zone"), createdBy);
    }
}
