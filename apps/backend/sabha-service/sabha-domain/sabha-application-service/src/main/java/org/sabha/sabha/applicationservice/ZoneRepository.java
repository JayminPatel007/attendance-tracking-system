package org.sabha.sabha.applicationservice;

import java.util.Optional;
import java.util.UUID;

import org.sabha.sabha.domain.Zone;

/** Driven port for persisting and looking up {@link Zone} aggregates. */
public interface ZoneRepository {

    void save(Zone zone);

    boolean existsById(UUID id);

    /** The City the Zone belongs to (empty when no such Zone) — its delete-authority scope. */
    Optional<UUID> cityIdOf(UUID id);

    /** The number of Kshetras directly under the Zone — its block-if-non-empty count (ADR-0026). */
    int kshetraCount(UUID id);

    /** Hard-deletes the (empty) Zone row; never cascades (ADR-0026). */
    void deleteById(UUID id);
}
