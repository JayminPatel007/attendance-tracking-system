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
import org.junit.jupiter.api.BeforeEach;
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
 * Slice 5 end-to-end: Sanchalak-only Sabha-shaping (cancel / revert / reschedule
 * / venue-override) wired from REST through the Authorization Engine, the
 * aggregate, JDBC persistence, and the state-transition audit log. The negative
 * path proves the Sah-Sanchalak is rejected with 403 and leaves no side effects.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OccurrenceShapingIntegrationTest {

    private static final UUID SABHA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OCCURRENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000040");

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

    @BeforeEach
    void seedScheduledOccurrence() {
        // A Scheduled Occurrence dated a week out, so it is neither auto-opened nor
        // past its revert grace window during the test.
        jdbc.sql("""
                INSERT INTO occurrences (id, sabha_id, occurrence_date, state)
                VALUES (?, ?, CURRENT_DATE + 7, 'SCHEDULED')
                """)
                .param(OCCURRENCE_ID).param(SABHA_ID).update();
    }

    @AfterEach
    void cleanUp() {
        jdbc.sql("DELETE FROM occurrence_state_transitions WHERE occurrence_id = ?")
                .param(OCCURRENCE_ID).update();
        jdbc.sql("DELETE FROM occurrences WHERE id = ?").param(OCCURRENCE_ID).update();
    }

    @Test
    void sanchalakCancelsAScheduledOccurrenceWithReasonAndTheAuditTrailRecordsIt() throws Exception {
        String token = token("sanchalak");

        mockMvc.perform(post("/api/occurrences/" + OCCURRENCE_ID + "/cancel")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"reason\": \"Festival clash\" }"))
                .andExpect(status().isOk());

        assertThat(state()).isEqualTo("CANCELLED");
        String reason = jdbc.sql("""
                SELECT reason FROM occurrence_state_transitions
                WHERE occurrence_id = ? AND action = 'CANCEL'
                """).param(OCCURRENCE_ID).query((rs, n) -> rs.getString("reason")).single();
        assertThat(reason).isEqualTo("Festival clash");
    }

    @Test
    void sahSanchalakCancelIsForbiddenAndLeavesTheOccurrenceUntouched() throws Exception {
        String token = token("sah-sanchalak");

        mockMvc.perform(post("/api/occurrences/" + OCCURRENCE_ID + "/cancel")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"reason\": \"trying anyway\" }"))
                .andExpect(status().isForbidden());

        assertThat(state()).isEqualTo("SCHEDULED");
        assertThat(transitionCount()).isZero();
    }

    @Test
    void sanchalakReschedulesToANewDateWithoutTouchingTheStandingSchedule() throws Exception {
        String token = token("sanchalak");

        mockMvc.perform(post("/api/occurrences/" + OCCURRENCE_ID + "/reschedule")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "date": "2026-06-20", "startTime": "18:00", "endTime": "19:30" }
                                """))
                .andExpect(status().isOk());

        assertThat(state()).isEqualTo("RESCHEDULED");
        String rescheduledDate = jdbc.sql("SELECT rescheduled_date FROM occurrences WHERE id = ?")
                .param(OCCURRENCE_ID).query((rs, n) -> rs.getString("rescheduled_date")).single();
        assertThat(rescheduledDate).isEqualTo("2026-06-20");
    }

    @Test
    void sanchalakSetsAVenueOverrideLeavingStateScheduled() throws Exception {
        String token = token("sanchalak");

        mockMvc.perform(post("/api/occurrences/" + OCCURRENCE_ID + "/venue-override")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"venue\": \"Community Hall Annexe\" }"))
                .andExpect(status().isOk());

        assertThat(state()).isEqualTo("SCHEDULED");
        String venue = jdbc.sql("SELECT venue_override FROM occurrences WHERE id = ?")
                .param(OCCURRENCE_ID).query((rs, n) -> rs.getString("venue_override")).single();
        assertThat(venue).isEqualTo("Community Hall Annexe");
    }

    @Test
    void sanchalakRevertsACancelledOccurrenceAndBothEventsRemainInTheAuditTrail() throws Exception {
        String token = token("sanchalak");

        mockMvc.perform(post("/api/occurrences/" + OCCURRENCE_ID + "/cancel")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"reason\": \"Festival clash\" }"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/occurrences/" + OCCURRENCE_ID + "/revert")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        assertThat(state()).isEqualTo("SCHEDULED");
        assertThat(transitionCount()).isEqualTo(2);
    }

    private String state() {
        return jdbc.sql("SELECT state FROM occurrences WHERE id = ?")
                .param(OCCURRENCE_ID).query((rs, n) -> rs.getString("state")).single();
    }

    private int transitionCount() {
        return jdbc.sql("SELECT COUNT(*) AS c FROM occurrence_state_transitions WHERE occurrence_id = ?")
                .param(OCCURRENCE_ID).query((rs, n) -> rs.getInt("c")).single();
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
