package org.sabha.identity.dataaccess;

import java.util.UUID;

import org.sabha.identity.applicationservice.passwordreset.ReissueAuthorityLookup;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Reads the assigner-reissue authority from {@code role_assignments} (ADR-0004 /
 * ADR-0011): whether the caller is the recorded appointer of the target. The
 * Sant branch of that authorization now goes through the common-domain
 * {@link org.sabha.common.SantLookup} (issue #79), so this adapter no longer
 * knows the {@code SANT} role string.
 */
@Repository
public class JdbcReissueAuthorityLookup implements ReissueAuthorityLookup {

    private final JdbcClient jdbc;

    public JdbcReissueAuthorityLookup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean wasAppointedBy(UUID targetUserId, UUID appointerUserId) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1 FROM role_assignments
                    WHERE user_id = ? AND appointed_by = ? AND revoked_at IS NULL
                )
                """)
                .param(targetUserId)
                .param(appointerUserId)
                .query(Boolean.class)
                .single();
    }
}
