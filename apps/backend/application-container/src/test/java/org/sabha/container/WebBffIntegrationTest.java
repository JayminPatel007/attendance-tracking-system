package org.sabha.container;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import dasniko.testcontainers.keycloak.KeycloakContainer;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Backend-for-Frontend session surface (ADR-0022): the Angular app
 * authenticates via a server-side OIDC session and reads "who am I + which
 * sections may I see" from {@code GET /bff/me}. Web requests are cookie/session
 * authenticated, not Bearer — the mobile {@code /api/**} resource-server path is
 * proven separately in {@link SmokeAuthIntegrationTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class WebBffIntegrationTest {

    /** Seeded Sanchalak's Keycloak subject (infra/keycloak/realm-sabha.json). */
    private static final String SANCHALAK_SUBJECT = "00000000-0000-0000-0000-000000000005";

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
        // The BFF client provider's endpoints are derived from sabha.keycloak.issuer-uri
        // (see application.yml), so setting that one property points both at the container.
        r.add("sabha.keycloak.issuer-uri", () -> keycloak.getAuthServerUrl() + "/realms/sabha");
        r.add("sabha.keycloak.admin-base-url", keycloak::getAuthServerUrl);
        r.add("sabha.keycloak.realm", () -> "sabha");
        r.add("sabha.keycloak.admin-username", keycloak::getAdminUsername);
        r.add("sabha.keycloak.admin-password", keycloak::getAdminPassword);

        // Seed an MK member so /bff/me can resolve its sections.
        r.add("sabha.bootstrap.mk.username", () -> "mk-bff");
        r.add("sabha.bootstrap.mk.password", () -> "changeme123!");
        r.add("sabha.bootstrap.mk.full-name", () -> "BFF MK Member");
        r.add("sabha.bootstrap.mk.gender", () -> "MALE");
        r.add("sabha.bootstrap.mk.mobile", () -> "+919820133444");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcClient jdbc;

    @Test
    void bffMeRejectsAnUnauthenticatedXhrWith401() throws Exception {
        mockMvc.perform(get("/bff/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bffMeReturnsTheMkMembersUsernameAndStateOversightSections() throws Exception {
        String mkSubject = jdbc.sql("SELECT keycloak_user_id FROM users WHERE username = 'mk-bff'")
                .query((rs, n) -> rs.getObject("keycloak_user_id", UUID.class))
                .single()
                .toString();

        mockMvc.perform(get("/bff/me").with(oidcLogin().idToken(t -> t.subject(mkSubject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("mk-bff"))
                .andExpect(jsonPath("$.madhyasthaKaryalaya").value(true))
                .andExpect(jsonPath("$.sections", org.hamcrest.Matchers.hasItem("STRUCTURAL_ADMIN")))
                .andExpect(jsonPath("$.sections", org.hamcrest.Matchers.hasItem("DASHBOARD")));
    }

    @Test
    void bffMeHidesMkSectionsFromANonMkUser() throws Exception {
        mockMvc.perform(get("/bff/me").with(oidcLogin().idToken(t -> t.subject(SANCHALAK_SUBJECT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("sanchalak"))
                .andExpect(jsonPath("$.madhyasthaKaryalaya").value(false))
                .andExpect(jsonPath("$.sections", org.hamcrest.Matchers.hasItem("DASHBOARD")))
                .andExpect(jsonPath("$.sections", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("STRUCTURAL_ADMIN"))));
    }
}
