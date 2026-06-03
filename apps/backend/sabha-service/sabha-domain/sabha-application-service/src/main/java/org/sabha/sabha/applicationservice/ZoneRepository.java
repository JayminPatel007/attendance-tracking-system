package org.sabha.sabha.applicationservice;

import java.util.UUID;

import org.sabha.sabha.domain.Zone;

/** Driven port for persisting and looking up {@link Zone} aggregates. */
public interface ZoneRepository {

    void save(Zone zone);

    boolean existsById(UUID id);
}
