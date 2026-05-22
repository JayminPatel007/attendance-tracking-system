package org.sabha.identity.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.sabha.sharedkernel.CallerResolver;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Resolves an authenticated Keycloak subject (the {@code sub} JWT claim) to the
 * local {@code users.id}. The {@code users} table is owned by identity, so the
 * adapter lives here; the port lives in shared-kernel so other contexts can
 * depend on it without crossing the bounded-context seam (ADR-0015 / 0017).
 */
@Repository
public class JdbcCallerResolver implements CallerResolver {

    private final JdbcClient jdbc;

    public JdbcCallerResolver(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UUID> resolveUserId(UUID keycloakSubject) {
        return jdbc.sql("SELECT id FROM users WHERE keycloak_user_id = ?")
                .param(keycloakSubject)
                .query((rs, n) -> rs.getObject("id", UUID.class))
                .optional();
    }
}
