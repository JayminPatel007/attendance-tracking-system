package org.sabha.analytics.dataaccess;

import java.util.List;

import org.sabha.analytics.applicationservice.CandidateProjectionStore;
import org.sabha.analytics.domain.Candidate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the re-engagement projection. The replace is wholesale and atomic: the
 * old rows are cleared and the fresh candidate set inserted in one transaction,
 * so a concurrent dashboard read sees either the previous projection or the new
 * one, never a partial rebuild.
 */
@Repository
public class JdbcCandidateProjectionStore implements CandidateProjectionStore {

    private final JdbcClient jdbc;

    public JdbcCandidateProjectionStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
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
