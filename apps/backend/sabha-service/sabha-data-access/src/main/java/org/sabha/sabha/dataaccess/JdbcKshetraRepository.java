package org.sabha.sabha.dataaccess;

import java.util.Optional;
import java.util.UUID;

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

    @Override
    public Optional<UUID> zoneIdOf(UUID id) {
        return jdbc.sql("SELECT zone_id FROM kshetras WHERE id = ?")
                .param(id)
                .query((rs, n) -> rs.getObject("zone_id", UUID.class))
                .optional();
    }

    @Override
    public int sabhaCount(UUID id) {
        return jdbc.sql("SELECT count(*) FROM sabhas WHERE kshetra_id = ?")
                .param(id)
                .query(Integer.class)
                .single();
    }

    @Override
    public void deleteById(UUID id) {
        jdbc.sql("DELETE FROM kshetras WHERE id = ?").param(id).update();
    }
}
