package org.sabha.sabha.dataaccess;

import java.util.UUID;

import org.sabha.sabha.applicationservice.SabhaRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter for the Sabha-deletion path (ADR-0026). Counts a Sabha's recorded
 * Occurrences for the block-if-non-empty guard and hard-deletes the (empty) row.
 * The delete never cascades, so Occurrences/attendance are never destroyed —
 * a non-empty Sabha is rejected upstream before this runs.
 */
@Repository
public class JdbcSabhaRepository implements SabhaRepository {

    private final JdbcClient jdbc;

    public JdbcSabhaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int occurrenceCount(UUID sabhaId) {
        return jdbc.sql("SELECT count(*) FROM occurrences WHERE sabha_id = ?")
                .param(sabhaId)
                .query(Integer.class)
                .single();
    }

    @Override
    public void deleteById(UUID sabhaId) {
        jdbc.sql("DELETE FROM sabhas WHERE id = ?").param(sabhaId).update();
    }
}
