package org.sabha.container;

import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Issue #73: the BFF/REST contract is an artifact, not a convention. springdoc
 * renders an OpenAPI document from the live controllers; the typed web and mobile
 * clients are generated from it. This test pins two things:
 *
 * <ul>
 *   <li>the spec endpoint serves a document that covers <em>both</em> HTTP chains
 *       (ADR-0022) — the public {@code /api/**} resource-server chain and the
 *       session-cookie {@code /bff/**} chain;</li>
 *   <li>the committed {@code apps/backend/openapi.json} stays in lock-step with the
 *       controllers — a contract change that isn't regenerated fails the build,
 *       so the checked-in spec the clients generate from is never stale.</li>
 * </ul>
 *
 * <p>To regenerate the committed spec after an intentional contract change, run the
 * module's tests with {@code -Dopenapi.regenerate=true}; the drift assertion then
 * rewrites the file from the live controllers instead of failing.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractIntegrationTest extends KeycloakIntegrationTest {

    /**
     * The committed spec lives at {@code apps/backend/openapi.json} — one level up
     * from this module, beside the backend it describes and reachable as a sibling
     * relative path from the web and mobile codegen scripts. Surefire runs with the
     * module directory as its working directory.
     */
    private static final Path COMMITTED_SPEC = Path.of("..", "openapi.json");

    @Autowired
    MockMvc mockMvc;

    @Test
    void emitsAnOpenApiSpecCoveringBothTheApiAndBffChains() throws Exception {
        JsonNode paths = new ObjectMapper().readTree(liveSpec()).get("paths");
        assertThat(paths).as("spec has a paths object").isNotNull();
        assertThat(paths.has("/api/password-reset/request"))
                .as("public /api chain is documented").isTrue();
        assertThat(paths.fieldNames()).toIterable()
                .as("cookie /bff chain is documented").anyMatch(p -> p.startsWith("/bff/"));
    }

    @Test
    void committedSpecMatchesTheLiveControllers() throws Exception {
        String live = normalize(liveSpec());

        if (Boolean.parseBoolean(System.getProperty("openapi.regenerate", "false"))) {
            Files.writeString(COMMITTED_SPEC, live);
        }

        assertThat(Files.exists(COMMITTED_SPEC))
                .as("committed spec %s is missing — regenerate with -Dopenapi.regenerate=true",
                        COMMITTED_SPEC.toAbsolutePath().normalize())
                .isTrue();
        assertThat(normalize(Files.readString(COMMITTED_SPEC)))
                .as("committed spec drifted from the controllers — regenerate with -Dopenapi.regenerate=true")
                .isEqualTo(live);
    }

    // --- helpers -----------------------------------------------------------

    private String liveSpec() throws Exception {
        return mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /**
     * Render the document with recursively sorted keys and stable indentation, so
     * the committed/live comparison is insensitive to map-iteration order and the
     * diff a developer sees on regeneration is a clean semantic one.
     */
    private static String normalize(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        Object tree = mapper.readValue(json, Object.class);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree);
    }
}
