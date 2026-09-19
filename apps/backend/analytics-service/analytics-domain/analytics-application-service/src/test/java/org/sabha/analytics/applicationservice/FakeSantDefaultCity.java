package org.sabha.analytics.applicationservice;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** In-memory {@link SantDefaultCity}; {@code users.default_city_id} as a map. */
final class FakeSantDefaultCity implements SantDefaultCity {

    private final Map<UUID, UUID> defaults = new HashMap<>();

    @Override
    public Optional<UUID> defaultCityOf(UUID userId) {
        return Optional.ofNullable(defaults.get(userId));
    }

    @Override
    public void choose(UUID userId, UUID cityId) {
        defaults.put(userId, cityId);
    }
}
