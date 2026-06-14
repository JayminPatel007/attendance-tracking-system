package org.sabha.container;

import org.springframework.transaction.annotation.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;

/**
 * Occurrence reopen across the web BFF (Slice 13, ADR-0001, ADR-0022). The
 * seeded Nirikshak on (Kshetra Tracer, YUVAK) lists and reopens the seeded
 * Finalized Occurrence; the Sanchalak — who owns shaping but not reopen — is
 * rejected by the Authorization Engine. Cookie/session authenticated via the OIDC
 * login post-processor, mirroring {@link WebBffIntegrationTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OccurrenceReopenIntegrationTest extends KeycloakIntegrationTest {

    /** Seeded Nirikshak's Keycloak subject (slice-13 seed + realm-sabha.json). */
    private static final String NIRIKSHAK_SUBJECT = "00000000-0000-0000-0000-000000000052";
    /** Seeded Sanchalak — owns shaping, not reopen. */
    private static final String SANCHALAK_SUBJECT = "00000000-0000-0000-0000-000000000005";
    /** Seeded Finalized Occurrence asserted as never-reopened (read-only tests). */
    private static final String FINALIZED_OCCURRENCE = "00000000-0000-0000-0000-000000000021";
    /** A second Finalized Occurrence the reopen-mutation test acts on, to avoid racing shared state. */
    private static final String REOPENABLE_OCCURRENCE = "00000000-0000-0000-0000-000000000022";

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry r) {
        registerMkBootstrap(r);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcClient jdbc;

    @Test
    void aNirikshakSeesTheFinalizedOccurrenceInTheirScopeNotYetReopened() throws Exception {
        mockMvc.perform(get("/bff/occurrences").with(oidcLogin().idToken(t -> t.subject(NIRIKSHAK_SUBJECT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.occurrenceId == '" + FINALIZED_OCCURRENCE + "')].state")
                        .value(Matchers.hasItem("FINALIZED")))
                .andExpect(jsonPath("$[?(@.occurrenceId == '" + FINALIZED_OCCURRENCE + "')].reopened")
                        .value(Matchers.hasItem(false)));
    }

    @Test
    void aNirikshakReopensTheFinalizedOccurrenceAndItThenCarriesTheBadgeAndReason() throws Exception {
        Cookie xsrf = mockMvc.perform(get("/bff/occurrences").with(oidcLogin().idToken(t -> t.subject(NIRIKSHAK_SUBJECT))))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/bff/occurrences/" + REOPENABLE_OCCURRENCE + "/reopen")
                        .with(oidcLogin().idToken(t -> t.subject(NIRIKSHAK_SUBJECT)))
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType("application/json")
                        .content("{\"reason\":\"Forgot to mark Ravi\"}"))
                .andExpect(status().isNoContent());

        String state = jdbc.sql("SELECT state FROM occurrences WHERE id = ?")
                .param(java.util.UUID.fromString(REOPENABLE_OCCURRENCE))
                .query(String.class).single();
        org.assertj.core.api.Assertions.assertThat(state).isEqualTo("OPEN_FOR_MARKING");

        String reason = jdbc.sql("""
                SELECT reason FROM occurrence_state_transitions
                WHERE occurrence_id = ? AND action = 'REOPEN'
                ORDER BY at_timestamp DESC LIMIT 1
                """)
                .param(java.util.UUID.fromString(REOPENABLE_OCCURRENCE))
                .query(String.class).single();
        org.assertj.core.api.Assertions.assertThat(reason).isEqualTo("Forgot to mark Ravi");

        mockMvc.perform(get("/bff/occurrences").with(oidcLogin().idToken(t -> t.subject(NIRIKSHAK_SUBJECT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.occurrenceId == '" + REOPENABLE_OCCURRENCE + "')].reopened")
                        .value(Matchers.hasItem(true)))
                .andExpect(jsonPath("$[?(@.occurrenceId == '" + REOPENABLE_OCCURRENCE + "')].lastReopenReason")
                        .value(Matchers.hasItem("Forgot to mark Ravi")));
    }

    @Test
    void aSanchalakReopenAttemptIsForbidden() throws Exception {
        Cookie xsrf = mockMvc.perform(get("/bff/me").with(oidcLogin().idToken(t -> t.subject(SANCHALAK_SUBJECT))))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/bff/occurrences/" + FINALIZED_OCCURRENCE + "/reopen")
                        .with(oidcLogin().idToken(t -> t.subject(SANCHALAK_SUBJECT)))
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType("application/json")
                        .content("{\"reason\":\"let me in\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void reopenWithoutAReasonIsUnprocessable() throws Exception {
        Cookie xsrf = mockMvc.perform(get("/bff/occurrences").with(oidcLogin().idToken(t -> t.subject(NIRIKSHAK_SUBJECT))))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/bff/occurrences/" + FINALIZED_OCCURRENCE + "/reopen")
                        .with(oidcLogin().idToken(t -> t.subject(NIRIKSHAK_SUBJECT)))
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType("application/json")
                        .content("{\"reason\":\"   \"}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
