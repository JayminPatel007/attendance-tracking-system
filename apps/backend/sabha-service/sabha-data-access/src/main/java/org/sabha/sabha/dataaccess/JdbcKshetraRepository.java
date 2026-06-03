package org.sabha.sabha.dataaccess;

import org.sabha.sabha.applicationservice.KshetraRepository;
import org.sabha.sabha.domain.Kshetra;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter for the {@code kshetras} table (ADR-0009). The table pre-dates
 * this slice (Slice 2); structural creation populates the {@code zone_id} and
 * {@code created_by} columns added in the Slice 10 migration.
 */
@Repository
public class JdbcKshetraRepository implements KshetraRepository {

    private final JdbcClient jdbc;

    public JdbcKshetraRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(Kshetra kshetra) {
        jdbc.sql("INSERT INTO kshetras (id, zone_id, name, created_by) VALUES (?, ?, ?, ?)")
                .param(kshetra.id())
                .param(kshetra.zoneId())
                .param(kshetra.name())
                .param(kshetra.createdBy())
                .update();
    }
}
