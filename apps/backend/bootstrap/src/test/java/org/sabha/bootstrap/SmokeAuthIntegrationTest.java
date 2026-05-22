package org.sabha.bootstrap;

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

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SmokeAuthIntegrationTest {

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
    void resetMutableTables() {
        // Tests share Postgres + Keycloak across the class; reset write-side tables
        // between tests so order doesn't matter. Seeded read-side rows are left alone.
        jdbc.sql("DELETE FROM attendance_markings").update();
        jdbc.sql("DELETE FROM occurrences WHERE id <> '00000000-0000-0000-0000-000000000020'").update();
    }

    @Test
    void actuatorHealthIsPublicAndReportsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"));
    }

    @Test
    void whoamiRejectsRequestsWithoutABearerToken() throws Exception {
        mockMvc.perform(get("/api/whoami"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whoamiReturnsSeededSanchalakWhenGivenAValidToken() throws Exception {
        String token = obtainSanchalakAccessToken();

        mockMvc.perform(get("/api/whoami").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("sanchalak"))
                .andExpect(jsonPath("$.userId").value("00000000-0000-0000-0000-000000000004"))
                .andExpect(jsonPath("$.personId").value("00000000-0000-0000-0000-000000000003"));
    }

    @Test
    void currentRosterReturnsTodaysOccurrenceAndFiveSeededPeopleWithNoMarkings() throws Exception {
        String token = obtainSanchalakAccessToken();

        mockMvc.perform(get("/api/sanchalak/current-roster").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occurrence.id").value("00000000-0000-0000-0000-000000000020"))
                .andExpect(jsonPath("$.occurrence.state").value("OPEN_FOR_MARKING"))
                .andExpect(jsonPath("$.occurrence.sabhaId").value("00000000-0000-0000-0000-000000000002"))
                .andExpect(jsonPath("$.roster.length()").value(5))
                .andExpect(jsonPath("$.roster[0].personId").value("00000000-0000-0000-0000-000000000101"))
                .andExpect(jsonPath("$.roster[0].fullName").value("Roster Person 1"))
                .andExpect(jsonPath("$.roster[0].present").value(nullValue()))
                .andExpect(jsonPath("$.roster[4].personId").value("00000000-0000-0000-0000-000000000105"));
    }

    @Test
    void markingAPersonPresentPersistsAndShowsInTheRoster() throws Exception {
        String token = obtainSanchalakAccessToken();

        mockMvc.perform(post("/api/occurrences/00000000-0000-0000-0000-000000000020/markings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "personId": "00000000-0000-0000-0000-000000000101", "present": true }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/sanchalak/current-roster").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roster[0].personId").value("00000000-0000-0000-0000-000000000101"))
                .andExpect(jsonPath("$.roster[0].present").value(true))
                .andExpect(jsonPath("$.roster[1].present").value(nullValue()));
    }

    @Test
    void markingIsRejectedWhenOccurrenceIsNotOpenForMarking() throws Exception {
        String token = obtainSanchalakAccessToken();

        UUID finalizedOccurrenceId = UUID.fromString("00000000-0000-0000-0000-000000000021");
        jdbc.sql("""
                INSERT INTO occurrences (id, sabha_id, occurrence_date, state)
                VALUES (?, '00000000-0000-0000-0000-000000000002', CURRENT_DATE - 7, 'FINALIZED')
                """)
                .param(finalizedOccurrenceId)
                .update();

        mockMvc.perform(post("/api/occurrences/" + finalizedOccurrenceId + "/markings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "personId": "00000000-0000-0000-0000-000000000101", "present": true }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void markingTogglesPresentBackToAbsent() throws Exception {
        String token = obtainSanchalakAccessToken();

        mockMvc.perform(post("/api/occurrences/00000000-0000-0000-0000-000000000020/markings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "personId": "00000000-0000-0000-0000-000000000102", "present": true }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/occurrences/00000000-0000-0000-0000-000000000020/markings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "personId": "00000000-0000-0000-0000-000000000102", "present": false }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/sanchalak/current-roster").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roster[1].personId").value("00000000-0000-0000-0000-000000000102"))
                .andExpect(jsonPath("$.roster[1].present").value(false));
    }

    private String obtainSanchalakAccessToken() throws Exception {
        String body = "grant_type=password"
                + "&client_id=sabha-test"
                + "&username=sanchalak"
                + "&password=" + URLEncoder.encode("changeme123!", StandardCharsets.UTF_8)
                + "&scope=openid";

        HttpResponse<String> resp = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create(keycloak.getAuthServerUrl() + "/realms/sabha/protocol/openid-connect/token"))
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
