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

    @Override
    public Optional<User> findByPersonId(UUID personId) {
        return jdbc.sql("""
                SELECT id, person_id, username, keycloak_user_id
                FROM users
                WHERE person_id = ?
                """)
                .param(personId)
                .query((rs, rowNum) -> new User(
                        rs.getObject("id", UUID.class),
                        rs.getObject("person_id", UUID.class),
                        rs.getString("username"),
                        rs.getObject("keycloak_user_id", UUID.class)))
                .optional();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jdbc.sql("""
                SELECT id, person_id, username, keycloak_user_id
                FROM users
                WHERE username = ?
                """)
                .param(username)
                .query((rs, rowNum) -> new User(
                        rs.getObject("id", UUID.class),
                        rs.getObject("person_id", UUID.class),
                        rs.getString("username"),
                        rs.getObject("keycloak_user_id", UUID.class)))
                .optional();
    }

    @Override
    public Optional<User> findById(UUID userId) {
        return jdbc.sql("""
                SELECT id, person_id, username, keycloak_user_id
                FROM users
                WHERE id = ?
                """)
                .param(userId)
                .query((rs, rowNum) -> new User(
                        rs.getObject("id", UUID.class),
                        rs.getObject("person_id", UUID.class),
                        rs.getString("username"),
                        rs.getObject("keycloak_user_id", UUID.class)))
                .optional();
    }

    @Override
    public boolean existsByUsername(String username) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM users WHERE username = ?)")
                .param(username)
                .query(Boolean.class)
                .single();
    }

    @Override
    public void save(User user) {
        jdbc.sql("""
                INSERT INTO users (id, person_id, username, keycloak_user_id)
                VALUES (?, ?, ?, ?)
                """)
                .param(user.id())
                .param(user.personId())
                .param(user.username())
                .param(user.keycloakUserId())
                .update();
    }
}
