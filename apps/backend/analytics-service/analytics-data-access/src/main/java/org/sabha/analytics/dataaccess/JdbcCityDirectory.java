package org.sabha.analytics.dataaccess;

import java.util.List;
import java.util.UUID;

import org.sabha.analytics.applicationservice.CityDirectory;
import org.sabha.analytics.applicationservice.CityOption;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Lists the Cities a Sant may pick from (Slice 17). Single-organisation
 * (ADR-0005), so this is every City in the State, ordered by name for the chip.
 */
@Repository
public class JdbcCityDirectory implements CityDirectory {

    private final JdbcClient jdbc;

    public JdbcCityDirectory(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<CityOption> allCities() {
        return jdbc.sql("SELECT id, name FROM cities ORDER BY name")
                .query((rs, n) -> new CityOption(rs.getObject("id", UUID.class), rs.getString("name")))
                .list();
    }

    @Override
    public boolean exists(UUID cityId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM cities WHERE id = :cityId)")
                .param("cityId", cityId)
                .query(Boolean.class)
                .single();
    }
}
