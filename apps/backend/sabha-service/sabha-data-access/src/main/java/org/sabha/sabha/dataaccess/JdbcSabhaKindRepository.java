package org.sabha.sabha.dataaccess;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.sabha.sabha.applicationservice.SabhaKindRepository;
import org.sabha.sabha.domain.Demographic;
import org.sabha.sabha.domain.SabhaKind;
import org.sabha.sabha.domain.Track;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** JDBC adapter for the {@code sabha_kinds} table (ADR-0009, soft-retire ADR-0026). */
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

    @Override
    public Optional<SabhaKind> findById(UUID id) {
        return jdbc.sql("SELECT id, demographic, track, created_by, retired_at, retired_by "
                        + "FROM sabha_kinds WHERE id = ?")
                .param(id)
                .query((rs, n) -> new SabhaKind(
                        rs.getObject("id", UUID.class),
                        Demographic.valueOf(rs.getString("demographic")),
                        Track.valueOf(rs.getString("track")),
                        rs.getObject("created_by", UUID.class),
                        toInstant(rs.getTimestamp("retired_at")),
                        rs.getObject("retired_by", UUID.class)))
                .optional();
    }

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }

    @Override
    public void update(SabhaKind kind) {
        jdbc.sql("UPDATE sabha_kinds SET retired_at = ?, retired_by = ? WHERE id = ?")
                .param(kind.retiredAt() == null ? null : Timestamp.from(kind.retiredAt()))
                .param(kind.retiredBy())
                .param(kind.id())
                .update();
    }
}
