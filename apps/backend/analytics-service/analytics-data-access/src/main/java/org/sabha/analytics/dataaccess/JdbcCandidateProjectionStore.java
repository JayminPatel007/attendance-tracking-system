package org.sabha.analytics.dataaccess;

import java.util.List;

import org.sabha.analytics.applicationservice.CandidateProjectionStore;
import org.sabha.analytics.domain.Candidate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Writes the re-engagement projection. The replace is wholesale: the old rows are
 * cleared and the fresh candidate set inserted, so a concurrent dashboard read
 * sees either the previous projection or the new one, never a partial rebuild.
 * The enclosing transaction is owned by the {@code @Transactional}
 * {@code ReEngagementProjectionScanner.refresh()} use case (ADR-0018), not this
 * adapter.
 */
@Repository
public class JdbcCandidateProjectionStore implements CandidateProjectionStore {

    private final JdbcClient jdbc;

    public JdbcCandidateProjectionStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void replaceAll(List<Candidate> candidates) {
        jdbc.sql("DELETE FROM reengagement_candidates").update();
        for (Candidate candidate : candidates) {
            jdbc.sql("""
                    INSERT INTO reengagement_candidates (person_id, home_sabha_id, missed_streak, tier)
                    VALUES (?, ?, ?, ?)
                    """)
                    .params(candidate.personId(), candidate.homeSabhaId(), candidate.missedStreak(),
                            candidate.tier().name())
                    .update();
        }
    }
}
