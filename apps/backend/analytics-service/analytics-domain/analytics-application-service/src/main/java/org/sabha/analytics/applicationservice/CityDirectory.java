package org.sabha.analytics.applicationservice;

import java.util.List;
import java.util.UUID;

/**
 * Read port over the Cities a Sant may pick from in the dashboard chip (Slice
 * 17). Single-organisation (ADR-0005), so this is every City in the State.
 * {@link #exists} guards a pick against an unknown City.
 */
public interface CityDirectory {

    List<CityOption> allCities();

    boolean exists(UUID cityId);
}
