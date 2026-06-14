package org.sabha.container;

import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the install-time Madhyastha Karyalaya bootstrap (ADR-0011 one-off seed)
 * runs at startup: it provisions the member in Keycloak via the Admin API and
 * creates the linked local Person + User + {@code MADHYASTHA_KARYALAYA}
 * role_assignment row (null scope, per the Slice 9 decision).
 */
@SpringBootTest
@Transactional
class MkBootstrapIntegrationTest extends KeycloakIntegrationTest {

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry r) {
        registerMkBootstrap(r);
    }

    @Autowired
    JdbcClient jdbc;

    @Test
    void seedsTheMkMemberLocallyAndInKeycloakAtStartup() {
        UUID userId = jdbc.sql("SELECT id FROM users WHERE username = ?")
                .param(MK_USERNAME)
                .query((rs, n) -> rs.getObject("id", UUID.class))
                .single();

        UUID keycloakUserId = jdbc.sql("SELECT keycloak_user_id FROM users WHERE id = ?")
                .param(userId)
                .query((rs, n) -> rs.getObject("keycloak_user_id", UUID.class))
                .single();
        assertThat(keycloakUserId).isNotNull();

        UUID personId = jdbc.sql("SELECT person_id FROM users WHERE id = ?")
                .param(userId)
                .query((rs, n) -> rs.getObject("person_id", UUID.class))
                .single();
        String fullName = jdbc.sql("SELECT full_name FROM persons WHERE id = ?")
                .param(personId)
                .query((rs, n) -> rs.getString("full_name"))
                .single();
        assertThat(fullName).isEqualTo(MK_FULL_NAME);

        Integer mkRows = jdbc.sql("""
                SELECT COUNT(*) AS c FROM role_assignments
                WHERE user_id = ?
                  AND role = 'MADHYASTHA_KARYALAYA'
                  AND sabha_id IS NULL
                  AND kshetra_id IS NULL
                """)
                .param(userId)
                .query((rs, n) -> rs.getInt("c"))
                .single();
        assertThat(mkRows).isEqualTo(1);
    }

    @Test
    void seedsExactlyOneMkMember() {
        Integer mkCount = jdbc.sql("SELECT COUNT(*) AS c FROM role_assignments WHERE role = 'MADHYASTHA_KARYALAYA'")
                .query((rs, n) -> rs.getInt("c"))
                .single();
        assertThat(mkCount).isEqualTo(1);
    }
}
