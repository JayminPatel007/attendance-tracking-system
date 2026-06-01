package org.sabha.identity.dataaccess;

import java.util.UUID;

import org.sabha.identity.applicationservice.MadhyasthaKaryalayaMembership;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter for Madhyastha Karyalaya membership. MK is recorded as a
 * {@code role_assignments} row with {@code role = 'MADHYASTHA_KARYALAYA'} and
 * null scope (State-level; single-organization per ADR-0005, so there is no
 * per-State column). The {@link org.sabha.common.Role} enum deliberately
 * excludes MK, so this membership lives outside {@code RoleAssignmentLookup}.
 */
@Repository
public class JdbcMadhyasthaKaryalayaMembership implements MadhyasthaKaryalayaMembership {

    private static final String MK_ROLE = "MADHYASTHA_KARYALAYA";

    private final JdbcClient jdbc;

    public JdbcMadhyasthaKaryalayaMembership(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean anyMemberExists() {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM role_assignments WHERE role = ?)")
                .param(MK_ROLE)
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
                .param(MK_ROLE)
                .update();
    }

    @Override
    public boolean isMember(UUID userId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM role_assignments WHERE user_id = ? AND role = ?)")
                .param(userId)
                .param(MK_ROLE)
                .query(Boolean.class)
                .single();
    }
}
