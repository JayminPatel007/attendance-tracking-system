package org.sabha.identity.dataaccess;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import org.sabha.identity.applicationservice.selection.SelectionRepository;
import org.sabha.identity.domain.NominationStatus;
import org.sabha.identity.domain.SelectionNomination;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter persisting {@link SelectionNomination} aggregates (ADR-0006). The
 * nomination row doubles as the audit trail, so {@code save} is an upsert keyed on
 * id that writes the full lifecycle (status, decider, decided-at, reason). The
 * de-duplication and deselection lookups back the orchestrator's guards.
 */
@Repository
public class JdbcSelectionRepository implements SelectionRepository {

    private final JdbcClient jdbc;

    public JdbcSelectionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(SelectionNomination nomination) {
        jdbc.sql("""
                INSERT INTO selection_nominations
                    (id, person_id, regular_sabha_id, selective_sabha_id, kshetra_id,
                     demographic, track, nominated_by, nominated_at, status,
                     decided_by, decided_at, rejection_reason)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status,
                                               decided_by = EXCLUDED.decided_by,
                                               decided_at = EXCLUDED.decided_at,
                                               rejection_reason = EXCLUDED.rejection_reason
                """)
                .param(nomination.id())
                .param(nomination.personId())
                .param(nomination.regularSabhaId())
                .param(nomination.selectiveSabhaId())
                .param(nomination.kshetraId())
                .param(nomination.demographic())
                .param(nomination.track())
                .param(nomination.nominatedBy())
                .param(Timestamp.from(nomination.nominatedAt()))
                .param(nomination.status().name())
                .param(nomination.decidedBy())
                .param(nomination.decidedAt() == null ? null : Timestamp.from(nomination.decidedAt()))
                .param(nomination.rejectionReason())
                .update();
    }

    @Override
    public Optional<SelectionNomination> findById(UUID nominationId) {
        return jdbc.sql(SELECT_BASE + " WHERE id = ?")
                .param(nominationId)
                .query(JdbcSelectionRepository::mapRow)
                .optional();
    }

    @Override
    public boolean hasPendingFor(UUID personId, String track) {
        return jdbc.sql("""
                SELECT COUNT(*) AS c FROM selection_nominations
                WHERE person_id = ? AND track = ? AND status = 'PENDING'
                """)
                .param(personId)
                .param(track)
                .query((rs, n) -> rs.getInt("c"))
                .single() > 0;
    }

    @Override
    public Optional<SelectionNomination> findApproved(UUID personId, UUID selectiveSabhaId) {
        return jdbc.sql(SELECT_BASE + " WHERE person_id = ? AND selective_sabha_id = ? AND status = 'APPROVED'")
                .param(personId)
                .param(selectiveSabhaId)
                .query(JdbcSelectionRepository::mapRow)
                .optional();
    }

    private static final String SELECT_BASE = """
            SELECT id, person_id, regular_sabha_id, selective_sabha_id, kshetra_id,
                   demographic, track, nominated_by, nominated_at, status,
                   decided_by, decided_at, rejection_reason
            FROM selection_nominations
            """;

    private static SelectionNomination mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Timestamp decidedAt = rs.getTimestamp("decided_at");
        return SelectionNomination.rehydrate(
                rs.getObject("id", UUID.class),
                rs.getObject("person_id", UUID.class),
                rs.getObject("regular_sabha_id", UUID.class),
                rs.getObject("selective_sabha_id", UUID.class),
                rs.getObject("kshetra_id", UUID.class),
                rs.getString("demographic"),
                rs.getString("track"),
                rs.getObject("nominated_by", UUID.class),
                rs.getTimestamp("nominated_at").toInstant(),
                NominationStatus.valueOf(rs.getString("status")),
                rs.getObject("decided_by", UUID.class),
                decidedAt == null ? null : decidedAt.toInstant(),
                rs.getString("rejection_reason"));
    }
}
