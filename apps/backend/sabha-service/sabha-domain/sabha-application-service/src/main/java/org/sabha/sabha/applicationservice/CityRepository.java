package org.sabha.sabha.applicationservice;

import java.util.UUID;

import org.sabha.sabha.domain.City;

/** Driven port for persisting and looking up {@link City} aggregates. */
public interface CityRepository {

    void save(City city);

    boolean existsById(UUID id);

    /** The number of Zones directly under the City — its block-if-non-empty count (ADR-0026). */
    int zoneCount(UUID cityId);

    /** Hard-deletes the (empty) City row; never cascades (ADR-0026). */
    void deleteById(UUID id);
}
