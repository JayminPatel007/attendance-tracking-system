package org.sabha.sabha.dataaccess;

import org.sabha.sabha.applicationservice.SabhaKindRepository;
import org.sabha.sabha.domain.Demographic;
import org.sabha.sabha.domain.SabhaKind;
import org.sabha.sabha.domain.Track;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** JDBC adapter for the {@code sabha_kinds} table (ADR-0009). */
@Repository
public class JdbcSabhaKindRepository implements SabhaKindRepository {

    private final JdbcClient jdbc;

    public JdbcSabhaKindRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(SabhaKind kind) {
        jdbc.sql("INSERT INTO sabha_kinds (id, demographic, track, created_by) VALUES (?, ?, ?, ?)")
                .param(kind.id())
                .param(kind.demographic().name())
                .param(kind.track().name())
                .param(kind.createdBy())
                .update();
    }

    @Override
    public boolean exists(Demographic demographic, Track track) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM sabha_kinds WHERE demographic = ? AND track = ?)")
                .param(demographic.name())
                .param(track.name())
                .query(Boolean.class)
                .single();
    }
}
