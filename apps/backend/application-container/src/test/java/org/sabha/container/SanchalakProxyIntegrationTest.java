package org.sabha.container;

import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import org.hamcrest.Matchers;
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

/**
 * Nirikshak Sanchalak-proxy mode across the web BFF (Slice 14, ADR-0001,
 * ADR-0022). The seeded Nirikshak is assigned to the tracer Sabha and: sees it in
 * the picker with the Sanchalak's "last seen" hint; cancels a Scheduled Occurrence
 * as a proxy, which the audit log attributes to them acting on behalf of the absent
 * Sanchalak; and is rejected on an Occurrence of a Sabha outside their assignment.
 * A Sanchalak shaping their own Sabha through the same endpoint carries no
 * on-behalf-of attribution. Cookie/session authenticated via the OIDC login
 * post-processor, mirroring {@link OccurrenceReopenIntegrationTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SanchalakProxyIntegrationTest extends KeycloakIntegrationTest {

    /** Seeded Nirikshak's Keycloak subject, assigned to the Proxy Sabha (slice-13 + slice-14 seed). */
    private static final String NIRIKSHAK_SUBJECT = "00000000-0000-0000-0000-000000000052";
    /** Seeded Sanchalak of the Proxy Sabha. */
    private static final String PROXY_SANCHALAK_SUBJECT = "00000000-0000-0000-0000-0000000000b5";

    private static final UUID NIRIKSHAK_USER = UUID.fromString("00000000-0000-0000-0000-000000000051");
    private static final UUID PROXY_SANCHALAK_USER = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final String PROXY_SABHA = "00000000-0000-0000-0000-0000000000b0";
    /** Scheduled Occurrence the Nirikshak cancels as proxy. */
    private static final UUID PROXY_CANCEL_OCCURRENCE = UUID.fromString("00000000-0000-0000-0000-0000000000b3");
    /** Scheduled Occurrence the Sanchalak shapes directly. */
    private static final String DIRECT_OCCURRENCE = "00000000-0000-0000-0000-0000000000b6";
    /** A Finalized Occurrence on the tracer Sabha, which is NOT assigned to the Nirikshak. */
    private static final String UNASSIGNED_OCCURRENCE = "00000000-0000-0000-0000-000000000021";

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry r) {
        registerMkBootstrap(r);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcClient jdbc;

    @Test
    void aNirikshakSeesTheirAssignedSabhaWithTheSanchalaksLastSeenHint() throws Exception {
        mockMvc.perform(get("/bff/proxy/sabhas").with(oidcLogin().idToken(t -> t.subject(NIRIKSHAK_SUBJECT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sabhaId == '" + PROXY_SABHA + "')].sanchalakName")
                        .value(Matchers.hasItem("Proxy Sanchalak")))
                // Last seen = GREATEST(login 06-01, sync 06-03, marking none) = the seeded sync.
                .andExpect(jsonPath("$[?(@.sabhaId == '" + PROXY_SABHA + "')].lastSeenAt")
                        .value(Matchers.hasItem("2026-06-03T19:30:00Z")));
    }

    @Test
    void aNirikshakCancelsAnOccurrenceAsProxyAuditedOnBehalfOfTheAbsentSanchalak() throws Exception {
        Cookie xsrf = mockMvc.perform(get("/bff/proxy/sabhas")
                        .with(oidcLogin().idToken(t -> t.subject(NIRIKSHAK_SUBJECT))))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/bff/proxy/occurrences/" + PROXY_CANCEL_OCCURRENCE + "/cancel")
                        .with(oidcLogin().idToken(t -> t.subject(NIRIKSHAK_SUBJECT)))
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType("application/json")
                        .content("{\"reason\":\"Sanchalak unreachable this week\"}"))
                .andExpect(status().isNoContent());

        String state = jdbc.sql("SELECT state FROM occurrences WHERE id = ?")
                .param(PROXY_CANCEL_OCCURRENCE)
                .query(String.class).single();
        org.assertj.core.api.Assertions.assertThat(state).isEqualTo("CANCELLED");

        UUID actor = jdbc.sql("""
                SELECT actor_user_id FROM occurrence_state_transitions
                WHERE occurrence_id = ? AND action = 'CANCEL'
                ORDER BY at_timestamp DESC LIMIT 1
                """)
                .param(PROXY_CANCEL_OCCURRENCE)
                .query(UUID.class).single();
        UUID onBehalfOf = jdbc.sql("""
                SELECT on_behalf_of_user_id FROM occurrence_state_transitions
                WHERE occurrence_id = ? AND action = 'CANCEL'
                ORDER BY at_timestamp DESC LIMIT 1
                """)
                .param(PROXY_CANCEL_OCCURRENCE)
                .query(UUID.class).single();

        org.assertj.core.api.Assertions.assertThat(actor).isEqualTo(NIRIKSHAK_USER);
        org.assertj.core.api.Assertions.assertThat(onBehalfOf).isEqualTo(PROXY_SANCHALAK_USER);
    }

    @Test
    void aSanchalakShapingTheirOwnSabhaThroughTheProxyEndpointHasNoOnBehalfOfAttribution() throws Exception {
        Cookie xsrf = mockMvc.perform(get("/bff/me")
                        .with(oidcLogin().idToken(t -> t.subject(PROXY_SANCHALAK_SUBJECT))))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/bff/proxy/occurrences/" + DIRECT_OCCURRENCE + "/venue-override")
                        .with(oidcLogin().idToken(t -> t.subject(PROXY_SANCHALAK_SUBJECT)))
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType("application/json")
                        .content("{\"venue\":\"Community Hall Annexe\"}"))
                .andExpect(status().isNoContent());

        UUID onBehalfOf = jdbc.sql("""
                SELECT on_behalf_of_user_id FROM occurrence_state_transitions
                WHERE occurrence_id = ? AND action = 'OVERRIDE_VENUE'
                ORDER BY at_timestamp DESC LIMIT 1
                """)
                .param(UUID.fromString(DIRECT_OCCURRENCE))
                .query(UUID.class).optional().orElse(null);

        org.assertj.core.api.Assertions.assertThat(onBehalfOf).isNull();
    }

    @Test
    void aNirikshakIsForbiddenOnAnOccurrenceOfASabhaOutsideTheirAssignment() throws Exception {
        Cookie xsrf = mockMvc.perform(get("/bff/proxy/sabhas")
                        .with(oidcLogin().idToken(t -> t.subject(NIRIKSHAK_SUBJECT))))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/bff/proxy/occurrences/" + UNASSIGNED_OCCURRENCE + "/venue-override")
                        .with(oidcLogin().idToken(t -> t.subject(NIRIKSHAK_SUBJECT)))
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType("application/json")
                        .content("{\"venue\":\"Somewhere else\"}"))
                .andExpect(status().isForbidden());
    }
}
