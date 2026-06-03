package org.sabha.sabha.dataaccess;

import java.util.UUID;

import org.sabha.sabha.applicationservice.ZoneRepository;
import org.sabha.sabha.domain.Zone;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** JDBC adapter for the {@code zones} table (ADR-0009). */
@Repository
public class JdbcZoneRepository implements ZoneRepository {

    private final JdbcClient jdbc;

    public JdbcZoneRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(Zone zone) {
        jdbc.sql("INSERT INTO zones (id, city_id, name, created_by) VALUES (?, ?, ?, ?)")
                .param(zone.id())
                .param(zone.cityId())
                .param(zone.name())
                .param(zone.createdBy())
                .update();
    }

    @Override
    public boolean existsById(UUID id) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM zones WHERE id = ?)")
                .param(id)
                .query(Boolean.class)
                .single();
    }
}
