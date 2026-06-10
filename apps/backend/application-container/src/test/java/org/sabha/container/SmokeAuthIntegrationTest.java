package org.sabha.container;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
        // Clear an occurrence's transitions before the occurrence itself: the
        // slice-19 audit seed is the first to seed occurrence_state_transitions, so
        // deleting the parent rows now trips the FK without this (ADR-0023).
        jdbc.sql("DELETE FROM attendance_markings").update();
        jdbc.sql("DELETE FROM occurrence_state_transitions WHERE occurrence_id <> '00000000-0000-0000-0000-000000000020'").update();
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
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void syncWithAFreshRosterAppliesEveryQueuedMarkingAndPersistsTheClientMarkedAt() throws Exception {
        String token = obtainSanchalakAccessToken();
        Instant fresh = Instant.now().minus(Duration.ofHours(1));
        Instant clientMarkedAt = Instant.now().minus(Duration.ofMinutes(5)).truncatedTo(ChronoUnit.MILLIS);

        String body = """
                {
                  "rosterVersion": "%s",
                  "markings": [
                    { "occurrenceId": "00000000-0000-0000-0000-000000000020",
                      "personId":     "00000000-0000-0000-0000-000000000101",
                      "present":      true,
                      "clientMarkedAt": "%s" }
                  ]
                }
                """.formatted(fresh, clientMarkedAt);

        mockMvc.perform(post("/api/sync")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedCount").value(1));

        Instant persisted = jdbc.sql("""
                SELECT client_marked_at FROM attendance_markings
                WHERE occurrence_id = '00000000-0000-0000-0000-000000000020'
                  AND person_id = '00000000-0000-0000-0000-000000000101'
                """).query((rs, n) -> rs.getTimestamp("client_marked_at").toInstant()).single();
        org.assertj.core.api.Assertions.assertThat(persisted).isEqualTo(clientMarkedAt);
    }

    @Test
    void syncWithARosterVersionOlderThanSevenDaysRejectsWithCodeRosterStale() throws Exception {
        String token = obtainSanchalakAccessToken();
        Instant stale = Instant.now().minus(Duration.ofDays(7)).minus(Duration.ofMinutes(1));

        String body = """
                {
                  "rosterVersion": "%s",
                  "markings": [
                    { "occurrenceId": "00000000-0000-0000-0000-000000000020",
                      "personId":     "00000000-0000-0000-0000-000000000101",
                      "present":      true,
                      "clientMarkedAt": "%s" }
                  ]
                }
                """.formatted(stale, Instant.now());

        mockMvc.perform(post("/api/sync")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROSTER_STALE"));

        Integer rowCount = jdbc.sql("""
                SELECT COUNT(*) AS c FROM attendance_markings
                WHERE occurrence_id = '00000000-0000-0000-0000-000000000020'
                """).query((rs, n) -> rs.getInt("c")).single();
        org.assertj.core.api.Assertions.assertThat(rowCount).isZero();
    }

    @Test
    void syncAppliesLastWriteWinsWhenTwoDevicesPostMarkingsForTheSamePersonOutOfOrder() throws Exception {
        String token = obtainSanchalakAccessToken();
        Instant fresh = Instant.now().minus(Duration.ofHours(1));
        Instant earlier = Instant.now().minus(Duration.ofMinutes(10)).truncatedTo(ChronoUnit.MILLIS);
        Instant later = earlier.plus(Duration.ofMinutes(5));

        // Device B (the laggard) arrives FIRST with the LATER markedAt — present.
        mockMvc.perform(post("/api/sync")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rosterVersion": "%s",
                                  "markings": [
                                    { "occurrenceId": "00000000-0000-0000-0000-000000000020",
                                      "personId":     "00000000-0000-0000-0000-000000000102",
                                      "present":      true,
                                      "clientMarkedAt": "%s" }
                                  ]
                                }
                                """.formatted(fresh, later)))
                .andExpect(status().isOk());

        // Device A then catches up with the EARLIER markedAt — absent. LWW says present wins.
        mockMvc.perform(post("/api/sync")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rosterVersion": "%s",
                                  "markings": [
                                    { "occurrenceId": "00000000-0000-0000-0000-000000000020",
                                      "personId":     "00000000-0000-0000-0000-000000000102",
                                      "present":      false,
                                      "clientMarkedAt": "%s" }
                                  ]
                                }
                                """.formatted(fresh, earlier)))
                .andExpect(status().isOk());

        Boolean present = jdbc.sql("""
                SELECT present FROM attendance_markings
                WHERE occurrence_id = '00000000-0000-0000-0000-000000000020'
                  AND person_id = '00000000-0000-0000-0000-000000000102'
                """).query((rs, n) -> rs.getBoolean("present")).single();
        org.assertj.core.api.Assertions.assertThat(present).isTrue();
    }

    @Test
    void syncIsIdempotentWhenTheSameBatchIsReplayed() throws Exception {
        String token = obtainSanchalakAccessToken();
        Instant fresh = Instant.now().minus(Duration.ofHours(1));
        Instant clientMarkedAt = Instant.now().minus(Duration.ofMinutes(5)).truncatedTo(ChronoUnit.MILLIS);
        String body = """
                {
                  "rosterVersion": "%s",
                  "markings": [
                    { "occurrenceId": "00000000-0000-0000-0000-000000000020",
                      "personId":     "00000000-0000-0000-0000-000000000103",
                      "present":      true,
                      "clientMarkedAt": "%s" }
                  ]
                }
                """.formatted(fresh, clientMarkedAt);

        mockMvc.perform(post("/api/sync").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        mockMvc.perform(post("/api/sync").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());

        Integer rowCount = jdbc.sql("""
                SELECT COUNT(*) AS c FROM attendance_markings
                WHERE occurrence_id = '00000000-0000-0000-0000-000000000020'
                  AND person_id = '00000000-0000-0000-0000-000000000103'
                """).query((rs, n) -> rs.getInt("c")).single();
        org.assertj.core.api.Assertions.assertThat(rowCount).isEqualTo(1);
    }

    @Test
    void currentRosterIncludesARosterVersionForTheClientToEchoBackInSync() throws Exception {
        String token = obtainSanchalakAccessToken();

        mockMvc.perform(get("/api/sanchalak/current-roster").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rosterVersion").exists());
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
