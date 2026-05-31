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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice 8A end-to-end: a Sanchalak initiates a Verified Home Sabha Transfer for a
 * Person whose REGULAR_YUVAK Home Sabha is in another Kshetra, then confirms with
 * the OTP — wired from REST through the orchestrator, the aggregate, the OTP
 * gateway, and JDBC. Proves the correct OTP swaps the demographic's Home Sabha to
 * the destination, and that a wrong OTP is rejected (422) and leaves it untouched.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class HomeSabhaTransferIntegrationTest {

    // Seeded by slice-2/002-seed.sql: the REGULAR_YUVAK tracer Sabha and its Sanchalak.
    private static final UUID DESTINATION_SABHA = UUID.fromString("00000000-0000-0000-0000-000000000002");
    // Seeded by slice-8/001: a Person with their own mobile whose REGULAR_YUVAK Home
    // Sabha is in a different Kshetra — the lateral-transfer subject.
    private static final UUID TRANSFER_PERSON = UUID.fromString("00000000-0000-0000-0000-000000000303");
    private static final UUID OTHER_YUVAK_SABHA = UUID.fromString("00000000-0000-0000-0000-000000000302");

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
        // Drop transfer rows (also clears the per-mobile rate-limit / cooldown
        // history) and restore the Person's Home Sabha to its seeded state.
        jdbc.sql("DELETE FROM home_sabha_transfers").update();
        jdbc.sql("DELETE FROM home_sabhas WHERE person_id = ?").param(TRANSFER_PERSON).update();
        jdbc.sql("INSERT INTO home_sabhas (person_id, sabha_id) VALUES (?, ?)")
                .param(TRANSFER_PERSON).param(OTHER_YUVAK_SABHA).update();
    }

    @Test
    void verifiedTransferSwapsThePersonsHomeSabhaForThatDemographic() throws Exception {
        String token = token("sanchalak");

        UUID transferId = initiate(token);
        String otp = jdbc.sql("SELECT otp_code FROM home_sabha_transfers WHERE id = ?")
                .param(transferId).query((rs, n) -> rs.getString("otp_code")).single();

        mockMvc.perform(post("/api/home-sabha-transfers/" + transferId + "/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"otpCode\": \"" + otp + "\" }"))
                .andExpect(status().isOk());

        assertThat(homeSabhaIds(TRANSFER_PERSON)).isEqualTo(DESTINATION_SABHA.toString());
    }

    @Test
    void wrongOtpIsRejectedAndLeavesTheHomeSabhaUnchanged() throws Exception {
        String token = token("sanchalak");

        UUID transferId = initiate(token);

        mockMvc.perform(post("/api/home-sabha-transfers/" + transferId + "/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"otpCode\": \"000000\" }"))
                .andExpect(status().isUnprocessableEntity());

        assertThat(homeSabhaIds(TRANSFER_PERSON)).isEqualTo(OTHER_YUVAK_SABHA.toString());
    }

    private UUID initiate(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/home-sabha-transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"personId\": \"" + TRANSFER_PERSON + "\", "
                                + "\"destinationSabhaId\": \"" + DESTINATION_SABHA + "\" }"))
                .andExpect(status().isOk())
                .andReturn();
        String transferId = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString())
                .get("transferId").asText();
        return UUID.fromString(transferId);
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
