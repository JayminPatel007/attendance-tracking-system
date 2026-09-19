package org.sabha.analytics.applicationservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** In-memory {@link CityDirectory}; every added City is pickable and listed. */
final class FakeCityDirectory implements CityDirectory {

    private final Map<UUID, String> cities = new HashMap<>();

    void add(UUID id) {
        cities.put(id, "City " + id);
    }

    @Override
    public boolean exists(UUID cityId) {
        return cities.containsKey(cityId);
    }

    @Override
    public List<CityOption> allCities() {
        return cities.entrySet().stream()
                .map(e -> new CityOption(e.getKey(), e.getValue()))
                .toList();
    }
}
