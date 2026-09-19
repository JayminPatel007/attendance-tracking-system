package org.sabha.identity.dataaccess;

import java.util.UUID;

import org.sabha.common.OversightRole;
import org.sabha.identity.applicationservice.bootstrap.MadhyasthaKaryalayaMembership;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter for Madhyastha Karyalaya membership. MK is recorded as a
 * {@code role_assignments} row with {@code role = 'MADHYASTHA_KARYALAYA'} and
 * null scope (State-level; single-organization per ADR-0005, so there is no
 * per-State column). The {@link org.sabha.common.Role} enum deliberately
 * excludes MK.
 *
 * <p>It implemented two interfaces until ADR-0032: the identity-internal {@link
 * MadhyasthaKaryalayaMembership} (bootstrap write plus the global existence
 * check) and the cross-context {@code MadhyasthaKaryalayaLookup}, whose {@code
 * isMember(userId)} was caller-keyed and folded into {@code
 * CallerAuthority.isMadhyasthaKaryalaya()}. Only one half folded, so the adapter
 * survives — {@code anyMemberExists} asks about the organization and {@code
 * grantTo} is a write, and neither is about a caller.</p>
 */
@Repository
public class JdbcMadhyasthaKaryalayaMembership implements MadhyasthaKaryalayaMembership {

    private final JdbcClient jdbc;

    public JdbcMadhyasthaKaryalayaMembership(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean anyMemberExists() {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM role_assignments WHERE role = ? AND revoked_at IS NULL)")
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
}
