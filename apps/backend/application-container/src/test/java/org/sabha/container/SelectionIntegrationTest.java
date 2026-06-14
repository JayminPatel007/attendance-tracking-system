package org.sabha.container;

import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice 16 end-to-end (ADR-0006): a Regular Sanchalak nominates a Roster Person
 * for the selective YSS track (mobile JWT), the nomination surfaces in the
 * demographic Nirdeshak's queue, the Nirdeshak approves (web BFF) adding the
 * selective Home Sabha while leaving the Regular one intact, and a later
 * deselection removes only the selective Home Sabha. Wired from REST/BFF through
 * the orchestrator, the aggregate, and JDBC.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SelectionIntegrationTest extends KeycloakIntegrationTest {

    // slice-2/002-seed: REGULAR_YUVAK tracer Sabha, its Sanchalak, and a Roster Person.
    private static final UUID REGULAR_SABHA = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ROSTER_PERSON = UUID.fromString("00000000-0000-0000-0000-000000000101");
    // slice-16/001-seed: the YSS_YUVAK Sabha in the tracer Kshetra, and the demographic Nirdeshak.
    private static final UUID SELECTIVE_SABHA = UUID.fromString("00000000-0000-0000-0000-000000000016");
    private static final String NIRDESHAK_SUBJECT = "00000000-0000-0000-0000-000000000062";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcClient jdbc;

    @Test
    void aSanchalakNominatesTheNirdeshakApprovesAndTheSelectiveHomeSabhaIsAddedAdditively() throws Exception {
        UUID nominationId = nominate();

        // The nomination surfaces in the demographic Nirdeshak's queue.
        mockMvc.perform(get("/bff/selection/nominations")
                        .with(oidcLogin().idToken(t -> t.subject(NIRDESHAK_SUBJECT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nominationId").value(nominationId.toString()))
                .andExpect(jsonPath("$[0].personId").value(ROSTER_PERSON.toString()))
                .andExpect(jsonPath("$[0].track").value("YSS"));

        approve(nominationId);

        // Additive: the Person now holds both the Regular and the selective Home Sabha.
        assertThat(homeSabhaIds(ROSTER_PERSON))
                .contains(REGULAR_SABHA.toString())
                .contains(SELECTIVE_SABHA.toString());
        assertThat(nominationStatus(nominationId)).isEqualTo("APPROVED");

        // The queue is now empty (no PENDING nominations remain).
        mockMvc.perform(get("/bff/selection/nominations")
                        .with(oidcLogin().idToken(t -> t.subject(NIRDESHAK_SUBJECT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // The approved Person now appears in the Nirdeshak's selected list — the
        // source the web deselect action acts on (carries person + selective Sabha).
        mockMvc.perform(get("/bff/selection/selected")
                        .with(oidcLogin().idToken(t -> t.subject(NIRDESHAK_SUBJECT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].personId").value(ROSTER_PERSON.toString()))
                .andExpect(jsonPath("$[0].selectiveSabhaId").value(SELECTIVE_SABHA.toString()))
                .andExpect(jsonPath("$[0].track").value("YSS"));

        deselect();

        // Deselection removes only the selective Home Sabha; the Regular one stays.
        assertThat(homeSabhaIds(ROSTER_PERSON))
                .contains(REGULAR_SABHA.toString())
                .doesNotContain(SELECTIVE_SABHA.toString());
        assertThat(nominationStatus(nominationId)).isEqualTo("DESELECTED");

        // A deselected Person drops out of the selected list.
        mockMvc.perform(get("/bff/selection/selected")
                        .with(oidcLogin().idToken(t -> t.subject(NIRDESHAK_SUBJECT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void aSecondPendingNominationForTheSamePersonIsRejectedAsAConflict() throws Exception {
        nominate();

        String token = token("sanchalak");
        mockMvc.perform(post("/api/sanchalak/nominations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"personId\": \"" + ROSTER_PERSON + "\", "
                                + "\"regularSabhaId\": \"" + REGULAR_SABHA + "\" }"))
                .andExpect(status().isConflict());
    }

    private UUID nominate() throws Exception {
        String token = token("sanchalak");
        MvcResult result = mockMvc.perform(post("/api/sanchalak/nominations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"personId\": \"" + ROSTER_PERSON + "\", "
                                + "\"regularSabhaId\": \"" + REGULAR_SABHA + "\" }"))
                .andExpect(status().isOk())
                .andReturn();
        String id = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString())
                .get("nominationId").asText();
        return UUID.fromString(id);
    }

    private void approve(UUID nominationId) throws Exception {
        Cookie xsrf = xsrfCookie();
        mockMvc.perform(post("/bff/selection/nominations/" + nominationId + "/approve")
                        .with(oidcLogin().idToken(t -> t.subject(NIRDESHAK_SUBJECT)))
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue()))
                .andExpect(status().isNoContent());
    }

    private void deselect() throws Exception {
        Cookie xsrf = xsrfCookie();
        mockMvc.perform(post("/bff/selection/deselect")
                        .with(oidcLogin().idToken(t -> t.subject(NIRDESHAK_SUBJECT)))
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"personId\": \"" + ROSTER_PERSON + "\", "
                                + "\"selectiveSabhaId\": \"" + SELECTIVE_SABHA + "\" }"))
                .andExpect(status().isNoContent());
    }

    private Cookie xsrfCookie() throws Exception {
        return mockMvc.perform(get("/bff/selection/nominations")
                        .with(oidcLogin().idToken(t -> t.subject(NIRDESHAK_SUBJECT))))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
    }

    private String homeSabhaIds(UUID personId) {
        return jdbc.sql("SELECT string_agg(sabha_id::text, ',' ORDER BY sabha_id) AS ids "
                        + "FROM home_sabhas WHERE person_id = ?")
                .param(personId)
                .query((rs, n) -> rs.getString("ids")).single();
    }

    private String nominationStatus(UUID nominationId) {
        return jdbc.sql("SELECT status FROM selection_nominations WHERE id = ?")
                .param(nominationId)
                .query(String.class).single();
    }

    private String token(String username) throws Exception {
        String body = "grant_type=password"
                + "&client_id=sabha-test"
                + "&username=" + username
                + "&password=" + URLEncoder.encode("changeme123!", StandardCharsets.UTF_8)
                + "&scope=openid";

        HttpResponse<String> resp = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create(KEYCLOAK.getAuthServerUrl()
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
