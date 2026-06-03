package org.sabha.sabha.applicationservice;

import java.util.UUID;

import org.sabha.sabha.domain.City;

/** Driven port for persisting and looking up {@link City} aggregates. */
public interface CityRepository {

    void save(City city);

    boolean existsById(UUID id);
}
