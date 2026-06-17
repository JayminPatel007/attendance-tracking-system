package org.sabha.identity.dataaccess;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.sabha.identity.applicationservice.passwordreset.PasswordReissueAuditLog;
import org.sabha.identity.applicationservice.passwordreset.PasswordResetRepository;
import org.sabha.identity.domain.OtpChallenge;
import org.sabha.identity.domain.PasswordReset;
import org.sabha.identity.domain.PasswordResetStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter persisting {@link PasswordReset} aggregates and assigner-reissue
 * audit rows into {@code password_resets} (ADR-0004). A self-service reset is
 * inserted on request and updated in place through verify / complete, so
 * {@code save} is an upsert keyed on the reset id (mirroring
 * {@link JdbcHomeSabhaTransferRepository}). The {@code status} column is the
 * aggregate's derived status; on read the OTP sub-state flags are reconstructed
 * from it. Reissue rows are append-only audit and never re-read as aggregates —
 * so this one adapter also fronts the {@link PasswordReissueAuditLog} port,
 * keeping both reset paths in the shared {@code password_resets} table.
 */
@Repository
public class JdbcPasswordResetRepository implements PasswordResetRepository, PasswordReissueAuditLog {

    private static final String SELF_SERVICE = "SELF_SERVICE_OTP";
    private static final String ASSIGNER_REISSUE = "ASSIGNER_REISSUE";

    private final JdbcClient jdbc;

    public JdbcPasswordResetRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(PasswordReset reset) {
        OtpChallenge challenge = reset.challenge();
        jdbc.sql("""
                INSERT INTO password_resets
                    (id, target_user_id, keycloak_user_id, method, mobile, otp_code,
                     initiated_at, otp_expires_at, attempts, status,
                     reset_token, reset_token_expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET attempts = EXCLUDED.attempts,
                                               status = EXCLUDED.status,
                                               reset_token = EXCLUDED.reset_token,
                                               reset_token_expires_at = EXCLUDED.reset_token_expires_at
                """)
                .param(reset.id())
                .param(reset.userId())
                .param(reset.keycloakUserId())
                .param(SELF_SERVICE)
                .param(reset.mobile())
                .param(challenge.codeHash())
                .param(Timestamp.from(reset.initiatedAt()))
                .param(Timestamp.from(challenge.expiresAt()))
                .param(challenge.attempts())
                .param(reset.status().name())
                .param(reset.resetToken())
                .param(reset.resetTokenExpiresAt() == null ? null : Timestamp.from(reset.resetTokenExpiresAt()))
                .update();
    }

    @Override
    public Optional<PasswordReset> findById(UUID id) {
        return jdbc.sql(selectByColumn("id")).param(id).query(this::map).optional();
    }

    @Override
    public Optional<PasswordReset> findByResetToken(String resetToken) {
        return jdbc.sql(selectByColumn("reset_token")).param(resetToken).query(this::map).optional();
    }

    @Override
    public Optional<Instant> lastInitiatedAt(String mobile) {
        return jdbc.sql("""
                SELECT initiated_at FROM password_resets
                WHERE mobile = ? AND method = ?
                ORDER BY initiated_at DESC
                LIMIT 1
                """)
                .param(mobile)
                .param(SELF_SERVICE)
                .query((rs, n) -> rs.getTimestamp("initiated_at").toInstant())
                .optional();
    }

    @Override
    public int sendCountSince(String mobile, Instant since) {
        return jdbc.sql("""
                SELECT COUNT(*) AS c FROM password_resets
                WHERE mobile = ? AND method = ? AND initiated_at >= ?
                """)
                .param(mobile)
                .param(SELF_SERVICE)
                .param(Timestamp.from(since))
                .query((rs, n) -> rs.getInt("c"))
                .single();
    }

    @Override
    public void recordReissue(UUID id, UUID targetUserId, UUID actorUserId, Instant reissuedAt) {
        jdbc.sql("""
                INSERT INTO password_resets
                    (id, target_user_id, method, initiated_at, actor_user_id, completed_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """)
                .param(id)
                .param(targetUserId)
                .param(ASSIGNER_REISSUE)
                .param(Timestamp.from(reissuedAt))
                .param(actorUserId)
                .param(Timestamp.from(reissuedAt))
                .update();
    }

    private static String selectByColumn(String column) {
        return """
                SELECT id, target_user_id, keycloak_user_id, mobile, otp_code,
                       initiated_at, otp_expires_at, attempts, status,
                       reset_token, reset_token_expires_at
                FROM password_resets
                WHERE %s = ? AND method = 'SELF_SERVICE_OTP'
                """.formatted(column);
    }

    private PasswordReset map(ResultSet rs, int rowNum) throws SQLException {
        Timestamp tokenExpiry = rs.getTimestamp("reset_token_expires_at");
        return PasswordReset.rehydrate(
                rs.getObject("id", UUID.class),
                rs.getObject("target_user_id", UUID.class),
                rs.getObject("keycloak_user_id", UUID.class),
                rs.getString("mobile"),
                rs.getString("otp_code"),
                rs.getTimestamp("otp_expires_at").toInstant(),
                rs.getInt("attempts"),
                PasswordResetStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("initiated_at").toInstant(),
                rs.getString("reset_token"),
                tokenExpiry == null ? null : tokenExpiry.toInstant());
    }
}
