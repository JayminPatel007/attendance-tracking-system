package org.sabha.sabha.applicationservice;

import java.util.Optional;
import java.util.UUID;

import org.sabha.sabha.domain.Kshetra;

/** Driven port for persisting {@link Kshetra} aggregates. */
public interface KshetraRepository {

    void save(Kshetra kshetra);

    /** The Zone the Kshetra belongs to (empty when no such Kshetra) — its delete-authority scope. */
    Optional<UUID> zoneIdOf(UUID id);

    /** The number of Sabhas directly under the Kshetra — its block-if-non-empty count (ADR-0026). */
    int sabhaCount(UUID id);

    /** Hard-deletes the (empty) Kshetra row; never cascades (ADR-0026). */
    void deleteById(UUID id);
}
