package org.sabha.analytics.dataaccess;

import org.sabha.analytics.applicationservice.ThresholdConfig;
import org.sabha.analytics.domain.Thresholds;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Reads the MK-owned re-engagement thresholds from the single-row
 * {@code analytics_thresholds} config table (ADR-0010). One read per calculation.
 */
@Repository
public class JdbcThresholdConfig implements ThresholdConfig {

    private final JdbcClient jdbc;

    public JdbcThresholdConfig(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Thresholds current() {
        return jdbc.sql("SELECT candidate_threshold, priority_threshold FROM analytics_thresholds WHERE id = 1")
                .query((rs, n) -> new Thresholds(rs.getInt("candidate_threshold"), rs.getInt("priority_threshold")))
                .single();
    }
}
