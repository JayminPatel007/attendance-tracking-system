package org.sabha.analytics.dataaccess;

import java.util.UUID;

import org.sabha.analytics.applicationservice.ThresholdAdmin;
import org.sabha.analytics.domain.Thresholds;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Updates the single-row {@code analytics_thresholds} config (ADR-0010), stamping
 * the MK member who changed it.
 */
@Repository
public class JdbcThresholdAdmin implements ThresholdAdmin {

    private final JdbcClient jdbc;

    public JdbcThresholdAdmin(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void update(Thresholds thresholds, UUID updatedBy) {
        jdbc.sql("""
                UPDATE analytics_thresholds
                   SET candidate_threshold = ?, priority_threshold = ?, updated_by = ?, updated_at = now()
                 WHERE id = 1
                """)
                .params(thresholds.candidate(), thresholds.priority(), updatedBy)
                .update();
    }
}
