package org.sabha.sabha.dataaccess;

import java.util.UUID;

import org.sabha.sabha.applicationservice.CityRepository;
import org.sabha.sabha.domain.City;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** JDBC adapter for the {@code cities} table (ADR-0009). */
@Repository
public class JdbcCityRepository implements CityRepository {

    private final JdbcClient jdbc;

    public JdbcCityRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(City city) {
        jdbc.sql("INSERT INTO cities (id, name, created_by) VALUES (?, ?, ?)")
                .param(city.id())
                .param(city.name())
                .param(city.createdBy())
                .update();
    }

    @Override
    public boolean existsById(UUID id) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM cities WHERE id = ?)")
                .param(id)
                .query(Boolean.class)
                .single();
    }
}
