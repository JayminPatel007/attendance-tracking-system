package org.sabha.container;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The exception taxonomy gives common-domain base types their HTTP semantics
 * once (issue #78). The canonical mapping under test: an authenticated caller
 * whose Keycloak subject maps to no local {@code users} row is <em>forbidden</em>
 * (403), never a server error (500) — and that holds identically across bounded
 * contexts, since {@code CallerUnknownException} is the one common-domain failure
 * mode of {@link org.sabha.common.CallerResolver}. Before this issue the identity
 * context threw its own copy that no advice mapped, so identity endpoints 500'd.
 *
 * <p>Issue #107 extends this to the web BFF: routing the unknown-caller 403
 * through {@code requireUserId} (instead of a controller-local bodyless
 * {@code 403.build()}) means every caller-unknown refusal — REST or BFF — carries
 * the same RFC 9457 {@code problem+json} body.
 *
 * <p>ADR-0030 moves where the refusal happens — the {@code @CurrentUser} resolver
 * at the edge, before any use case runs — without changing what it is. Two cases
 * change status: a read that used to fuse "unknown caller" into its empty result
 * now refuses, and a subject that is not a UUID is refused rather than bursting
 * out of {@code UUID.fromString} as a 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExceptionTaxonomyIntegrationTest extends KeycloakIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void authenticatedCallerWithNoLocalUserIsForbiddenFromAnIdentityEndpoint() throws Exception {
        String body = """
                { "fullName": "Nobody", "gender": "MALE",
                  "mobile": "+919820000078",
                  "homeSabhaId": "00000000-0000-0000-0000-000000000002" }
                """;

        mockMvc.perform(post("/api/directory/persons")
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedCallerWithNoLocalUserIsForbiddenFromAnAttendanceEndpoint() throws Exception {
        String body = """
                { "rosterVersion": "2099-01-01T00:00:00Z", "markings": [] }
                """;

        mockMvc.perform(post("/api/sync")
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    /**
     * Before ADR-0030 this read resolved the caller inside the use case and folded
     * an unknown one into {@code Optional.empty()} → 404, which said "no Sabha to
     * mark" when the truth was "I do not know who you are".
     */
    @Test
    void unknownCallerOnAReadThatUsedToAnswerNotFoundIsNowForbidden() throws Exception {
        mockMvc.perform(get("/api/sanchalak/current-roster")
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    /**
     * A subject claim that is not a UUID — a service account, a second identity
     * provider, a hand-rolled test token — used to throw {@link
     * IllegalArgumentException} out of {@code UUID.fromString} at the top of the
     * controller and surface as a 500.
     */
    @Test
    void aSubjectThatIsNotAUuidIsForbiddenRatherThanAServerError() throws Exception {
        mockMvc.perform(get("/api/sanchalak/current-roster")
                        .with(jwt().jwt(j -> j.subject("service-account-batch"))))
                .andExpect(status().isForbidden());
    }

    /**
     * The other half of the contract: a caller the system <em>does</em> know still
     * gets the empty answer, not a refusal. The 404/empty result now means only
     * "nothing to show".
     */
    @Test
    void aKnownCallerWithNothingToShowStillGetsTheEmptyAnswer() throws Exception {
        mockMvc.perform(get("/api/sanchalak/monthly-sabhas")
                        .with(jwt().jwt(j -> j.subject("00000000-0000-0000-0000-000000000005"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * The load-bearing 404. {@code occurrence_control_api.dart} reads a 404 here as
     * "no Occurrence right now" and renders an empty state, so this code must keep
     * meaning that — for a <em>known</em> caller — even though an unknown one has
     * moved to 403. The Slice 13 reopen-tier Nirikshak is a real User who presides
     * over no Sabha as Sanchalak.
     */
    @Test
    void aKnownCallerWithNoCurrentOccurrenceStillGetsNotFound() throws Exception {
        mockMvc.perform(get("/api/sanchalak/current-occurrence")
                        .with(jwt().jwt(j -> j.subject("00000000-0000-0000-0000-000000000052"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void callerUnknownOnAWebBffEndpointReturnsAProblemDetailNotABodylessForbidden() throws Exception {
        mockMvc.perform(get("/bff/selection/nominations")
                        .with(oidcLogin().idToken(t -> t.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").exists());
    }
}
