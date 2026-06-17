package org.sabha.identity.dataaccess;

import java.util.UUID;

import org.sabha.identity.applicationservice.appointment.AppointerAuthorityLookup;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Reads whether a user already holds an appointing tier at a given scope from
 * {@code role_assignments} (ADR-0011). Each tier matches on its scope column plus
 * the demographic token; the appointment Authorization Engine resolves the
 * geographic containment before calling these.
 */
@Repository
public class JdbcAppointerAuthorityLookup implements AppointerAuthorityLookup {

    private final JdbcClient jdbc;

    public JdbcAppointerAuthorityLookup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean holdsNirdeshak(UUID userId, UUID kshetraId, String demographic) {
        return holds("NIRDESHAK", "kshetra_id", userId, kshetraId, demographic);
    }

    @Override
    public boolean holdsSanyojak(UUID userId, UUID zoneId, String demographic) {
        return holds("SANYOJAK", "zone_id", userId, zoneId, demographic);
    }

    @Override
    public boolean holdsRegionalTeam(UUID userId, UUID cityId, String demographic) {
        return holds("REGIONAL_TEAM", "city_id", userId, cityId, demographic);
    }

    private boolean holds(String role, String scopeColumn, UUID userId, UUID scopeId, String demographic) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1 FROM role_assignments
                    WHERE user_id = ? AND role = ? AND %s = ? AND demographic = ?
                )
                """.formatted(scopeColumn))
                .param(userId)
                .param(role)
                .param(scopeId)
                .param(demographic)
                .query(Boolean.class)
                .single();
    }
}
