package org.sabha.container;

import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
@Transactional
class WebBffIntegrationTest extends KeycloakIntegrationTest {

    /** Seeded Sanchalak's Keycloak subject (infra/keycloak/realm-sabha.json). */
    private static final String SANCHALAK_SUBJECT = "00000000-0000-0000-0000-000000000005";

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry r) {
        // Seed an MK member so /bff/me can resolve its sections.
        registerMkBootstrap(r);
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
        String mkSubject = jdbc.sql("SELECT keycloak_user_id FROM users WHERE username = ?")
                .param(MK_USERNAME)
                .query((rs, n) -> rs.getObject("keycloak_user_id", UUID.class))
                .single()
                .toString();

        mockMvc.perform(get("/bff/me").with(oidcLogin().idToken(t -> t.subject(mkSubject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(MK_USERNAME))
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
                .andExpect(jsonPath("$.sections", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("STRUCTURAL_ADMIN"))))
                // A Sanchalak resolves to no audit scope, so the BFF engine denies them
                // and the section stays hidden (issue #80, AC: no other role changes).
                .andExpect(jsonPath("$.sections", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("AUDIT_LOG"))));
    }

    @Test
    void bffMeGivesARegionalTeamMemberTheAuditLogSection() throws Exception {
        // A Regional Team member is not an operational Role, so the old nav gate
        // never surfaced the section though the BFF authorised their City scope
        // (issue #80). With the gate now deriving from the same scope resolution,
        // a REGIONAL_TEAM role-assignment row unlocks the sidebar entry end-to-end.
        UUID subject = UUID.randomUUID();
        UUID rtUser = seedUser(subject, "regional-team-member");
        UUID city = seedCity(rtUser);
        cityScopedRole(rtUser, "REGIONAL_TEAM", city, "YUVAK");

        mockMvc.perform(get("/bff/me").with(oidcLogin().idToken(t -> t.subject(subject.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("regional-team-member"))
                .andExpect(jsonPath("$.madhyasthaKaryalaya").value(false))
                .andExpect(jsonPath("$.sections", org.hamcrest.Matchers.hasItem("AUDIT_LOG")))
                .andExpect(jsonPath("$.sections", org.hamcrest.Matchers.hasItem("DASHBOARD")));
    }

    @Test
    void bffMeIssuesAnXsrfCookieTheSpaCanEcho() throws Exception {
        Cookie xsrf = mockMvc.perform(get("/bff/me").with(oidcLogin().idToken(t -> t.subject(SANCHALAK_SUBJECT))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("XSRF-TOKEN");

        assertThat(xsrf).isNotNull();
        assertThat(xsrf.getValue()).isNotBlank();
    }

    @Test
    void logoutSucceedsWhenTheSpaEchoesTheXsrfToken() throws Exception {
        Cookie xsrf = mockMvc.perform(get("/bff/me").with(oidcLogin().idToken(t -> t.subject(SANCHALAK_SUBJECT))))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/bff/logout")
                        .with(oidcLogin().idToken(t -> t.subject(SANCHALAK_SUBJECT)))
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue()))
                .andExpect(status().isNoContent());
    }

    @Test
    void logoutIsRejectedWithoutTheXsrfToken() throws Exception {
        mockMvc.perform(post("/bff/logout")
                        .with(oidcLogin().idToken(t -> t.subject(SANCHALAK_SUBJECT))))
                .andExpect(status().isForbidden());
    }

    private UUID seedUser(UUID keycloakSubject, String username) {
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO persons (id, full_name, gender, mobile) VALUES (?, 'Regional Team', 'MALE', ?)")
                .params(id, "+91" + Math.abs(id.hashCode())).update();
        jdbc.sql("INSERT INTO users (id, person_id, username, keycloak_user_id) VALUES (?, ?, ?, ?)")
                .params(id, id, username, keycloakSubject).update();
        return id;
    }

    private UUID seedCity(UUID createdBy) {
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO cities (id, name, created_by) VALUES (?, 'Test City', ?)")
                .params(id, createdBy).update();
        return id;
    }

    private void cityScopedRole(UUID userId, String role, UUID cityId, String demographic) {
        jdbc.sql("INSERT INTO role_assignments (id, user_id, role, city_id, demographic) VALUES (?, ?, ?, ?, ?)")
                .params(UUID.randomUUID(), userId, role, cityId, demographic).update();
    }
}
