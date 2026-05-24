package org.sabha.identity.dataaccess;

import java.util.Optional;
import java.util.UUID;

import org.sabha.identity.applicationservice.UserRepository;
import org.sabha.identity.domain.User;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserRepository implements UserRepository {

    private final JdbcClient jdbc;

    public JdbcUserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<User> findByKeycloakUserId(UUID keycloakUserId) {
        return jdbc.sql("""
                SELECT id, person_id, username, keycloak_user_id
                FROM users
                WHERE keycloak_user_id = ?
                """)
                .param(keycloakUserId)
                .query((rs, rowNum) -> new User(
                        rs.getObject("id", UUID.class),
                        rs.getObject("person_id", UUID.class),
                        rs.getString("username"),
                        rs.getObject("keycloak_user_id", UUID.class)))
                .optional();
    }
}
