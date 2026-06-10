package org.sabha.identity.dataaccess;

import java.util.UUID;

import org.sabha.identity.applicationservice.SantMembership;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter for Sant membership (Slice 19). Sant is a {@code role_assignments}
 * row with {@code role = 'SANT'}, outside the operational
 * {@link org.sabha.common.Role} enum — the same representation the analytics
 * {@code JdbcSantDirectory} reads for the dashboard's universal-read exception
 * (ADR-0008). The Sant's formal (City, demographic) on that row is irrelevant
 * here: the web shell only needs the boolean to unlock the Audit-log section.
 */
@Repository
public class JdbcSantMembership implements SantMembership {

    private final JdbcClient jdbc;

    public JdbcSantMembership(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean isSant(UUID userId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM role_assignments WHERE user_id = :userId AND role = 'SANT')")
                .param("userId", userId)
                .query(Boolean.class)
                .single();
    }
}
