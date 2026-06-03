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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import dasniko.testcontainers.keycloak.KeycloakContainer;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Structural creation end-to-end over the BFF (ADR-0009, ADR-0022). Proves the
 * authority chain through real HTTP + Postgres: an MK member creates Cities,
 * Zones, and Sabha Kinds; a Sanyojak creates Kshetras only within their Zone;
 * non-authorized callers are rejected by the Authorization Engine; and the
 * Sanyukta-Regular-only invariant holds at the wire.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class StructuralCreationIntegrationTest {

    /** Seeded Sanchalak (non-MK) Keycloak subject — infra/keycloak/realm-sabha.json. */
    private static final String SANCHALAK_SUBJECT = "00000000-0000-0000-0000-000000000005";

    private static final UUID SANYOJAK_SUBJECT = UUID.fromString("00000000-0000-0000-0000-0000000005a1");
    private static final UUID SANYOJAK_PERSON = UUID.fromString("00000000-0000-0000-0000-0000000005a2");
    private static final UUID SANYOJAK_USER = UUID.fromString("00000000-0000-0000-0000-0000000005a3");

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

        r.add("sabha.bootstrap.mk.username", () -> "mk-struct");
        r.add("sabha.bootstrap.mk.password", () -> "changeme123!");
        r.add("sabha.bootstrap.mk.full-name", () -> "Structural MK Member");
        r.add("sabha.bootstrap.mk.gender", () -> "MALE");
        r.add("sabha.bootstrap.mk.mobile", () -> "+919820155666");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcClient jdbc;

    @Test
    void mkMemberCreatesACityPersistedWithCreatedBySetToThemselves() throws Exception {
        UUID mkUser = localUserId("mk-struct");
        String mkSubject = keycloakSubject("mk-struct");

        String body = mockMvc.perform(authedPost(mkSubject, "/bff/structure/cities", "{\"name\":\"Surat\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();
        UUID cityId = UUID.fromString(body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));

        UUID createdBy = jdbc.sql("SELECT created_by FROM cities WHERE id = ?")
                .param(cityId).query((rs, n) -> rs.getObject("created_by", UUID.class)).single();
        assertThat(createdBy).isEqualTo(mkUser);
    }

    @Test
    void mkMemberCreatesAZoneWithinACity() throws Exception {
        String mkSubject = keycloakSubject("mk-struct");
        UUID cityId = createCity(mkSubject, "Vadodara");

        mockMvc.perform(authedPost(mkSubject, "/bff/structure/zones",
                        "{\"cityId\":\"" + cityId + "\",\"name\":\"VadodaraNorth\"}"))
                .andExpect(status().isCreated());

        Long zones = jdbc.sql("SELECT count(*) FROM zones WHERE city_id = ? AND name = 'VadodaraNorth'")
                .param(cityId).query(Long.class).single();
        assertThat(zones).isEqualTo(1L);
    }

    @Test
    void mkMemberRegistersASabhaKind() throws Exception {
        String mkSubject = keycloakSubject("mk-struct");

        mockMvc.perform(authedPost(mkSubject, "/bff/structure/sabha-kinds",
                        "{\"demographic\":\"YUVATI\",\"track\":\"YSS\"}"))
                .andExpect(status().isCreated());

        Long kinds = jdbc.sql("SELECT count(*) FROM sabha_kinds WHERE demographic='YUVATI' AND track='YSS'")
                .query(Long.class).single();
        assertThat(kinds).isEqualTo(1L);
    }

    @Test
    void theKindBuilderRejectsASanyuktaSelectiveKind() throws Exception {
        String mkSubject = keycloakSubject("mk-struct");

        mockMvc.perform(authedPost(mkSubject, "/bff/structure/sabha-kinds",
                        "{\"demographic\":\"SANYUKTA\",\"track\":\"BSS\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void aNonMkUserCannotCreateACity() throws Exception {
        mockMvc.perform(authedPost(SANCHALAK_SUBJECT, "/bff/structure/cities", "{\"name\":\"Rajkot\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aSanyojakCreatesAKshetraWithinTheirZoneButNotOutsideIt() throws Exception {
        String mkSubject = keycloakSubject("mk-struct");
        UUID cityId = createCity(mkSubject, "Bhavnagar");
        UUID myZone = createZone(mkSubject, cityId, "BhavnagarCentral");
        UUID otherZone = createZone(mkSubject, cityId, "BhavnagarEast");
        seedSanyojak(myZone);

        mockMvc.perform(authedPost(SANYOJAK_SUBJECT.toString(), "/bff/structure/kshetras",
                        "{\"zoneId\":\"" + myZone + "\",\"name\":\"Goregaon-2\"}"))
                .andExpect(status().isCreated());

        UUID createdBy = jdbc.sql("SELECT created_by FROM kshetras WHERE zone_id = ? AND name = 'Goregaon-2'")
                .param(myZone).query((rs, n) -> rs.getObject("created_by", UUID.class)).single();
        assertThat(createdBy).isEqualTo(SANYOJAK_USER);

        mockMvc.perform(authedPost(SANYOJAK_SUBJECT.toString(), "/bff/structure/kshetras",
                        "{\"zoneId\":\"" + otherZone + "\",\"name\":\"OutOfScope\"}"))
                .andExpect(status().isForbidden());

        // my-zones scopes the Sanyojak's create form to exactly the Zone they own.
        mockMvc.perform(get("/bff/structure/my-zones")
                        .with(oidcLogin().idToken(t -> t.subject(SANYOJAK_SUBJECT.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(myZone.toString()));
    }

    // --- helpers -----------------------------------------------------------

    private UUID createCity(String mkSubject, String name) throws Exception {
        String body = mockMvc.perform(authedPost(mkSubject, "/bff/structure/cities", "{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));
    }

    private UUID createZone(String mkSubject, UUID cityId, String name) throws Exception {
        String body = mockMvc.perform(authedPost(mkSubject, "/bff/structure/zones",
                        "{\"cityId\":\"" + cityId + "\",\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));
    }

    private void seedSanyojak(UUID zoneId) {
        jdbc.sql("INSERT INTO persons (id, full_name, gender, mobile) VALUES (?, 'Sanyojak Tracer', 'MALE', '+919820100777')")
                .param(SANYOJAK_PERSON).update();
        jdbc.sql("INSERT INTO users (id, person_id, username, keycloak_user_id) VALUES (?, ?, 'sanyojak-struct', ?)")
                .param(SANYOJAK_USER).param(SANYOJAK_PERSON).param(SANYOJAK_SUBJECT).update();
        jdbc.sql("INSERT INTO role_assignments (id, user_id, role, zone_id) VALUES (?, ?, 'SANYOJAK', ?)")
                .param(UUID.randomUUID()).param(SANYOJAK_USER).param(zoneId).update();
    }

    private MockHttpServletRequestBuilder authedPost(String subject, String path, String json) throws Exception {
        Cookie xsrf = mockMvc.perform(get("/bff/me").with(oidcLogin().idToken(t -> t.subject(subject))))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
        return post(path)
                .with(oidcLogin().idToken(t -> t.subject(subject)))
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content(json);
    }

    private String keycloakSubject(String username) {
        return jdbc.sql("SELECT keycloak_user_id FROM users WHERE username = ?")
                .param(username).query((rs, n) -> rs.getObject("keycloak_user_id", UUID.class)).single().toString();
    }

    private UUID localUserId(String username) {
        return jdbc.sql("SELECT id FROM users WHERE username = ?")
                .param(username).query((rs, n) -> rs.getObject("id", UUID.class)).single();
    }
}
