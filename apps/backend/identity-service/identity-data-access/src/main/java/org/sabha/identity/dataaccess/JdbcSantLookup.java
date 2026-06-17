package org.sabha.identity.dataaccess;

import java.util.UUID;

import org.sabha.common.OversightRole;
import org.sabha.common.SantLookup;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The one JDBC adapter for Sant membership (issue #79). Sant is a
 * {@code role_assignments} row whose {@code role} is {@link OversightRole#SANT} —
 * the single home for that string and the "outside the operational
 * {@link org.sabha.common.Role} enum" caveat. The {@code role_assignments} table
 * is identity-owned, so this cross-context read lives in identity-data-access and
 * is exposed to every context through the common-domain {@link SantLookup} port
 * (ADR-0019, ADR-0029), mirroring {@link JdbcMadhyasthaKaryalayaMembership}.
 */
@Repository
public class JdbcSantLookup implements SantLookup {

    private final JdbcClient jdbc;

    public JdbcSantLookup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean isSant(UUID userId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM role_assignments WHERE user_id = ? AND role = ?)")
                .param(userId)
                .param(OversightRole.SANT.wireValue())
                .query(Boolean.class)
                .single();
    }
}
