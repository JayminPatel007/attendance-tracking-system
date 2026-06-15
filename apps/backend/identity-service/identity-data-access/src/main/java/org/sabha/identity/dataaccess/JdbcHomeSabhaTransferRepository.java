package org.sabha.identity.dataaccess;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.sabha.identity.applicationservice.HomeSabhaTransferRepository;
import org.sabha.identity.domain.HomeSabhaTransfer;
import org.sabha.identity.domain.OtpChallenge;
import org.sabha.identity.domain.TransferStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter persisting {@link HomeSabhaTransfer} aggregates (ADR-0002). A
 * transfer is inserted on initiate and updated in place on each confirm attempt
 * (status / attempts), so {@code save} is an upsert keyed on the transfer id. The
 * mobile-scoped count / last-initiated queries back the OTP rate limit and
 * resend cooldown off the {@code (mobile, initiated_at)} index.
 */
@Repository
public class JdbcHomeSabhaTransferRepository implements HomeSabhaTransferRepository {

    private final JdbcClient jdbc;

    public JdbcHomeSabhaTransferRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(HomeSabhaTransfer transfer) {
        OtpChallenge challenge = transfer.challenge();
        jdbc.sql("""
                INSERT INTO home_sabha_transfers
                    (id, person_id, mobile, destination_sabha_id, initiating_user_id,
                     otp_code, initiated_at, expires_at, status, attempts)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status,
                                               attempts = EXCLUDED.attempts
                """)
                .param(transfer.id())
                .param(transfer.personId())
                .param(transfer.mobile())
                .param(transfer.destinationSabhaId())
                .param(transfer.initiatingUserId())
                .param(challenge.codeHash())
                .param(Timestamp.from(transfer.initiatedAt()))
                .param(Timestamp.from(challenge.expiresAt()))
                .param(transfer.status().name())
                .param(challenge.attempts())
                .update();
    }

    @Override
    public Optional<HomeSabhaTransfer> findById(UUID transferId) {
        return jdbc.sql("""
                SELECT id, person_id, mobile, destination_sabha_id, initiating_user_id,
                       otp_code, initiated_at, expires_at, status, attempts
                FROM home_sabha_transfers
                WHERE id = ?
                """)
                .param(transferId)
                .query((rs, n) -> HomeSabhaTransfer.rehydrate(
                        rs.getObject("id", UUID.class),
                        rs.getObject("person_id", UUID.class),
                        rs.getString("mobile"),
                        rs.getObject("destination_sabha_id", UUID.class),
                        rs.getObject("initiating_user_id", UUID.class),
                        rs.getString("otp_code"),
                        rs.getTimestamp("initiated_at").toInstant(),
                        rs.getTimestamp("expires_at").toInstant(),
                        TransferStatus.valueOf(rs.getString("status")),
                        rs.getInt("attempts")))
                .optional();
    }

    @Override
    public int sendCountSince(String mobile, Instant since) {
        return jdbc.sql("""
                SELECT COUNT(*) AS c FROM home_sabha_transfers
                WHERE mobile = ? AND initiated_at >= ?
                """)
                .param(mobile)
                .param(Timestamp.from(since))
                .query((rs, n) -> rs.getInt("c"))
                .single();
    }

    @Override
    public Optional<Instant> lastInitiatedAt(String mobile) {
        return jdbc.sql("""
                SELECT initiated_at FROM home_sabha_transfers
                WHERE mobile = ?
                ORDER BY initiated_at DESC
                LIMIT 1
                """)
                .param(mobile)
                .query((rs, n) -> rs.getTimestamp("initiated_at").toInstant())
                .optional();
    }
}
