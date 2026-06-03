package org.sabha.sabha.dataaccess;

import java.util.Optional;
import java.util.UUID;

import org.sabha.common.SabhaShapeLookup;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of the cross-context {@link SabhaShapeLookup} port
 * (ADR-0012, ADR-0019): reads a Sabha's {@code schedule_shape} token so the
 * attendance context can guard monthly-ad-hoc Occurrence creation and the
 * compliance nudge. Empty when the Sabha does not exist.
 */
@Repository
public class JdbcSabhaShapeLookup implements SabhaShapeLookup {

    private final JdbcClient jdbc;

    public JdbcSabhaShapeLookup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<String> scheduleShapeOf(UUID sabhaId) {
        return jdbc.sql("SELECT schedule_shape FROM sabhas WHERE id = ?")
                .param(sabhaId)
                .query(String.class)
                .optional();
    }
}
