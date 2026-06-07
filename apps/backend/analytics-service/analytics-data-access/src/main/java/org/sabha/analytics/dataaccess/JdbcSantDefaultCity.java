package org.sabha.analytics.dataaccess;

import java.util.Optional;
import java.util.UUID;

import org.sabha.analytics.applicationservice.SantDefaultCity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Reads and writes a Sant's persisted default City on {@code users.default_city_id}
 * (Slice 17). The dashboard (analytics) owns this preference read/write directly
 * against the {@code users} row rather than through an identity port — the chosen
 * City is purely a dashboard concern. The column is nullable: empty until the
 * Sant first picks.
 */
@Repository
public class JdbcSantDefaultCity implements SantDefaultCity {

    private final JdbcClient jdbc;

    public JdbcSantDefaultCity(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UUID> defaultCityOf(UUID userId) {
        return jdbc.sql("SELECT default_city_id FROM users WHERE id = :userId")
                .param("userId", userId)
                .query(UUID.class)
                .optional();
    }

    @Override
    public void choose(UUID userId, UUID cityId) {
        jdbc.sql("UPDATE users SET default_city_id = :cityId WHERE id = :userId")
                .param("cityId", cityId)
                .param("userId", userId)
                .update();
    }
}
