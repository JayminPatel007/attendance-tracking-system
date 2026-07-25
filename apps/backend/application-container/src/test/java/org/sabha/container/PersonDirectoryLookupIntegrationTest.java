package org.sabha.container;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The two Directory lookups the mobile app opens its flows with, over HTTP
 * (issue #104). Both used to share one path and one {@code ResponseEntity<Object>}
 * handler, which is what forced the mobile client to parse the response by hand;
 * they are now separate paths with a response shape each. What matters to the
 * caller is pinned here: the mobile hit is a Person, the mobile miss is a
 * {@code 404} it reads as "this number is new", and the name lookup still
 * answers with the soft-warn candidate list.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PersonDirectoryLookupIntegrationTest extends KeycloakIntegrationTest {

    // Seeded by slice-2/002-seed.sql and slice-6/001-person-directory.sql.
    private static final UUID KSHETRA_TRACER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SEEDED_RAMESH = UUID.fromString("00000000-0000-0000-0000-000000000110");
    private static final String RAMESH_MOBILE = "+910000000110";

    @Autowired
    MockMvc mockMvc;

    @Test
    void anExactMobileLookupAnswersWithThatPerson() throws Exception {
        mockMvc.perform(get("/api/directory/persons").param("mobile", RAMESH_MOBILE).with(caller()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SEEDED_RAMESH.toString()))
                .andExpect(jsonPath("$.fullName").value("Ramesh Shah"))
                .andExpect(jsonPath("$.gender").value("MALE"));
    }

    @Test
    void anUnknownMobileIsANotFoundTheAddFlowReadsAsANewNumber() throws Exception {
        mockMvc.perform(get("/api/directory/persons").param("mobile", "+919999999104").with(caller()))
                .andExpect(status().isNotFound());
    }

    /** A blank number is a malformed request, not a Person who doesn't exist. */
    @Test
    void aBlankMobileIsRejectedRatherThanAnsweredWithNotFound() throws Exception {
        mockMvc.perform(get("/api/directory/persons").param("mobile", " ").with(caller()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aNameLookupAnswersWithTheKshetrasCloseMatches() throws Exception {
        mockMvc.perform(get("/api/directory/name-search")
                        .param("kshetraId", KSHETRA_TRACER.toString())
                        .param("name", "Ramish Shah")
                        .with(caller()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.personId == '%s')].fullName".formatted(SEEDED_RAMESH))
                        .value("Ramesh Shah"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor caller() {
        return jwt().jwt(j -> j.subject(UUID.randomUUID().toString()));
    }
}
