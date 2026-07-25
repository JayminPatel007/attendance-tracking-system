package org.sabha.container;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Issue #104: quality gate on the Person-Directory slice of the contract. The
 * generated clients can only be as good as the document they are generated from
 * — a response field springdoc leaves optional becomes a nullable model field
 * the caller has to assert away at the seam, and a handler typed
 * {@code ResponseEntity<Object>} becomes an untyped payload the caller has to
 * parse by hand. Both drove the mobile Directory code to stay hand-rolled
 * (issue #75's carve-outs).
 *
 * <p>These assertions read the committed {@code apps/backend/openapi.json} — the
 * exact document the web and mobile generators consume. {@link
 * OpenApiContractIntegrationTest} separately pins that file to the live
 * controllers, so an annotation removed from a record cannot pass this gate by
 * going un-regenerated. No Spring context is needed here: the subject is the
 * artifact.
 */
class DirectoryContractTest {

    /** Same relative path as the drift gate: surefire runs in the module directory. */
    private static final Path COMMITTED_SPEC = Path.of("..", "openapi.json");

    private static JsonNode spec;

    @BeforeAll
    static void readSpec() throws Exception {
        spec = new ObjectMapper().readTree(Files.readString(COMMITTED_SPEC));
    }

    @Test
    void walkInCandidateAlwaysCarriesAnIdentifiablePerson() {
        assertThat(requiredOf("WalkInCandidate"))
                .containsExactlyInAnyOrder("personId", "fullName", "homeSabhas");
    }

    @Test
    void nameCandidateAlwaysCarriesAnIdentifiablePerson() {
        assertThat(requiredOf("NameCandidate"))
                .containsExactlyInAnyOrder("personId", "fullName", "homeSabhas");
    }

    /**
     * {@code personId} is deliberately absent: it is null on the soft-warn
     * outcome, where nothing was created. The always-present pair is the
     * outcome discriminator and the candidate list.
     */
    @Test
    void addPersonResponseAlwaysCarriesItsOutcome() {
        assertThat(requiredOf("AddPersonResponse"))
                .containsExactlyInAnyOrder("candidates", "requiresOverride");
    }

    /** DOB, mobile and guardian are genuinely optional on a Person (CONTEXT.md). */
    @Test
    void personResponseAlwaysCarriesIdentityAndGender() {
        assertThat(requiredOf("PersonResponse"))
                .containsExactlyInAnyOrder("id", "fullName", "gender");
    }

    @Test
    void theMobileLookupIsTypedAsAPersonNotAWildcardObject() {
        JsonNode lookup = spec.at("/paths/~1api~1directory~1persons/get");

        assertThat(lookup.at("/responses/200/content/application~1json/schema/$ref").asText())
                .isEqualTo("#/components/schemas/PersonResponse");
        assertThat(lookup.at("/responses/404").isMissingNode())
                .as("the not-found outcome the add-person flow treats as \"new number\" is documented")
                .isFalse();
    }

    private static List<String> requiredOf(String schema) {
        JsonNode required = spec.at("/components/schemas/" + schema + "/required");
        assertThat(required.isArray())
                .as("schema %s declares required properties", schema)
                .isTrue();
        List<String> names = new ArrayList<>();
        required.forEach(name -> names.add(name.asText()));
        return names;
    }
}
