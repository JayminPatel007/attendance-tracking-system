package org.sabha.identity.dataaccess;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.sabha.common.Role;
import org.sabha.identity.applicationservice.session.UserRolesLookup;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Reads every operational {@link Role} a user holds, across all scopes, from the
 * {@code role_assignments} table. Role strings that are not operational tiers
 * (e.g. {@code MADHYASTHA_KARYALAYA}) are skipped rather than failing the lookup.
 */
@Repository
public class JdbcUserRolesLookup implements UserRolesLookup {

    private final JdbcClient jdbc;

    public JdbcUserRolesLookup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Set<Role> operationalRolesOf(UUID userId) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        jdbc.sql("SELECT role FROM role_assignments WHERE user_id = ? AND revoked_at IS NULL")
                .param(userId)
                .query((rs, n) -> rs.getString("role"))
                .list()
                .forEach(name -> toRole(name).ifPresent(roles::add));
        return roles;
    }

    private static Optional<Role> toRole(String name) {
        try {
            return Optional.of(Role.valueOf(name));
        } catch (IllegalArgumentException unknown) {
            return Optional.empty();
        }
    }
}
