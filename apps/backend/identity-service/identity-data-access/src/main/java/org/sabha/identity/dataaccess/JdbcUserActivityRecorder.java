package org.sabha.identity.dataaccess;

import java.time.Instant;
import java.util.UUID;

import org.sabha.common.UserActivityRecorder;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Upserts per-User activity signals into {@code user_activity} (Slice 14): the
 * latest login (web OIDC) and offline-sync push. These feed the proxy picker's
 * informational "last seen" hint together with the last attendance marking (read
 * directly from the marking log). An upsert keyed by {@code user_id} keeps exactly
 * one row per User, overwriting the relevant timestamp.
 */
@Repository
public class JdbcUserActivityRecorder implements UserActivityRecorder {

    private final JdbcClient jdbc;

    public JdbcUserActivityRecorder(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void recordSync(UUID userId, Instant at) {
        jdbc.sql("""
                INSERT INTO user_activity (user_id, last_synced_at)
                VALUES (?, ?)
                ON CONFLICT (user_id) DO UPDATE SET last_synced_at = EXCLUDED.last_synced_at
                """)
                .param(userId)
                .param(java.sql.Timestamp.from(at))
                .update();
    }

    @Override
    public void recordLogin(UUID userId, Instant at) {
        jdbc.sql("""
                INSERT INTO user_activity (user_id, last_login_at)
                VALUES (?, ?)
                ON CONFLICT (user_id) DO UPDATE SET last_login_at = EXCLUDED.last_login_at
                """)
                .param(userId)
                .param(java.sql.Timestamp.from(at))
                .update();
    }
}
