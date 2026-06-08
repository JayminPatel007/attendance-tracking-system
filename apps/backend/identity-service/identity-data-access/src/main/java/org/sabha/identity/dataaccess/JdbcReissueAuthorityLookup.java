package org.sabha.identity.dataaccess;

import java.util.UUID;

import org.sabha.identity.applicationservice.ReissueAuthorityLookup;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Reads the assigner-reissue authority from {@code role_assignments} (ADR-0004 /
 * ADR-0011): whether the caller is the recorded appointer of the target, and
 * whether the target is a Sant (the {@code SANT} role string, which lives outside
 * the operational {@code Role} enum).
 */
@Repository
public class JdbcReissueAuthorityLookup implements ReissueAuthorityLookup {

    private static final String SANT_ROLE = "SANT";

    private final JdbcClient jdbc;

    public JdbcReissueAuthorityLookup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean wasAppointedBy(UUID targetUserId, UUID appointerUserId) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1 FROM role_assignments
                    WHERE user_id = ? AND appointed_by = ?
                )
                """)
                .param(targetUserId)
                .param(appointerUserId)
                .query(Boolean.class)
                .single();
    }

    @Override
    public boolean isSant(UUID userId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM role_assignments WHERE user_id = ? AND role = ?)")
                .param(userId)
                .param(SANT_ROLE)
                .query(Boolean.class)
                .single();
    }
}
