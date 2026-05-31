package org.sabha.container;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice 7A end-to-end: a Sanchalak records a Walk-in against an open Occurrence
 * (POST /api/occurrences/{id}/walk-ins), wired from REST through the application
 * service, the aggregate, and JDBC persistence. Proves the Walk-in is stored with
 * {@code marking_type = WALK_IN} and is distinguishable from Roster markings, that
 * a cross-demographic Walk-in (a Baal-Sabha Person at this Yuvak Occurrence) is
 * accepted, and that walking a Person in never alters their Home Sabha.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class WalkInMarkingIntegrationTest {

    // Seeded by slice-2/002-seed.sql: today's OPEN_FOR_MARKING Occurrence of the
    // REGULAR_YUVAK tracer Sabha, and a Yuvak roster member.
    private static final UUID OCCURRENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID ROSTER_PERSON = UUID.fromString("00000000-0000-0000-0000-000000000101");
    // Seeded by slice-6/001-person-directory.sql: a Person whose Home Sabha is the
    // REGULAR_BAAL tracer Sabha — a cross-demographic visitor to this Yuvak Sabha.
    private static final UUID WALK_IN_PERSON = UUID.fromString("00000000-0000-0000-0000-000000000110");
    private static final UUID BAAL_HOME_SABHA = UUID.fromString("00000000-0000-0000-0000-000000000111");

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
        r.add("sabha.keycloak.issuer-uri",
                () -> keycloak.getAuthServerUrl() + "/realms/sabha");
        r.add("sabha.keycloak.admin-base-url", keycloak::getAuthServerUrl);
        r.add("sabha.keycloak.realm", () -> "sabha");
        r.add("sabha.keycloak.admin-username", keycloak::getAdminUsername);
        r.add("sabha.keycloak.admin-password", keycloak::getAdminPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcClient jdbc;

    @AfterEach
    void cleanUp() {
        jdbc.sql("DELETE FROM attendance_markings WHERE occurrence_id = ?")
                .param(OCCURRENCE_ID).update();
    }

    @Test
    void walkingInAPersonPersistsAWalkInMarkingDistinguishableFromRosterMarkings() throws Exception {
        String token = token("sanchalak");

        mockMvc.perform(post("/api/occurrences/" + OCCURRENCE_ID + "/markings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"personId\": \"" + ROSTER_PERSON + "\", \"present\": true }"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/occurrences/" + OCCURRENCE_ID + "/walk-ins")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"personId\": \"" + WALK_IN_PERSON + "\" }"))
                .andExpect(status().isOk());

        assertThat(markingType(ROSTER_PERSON)).isEqualTo("ROSTER");
        assertThat(markingType(WALK_IN_PERSON)).isEqualTo("WALK_IN");
        assertThat(present(WALK_IN_PERSON)).isTrue();
        // The discriminator lets a query split the two marking kinds apart.
        assertThat(countByType("WALK_IN")).isEqualTo(1);
        assertThat(countByType("ROSTER")).isEqualTo(1);
    }

    @Test
    void walkingInACrossDemographicPersonIsAcceptedAndDoesNotChangeTheirHomeSabha() throws Exception {
        String token = token("sanchalak");
        String homeSabhasBefore = homeSabhaIds(WALK_IN_PERSON);

        mockMvc.perform(post("/api/occurrences/" + OCCURRENCE_ID + "/walk-ins")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"personId\": \"" + WALK_IN_PERSON + "\" }"))
                .andExpect(status().isOk());

        assertThat(markingType(WALK_IN_PERSON)).isEqualTo("WALK_IN");
        // Home Sabha is untouched: still exactly the Baal Sabha it was seeded with.
        assertThat(homeSabhaIds(WALK_IN_PERSON)).isEqualTo(homeSabhasBefore);
        assertThat(homeSabhaIds(WALK_IN_PERSON)).isEqualTo(BAAL_HOME_SABHA.toString());
    }

    private String markingType(UUID personId) {
        return jdbc.sql("SELECT marking_type FROM attendance_markings WHERE occurrence_id = ? AND person_id = ?")
                .param(OCCURRENCE_ID).param(personId)
                .query((rs, n) -> rs.getString("marking_type")).single();
    }

    private boolean present(UUID personId) {
        return jdbc.sql("SELECT present FROM attendance_markings WHERE occurrence_id = ? AND person_id = ?")
                .param(OCCURRENCE_ID).param(personId)
                .query((rs, n) -> rs.getBoolean("present")).single();
    }

    private int countByType(String type) {
        return jdbc.sql("SELECT COUNT(*) AS c FROM attendance_markings WHERE occurrence_id = ? AND marking_type = ?")
                .param(OCCURRENCE_ID).param(type)
                .query((rs, n) -> rs.getInt("c")).single();
    }

    private String homeSabhaIds(UUID personId) {
        return jdbc.sql("SELECT string_agg(sabha_id::text, ',' ORDER BY sabha_id) AS ids "
                        + "FROM home_sabhas WHERE person_id = ?")
                .param(personId)
                .query((rs, n) -> rs.getString("ids")).single();
    }

    private String token(String username) throws Exception {
        String body = "grant_type=password"
                + "&client_id=sabha-test"
                + "&username=" + username
                + "&password=" + URLEncoder.encode("changeme123!", StandardCharsets.UTF_8)
                + "&scope=openid";

        HttpResponse<String> resp = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create(keycloak.getAuthServerUrl()
                                + "/realms/sabha/protocol/openid-connect/token"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            throw new IllegalStateException(
                    "Keycloak token endpoint returned " + resp.statusCode() + ": " + resp.body());
        }
        return new ObjectMapper().readTree(resp.body()).get("access_token").asText();
    }
}
