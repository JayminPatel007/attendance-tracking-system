package org.sabha.analytics.applicationservice;

import java.util.Optional;
import java.util.UUID;

/**
 * Read/write port for a Sant's persisted default City (Slice 17). The chosen
 * City <em>is</em> the default: picking a City in the dashboard chip both filters
 * the view and persists the choice, so it survives across logins. Stored on the
 * User record ({@code users.default_city_id}); empty until the Sant first picks.
 */
public interface SantDefaultCity {

    Optional<UUID> defaultCityOf(UUID userId);

    void choose(UUID userId, UUID cityId);
}
