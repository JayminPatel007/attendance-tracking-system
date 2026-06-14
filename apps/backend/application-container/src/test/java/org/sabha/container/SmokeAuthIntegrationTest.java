package org.sabha.container;

import org.springframework.transaction.annotation.Transactional;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SmokeAuthIntegrationTest extends KeycloakIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcClient jdbc;

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

        // A test-private id (not a fixed seed id): the suite shares one DB, and the
        // reopen seed already owns …0021. This row rolls back with the test transaction.
        UUID finalizedOccurrenceId = UUID.randomUUID();
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
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").exists())
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
                        .uri(URI.create(KEYCLOAK.getAuthServerUrl() + "/realms/sabha/protocol/openid-connect/token"))
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
