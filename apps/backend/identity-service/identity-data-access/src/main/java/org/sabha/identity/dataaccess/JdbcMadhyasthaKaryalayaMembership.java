package org.sabha.identity.dataaccess;

import java.util.UUID;

import org.sabha.common.MadhyasthaKaryalayaLookup;
import org.sabha.common.OversightRole;
import org.sabha.identity.applicationservice.bootstrap.MadhyasthaKaryalayaMembership;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter for Madhyastha Karyalaya membership. MK is recorded as a
 * {@code role_assignments} row with {@code role = 'MADHYASTHA_KARYALAYA'} and
 * null scope (State-level; single-organization per ADR-0005, so there is no
 * per-State column). The {@link org.sabha.common.Role} enum deliberately
 * excludes MK, so this membership lives outside {@code RoleAssignmentLookup}.
 *
 * <p>Serves both the identity-internal {@link MadhyasthaKaryalayaMembership}
 * port (bootstrap + read) and the cross-context read-only
 * {@link MadhyasthaKaryalayaLookup} (common-domain) that the sabha context's
 * structural-creation authorization consults — same {@code isMember} query,
 * one adapter (ADR-0019).</p>
 */
@Repository
public class JdbcMadhyasthaKaryalayaMembership
        implements MadhyasthaKaryalayaMembership, MadhyasthaKaryalayaLookup {

    private final JdbcClient jdbc;

    public JdbcMadhyasthaKaryalayaMembership(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean anyMemberExists() {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM role_assignments WHERE role = ?)")
                .param(OversightRole.MADHYASTHA_KARYALAYA.wireValue())
                .query(Boolean.class)
                .single();
    }

    @Override
    public void grantTo(UUID userId) {
        jdbc.sql("""
                INSERT INTO role_assignments (id, user_id, role, sabha_id, kshetra_id)
                VALUES (?, ?, ?, NULL, NULL)
                """)
                .param(UUID.randomUUID())
                .param(userId)
                .param(OversightRole.MADHYASTHA_KARYALAYA.wireValue())
                .update();
    }

    @Override
    public boolean isMember(UUID userId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM role_assignments WHERE user_id = ? AND role = ?)")
                .param(userId)
                .param(OversightRole.MADHYASTHA_KARYALAYA.wireValue())
                .query(Boolean.class)
                .single();
    }
}
