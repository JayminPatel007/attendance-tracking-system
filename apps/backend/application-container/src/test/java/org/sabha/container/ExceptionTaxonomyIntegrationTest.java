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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The exception taxonomy gives common-domain base types their HTTP semantics
 * once (issue #78). The canonical mapping under test: an authenticated caller
 * whose Keycloak subject maps to no local {@code users} row is <em>forbidden</em>
 * (403), never a server error (500) — and that holds identically across bounded
 * contexts, since {@code CallerUnknownException} is the one common-domain failure
 * mode of {@link org.sabha.common.CallerResolver}. Before this issue the identity
 * context threw its own copy that no advice mapped, so identity endpoints 500'd.
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
}
