package org.sabha.container;

import org.hamcrest.Matchers;
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
 * Audit-log viewer across the web BFF (Slice 19, ADR-0023, ADR-0022). The feed is
 * a read-model UNION over the audit columns earlier slices populate; the
 * slice-19 seed gives it a deterministic set: a Nirdeshak appointment, two
 * Occurrence transitions (one carrying an on-behalf-of proxy attribution), and a
 * State-level Sabha-kind creation with no geography. Asserts: the State-wide MK
 * reads everything including the geography-less entry; a Nirdeshak reads only the
 * entries in their Kshetra and never the State-level one; the proxy filter keeps
 * only the on-behalf-of entry; an entity drill-down returns one entity's history;
 * and the tiers below Nirdeshak (Nirikshak, Sanchalak) are forbidden.
 * Cookie/session authenticated via the OIDC login post-processor, mirroring
 * {@link SanchalakProxyIntegrationTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuditLogIntegrationTest {

    /** Slice-16 seed: Nirdeshak scoped to (Kshetra Tracer, YUVAK); slice-19 backfills its appointment audit. */
    private static final String NIRDESHAK_SUBJECT = "00000000-0000-0000-0000-000000000062";
    /** Slice-13 seed: Nirikshak — a tier below Nirdeshak, must be denied. */
    private static final String NIRIKSHAK_SUBJECT = "00000000-0000-0000-0000-000000000052";
    /** Slice-14 seed: a plain Sanchalak — must be denied. */
    private static final String SANCHALAK_SUBJECT = "00000000-0000-0000-0000-0000000000b5";
    /** The proxy occurrence the seeded transitions land on. */
    private static final String AUDIT_OCCURRENCE = "00000000-0000-0000-0000-0000000000b6";

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

        r.add("sabha.bootstrap.mk.username", () -> "mk-audit");
        r.add("sabha.bootstrap.mk.password", () -> "changeme123!");
        r.add("sabha.bootstrap.mk.full-name", () -> "Audit MK Member");
        r.add("sabha.bootstrap.mk.gender", () -> "MALE");
        r.add("sabha.bootstrap.mk.mobile", () -> "+919820134788");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcClient jdbc;

    /** The bootstrapped MK member's Keycloak subject (its keycloak_user_id). */
    private String mkSubject() {
        return jdbc.sql("""
                SELECT u.keycloak_user_id FROM users u
                JOIN role_assignments ra ON ra.user_id = u.id
                WHERE ra.role = 'MADHYASTHA_KARYALAYA' LIMIT 1
                """).query(String.class).single();
    }

    @Test
    void theMadhyasthaKaryalayaReadsTheWholeFeedIncludingTheStateLevelEntry() throws Exception {
        mockMvc.perform(get("/bff/audit-log").with(oidcLogin().idToken(t -> t.subject(mkSubject()))))
                .andExpect(status().isOk())
                // State-level Sabha-kind creation (no geography) is visible to the MK.
                .andExpect(jsonPath("$[?(@.action == 'SABHA_KIND_CREATED')]").exists())
                // ...as are the Occurrence transitions and the role appointment.
                .andExpect(jsonPath("$[?(@.targetType == 'OCCURRENCE')]").exists())
                .andExpect(jsonPath("$[?(@.targetType == 'ROLE_ASSIGNMENT')]").exists());
    }

    @Test
    void aNirdeshakReadsTheirKshetraButNotTheStateLevelEntry() throws Exception {
        mockMvc.perform(get("/bff/audit-log").with(oidcLogin().idToken(t -> t.subject(NIRDESHAK_SUBJECT))))
                .andExpect(status().isOk())
                // In-Kshetra entries are visible.
                .andExpect(jsonPath("$[?(@.targetType == 'OCCURRENCE')]").exists())
                .andExpect(jsonPath("$[?(@.targetType == 'ROLE_ASSIGNMENT')]").exists())
                // The geography-less State-level entry is not.
                .andExpect(jsonPath("$[?(@.action == 'SABHA_KIND_CREATED')]").doesNotExist());
    }

    @Test
    void theProxyFilterKeepsOnlyOnBehalfOfEntries() throws Exception {
        mockMvc.perform(get("/bff/audit-log?proxyOnly=true")
                        .with(oidcLogin().idToken(t -> t.subject(NIRDESHAK_SUBJECT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].action").value("CANCEL"))
                .andExpect(jsonPath("$[0].onBehalfOfUserId").value(Matchers.notNullValue()));
    }

    @Test
    void anEntityDrillDownReturnsJustThatEntitysHistory() throws Exception {
        mockMvc.perform(get("/bff/audit-log?targetType=OCCURRENCE&targetId=" + AUDIT_OCCURRENCE)
                        .with(oidcLogin().idToken(t -> t.subject(NIRDESHAK_SUBJECT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].targetId")
                        .value(Matchers.everyItem(Matchers.equalTo(AUDIT_OCCURRENCE))));
    }

    @Test
    void aNirikshakIsForbidden() throws Exception {
        mockMvc.perform(get("/bff/audit-log").with(oidcLogin().idToken(t -> t.subject(NIRIKSHAK_SUBJECT))))
                .andExpect(status().isForbidden());
    }

    @Test
    void aSanchalakIsForbidden() throws Exception {
        mockMvc.perform(get("/bff/audit-log").with(oidcLogin().idToken(t -> t.subject(SANCHALAK_SUBJECT))))
                .andExpect(status().isForbidden());
    }
}
