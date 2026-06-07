package org.sabha.analytics.dataaccess;

import java.util.UUID;

import org.sabha.analytics.applicationservice.SantDirectory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Reads Sant membership from {@code role_assignments} (Slice 17). Sant is a row
 * with {@code role = 'SANT'}, outside the operational {@link org.sabha.common.Role}
 * enum — the dashboard reads it directly, the same way it already couples to
 * {@code role_assignments} for the role-scoped predicate (ADR-0008, analytics
 * read models). The Sant's formal City/demographic on that row is irrelevant: the
 * universal-read exception ignores it.
 */
@Repository
public class JdbcSantDirectory implements SantDirectory {

    private final JdbcClient jdbc;

    public JdbcSantDirectory(JdbcClient jdbc) {
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
