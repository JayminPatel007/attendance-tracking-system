package org.sabha.container;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import dasniko.testcontainers.keycloak.KeycloakContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the install-time Madhyastha Karyalaya bootstrap (ADR-0011 one-off seed)
 * runs at startup: it provisions the member in Keycloak via the Admin API and
 * creates the linked local Person + User + {@code MADHYASTHA_KARYALAYA}
 * role_assignment row (null scope, per the Slice 9 decision).
 */
@SpringBootTest
@Testcontainers
class MkBootstrapIntegrationTest {

    private static final String MK_USERNAME = "mk-bootstrap";
    private static final String MK_FULL_NAME = "Bootstrap MK Member";
    private static final String MK_MOBILE = "+919820111222";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static KeycloakContainer keycloak = new KeycloakContainer()
            .withRealmImportFile("/realm-sabha.json");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry r) {
        r.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloak.getAuthServerUrl() + "/realms/sabha");
        r.add("sabha.keycloak.issuer-uri", () -> keycloak.getAuthServerUrl() + "/realms/sabha");
        r.add("sabha.keycloak.admin-base-url", keycloak::getAuthServerUrl);
        r.add("sabha.keycloak.realm", () -> "sabha");
        r.add("sabha.keycloak.admin-username", keycloak::getAdminUsername);
        r.add("sabha.keycloak.admin-password", keycloak::getAdminPassword);

        r.add("sabha.bootstrap.mk.username", () -> MK_USERNAME);
        r.add("sabha.bootstrap.mk.password", () -> "changeme123!");
        r.add("sabha.bootstrap.mk.full-name", () -> MK_FULL_NAME);
        r.add("sabha.bootstrap.mk.gender", () -> "MALE");
        r.add("sabha.bootstrap.mk.mobile", () -> MK_MOBILE);
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
