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
 * Issue #131: quality gate on the slice of the contract the <em>web</em> client
 * consumes, the sibling of {@link DirectoryContractTest} for the mobile
 * Directory surface. Same premise: a response field springdoc leaves optional
 * generates a possibly-undefined model field, so a screen reading it has to
 * assert or default it away at the API seam. Web sections avoided that by
 * hand-writing a parallel type mirror per section — three type systems for one
 * contract. Saying which fields the backend always populates, here, is what lets
 * those mirrors be deleted rather than merely moved.
 *
 * <p>Required and nullable are orthogonal, and the distinction is what makes the
 * generated model as strong as the mirror it replaces: a property that is
 * <em>always serialized</em> but sometimes carries {@code null} (an unresolved
 * actor name, a not-yet-retired Sabha Kind) is declared required <em>and</em>
 * nullable, which the generator renders {@code field: T | null} — the mirror's
 * exact type. Declaring it merely optional would render {@code field?: T} and
 * lose the "the server always tells you" half of the contract; declaring it
 * required and non-nullable would lie.
 *
 * <p>Assertions read the committed {@code apps/backend/openapi.json} — the exact
 * document the web generator consumes — and {@link OpenApiContractIntegrationTest}
 * separately pins that file to the live controllers, so no Spring context is
 * needed here. The subject is the artifact.
 */
class WebContractTest {

    /** Same relative path as the drift gate: surefire runs in the module directory. */
    private static final Path COMMITTED_SPEC = Path.of("..", "openapi.json");

    private static JsonNode spec;

    @BeforeAll
    static void readSpec() throws Exception {
        spec = new ObjectMapper().readTree(Files.readString(COMMITTED_SPEC));
    }

    // --- structural admin (ADR-0009, ADR-0026) -----------------------------

    /**
     * The child counts drive the block-if-non-empty delete affordance, so an
     * absent count would read as a deletable entity.
     */
    @Test
    void structuralViewsAlwaysCarryTheirIdentityAndChildCount() {
        assertThat(requiredOf("CityView")).containsExactlyInAnyOrder("id", "name", "zoneCount");
        assertThat(requiredOf("ZoneView"))
                .containsExactlyInAnyOrder("id", "name", "cityId", "cityName", "kshetraCount");
        assertThat(requiredOf("KshetraView"))
                .containsExactlyInAnyOrder("id", "name", "zoneId", "sabhaCount");
        assertThat(requiredOf("SabhaView")).containsExactlyInAnyOrder(
                "id", "kshetraId", "kshetraName", "demographic", "track", "standingVenue", "occurrenceCount");
    }

    /** {@code retiredAt} is the soft-retire marker: null for an active kind (ADR-0026). */
    @Test
    void aSabhaKindAlwaysReportsWhetherItIsRetired() {
        assertThat(requiredOf("SabhaKindView"))
                .containsExactlyInAnyOrder("id", "demographic", "track", "retiredAt");
        assertThat(nullablePropertiesOf("SabhaKindView")).containsExactly("retiredAt");
    }

    @Test
    void aCreatedEntityAlwaysCarriesItsId() {
        assertThat(requiredOf("CreatedResponse")).containsExactly("id");
    }

    // --- role appointment (ADR-0011, ADR-0025) -----------------------------

    /**
     * The ids are null on the soft-warn outcome, where nothing was created; the
     * always-present pair is the outcome discriminator and the candidate list —
     * the same shape {@code AddPersonResponse} carries on the mobile side.
     */
    @Test
    void anAppointmentResponseAlwaysCarriesItsOutcome() {
        assertThat(requiredOf("AppointmentResponse")).containsExactlyInAnyOrder(
                "personId", "userId", "assignmentId", "candidates", "requiresOverride");
        assertThat(nullablePropertiesOf("AppointmentResponse"))
                .containsExactlyInAnyOrder("personId", "userId", "assignmentId");
    }

    @Test
    void theSahNirdeshakCapAlwaysReportsBothSidesOfTheRatio() {
        assertThat(requiredOf("SahNirdeshakCapResponse"))
                .containsExactlyInAnyOrder("active", "cap", "reached");
    }

    // --- Sabha definition (ADR-0012) --------------------------------------

    @Test
    void aSabhaDefinitionResponseAlwaysCarriesItsOutcome() {
        assertThat(requiredOf("SabhaDefinitionResponse")).containsExactlyInAnyOrder(
                "sabhaId", "sanchalakAssignmentId", "sahSanchalakAssignmentId",
                "candidates", "requiresOverride");
        assertThat(nullablePropertiesOf("SabhaDefinitionResponse")).containsExactlyInAnyOrder(
                "sabhaId", "sanchalakAssignmentId", "sahSanchalakAssignmentId");
    }

    // --- occurrence reopen (Slice 13) / Sanchalak proxy (Slice 14) ---------

    /** {@code lastReopenReason} is derived from the audit log; null until reopened. */
    @Test
    void aReopenableOccurrenceAlwaysCarriesItsReopenHistory() {
        assertThat(requiredOf("ReopenListItem")).containsExactlyInAnyOrder(
                "occurrenceId", "date", "state", "kshetraName", "sabhaKind", "venue",
                "reopened", "lastReopenReason");
        assertThat(nullablePropertiesOf("ReopenListItem")).containsExactly("lastReopenReason");
    }

    /** A Sabha may sit with its Sanchalak unassigned, or never seen (Slice 14). */
    @Test
    void aProxiableSabhaAlwaysReportsItsSanchalakEvenWhenThereIsNone() {
        assertThat(requiredOf("ProxySabhaListItem")).containsExactlyInAnyOrder(
                "sabhaId", "sabhaLabel", "sanchalakUserId", "sanchalakName", "lastSeenAt");
        assertThat(nullablePropertiesOf("ProxySabhaListItem"))
                .containsExactlyInAnyOrder("sanchalakUserId", "sanchalakName", "lastSeenAt");
    }

    @Test
    void aProxiedOccurrenceAlwaysCarriesItsEffectiveDateAndState() {
        assertThat(requiredOf("ProxyOccurrenceItem"))
                .containsExactlyInAnyOrder("id", "effectiveDate", "state", "venue");
    }

    // --- audit log (Slice 19, ADR-0023) -----------------------------------

    /**
     * A system act has no actor, an unresolved actor no name, and only a
     * Nirikshak proxy action carries an on-behalf-of pair — all serialized, all
     * sometimes null.
     */
    @Test
    void anAuditEntryAlwaysCarriesEveryProvenanceFieldItHas() {
        assertThat(requiredOf("AuditEntry")).containsExactlyInAnyOrder(
                "id", "at", "actorUserId", "actorName", "onBehalfOfUserId", "onBehalfName",
                "targetType", "targetId", "action", "detail");
        assertThat(nullablePropertiesOf("AuditEntry")).containsExactlyInAnyOrder(
                "actorUserId", "actorName", "onBehalfOfUserId", "onBehalfName", "detail");
    }

    // --- re-engagement dashboard (Slice 15/17, ADR-0010) ------------------

    @Test
    void theDashboardReadsAlwaysCarryTheirFigures() {
        assertThat(requiredOf("DashboardOverview")).containsExactlyInAnyOrder("kpis", "headlineCandidates");
        assertThat(requiredOf("Kpis"))
                .containsExactlyInAnyOrder("totalCandidates", "priorityCandidates", "sabhasWithCandidates");
        assertThat(requiredOf("CandidateRow")).containsExactlyInAnyOrder(
                "personId", "personName", "homeSabhaId", "sabhaKind", "kshetraName",
                "demographic", "missedStreak", "tier");
        assertThat(requiredOf("Thresholds")).containsExactlyInAnyOrder("candidate", "priority");
    }

    /**
     * The tier is a closed two-value set (ADR-0010), and the dashboard renders a
     * different badge per value. Left as a bare string on the wire, the web had to
     * restate the pair to type the badge input — the last mirror in this section.
     */
    @Test
    void aCandidateNamesItsTierAsAClosedSet() {
        assertThat(enumOf("CandidateRow", "tier")).containsExactly("CANDIDATE", "PRIORITY");
    }

    /** The Kshetras with no Zone surface under a Zone node with a null id (ADR-0010). */
    @Test
    void theSabhaTreeAlwaysCarriesCountsAtEveryLevel() {
        assertThat(requiredOf("SabhaTree")).containsExactly("zones");
        assertThat(requiredOf("Zone"))
                .containsExactlyInAnyOrder("zoneId", "zoneName", "candidateCount", "kshetras");
        assertThat(nullablePropertiesOf("Zone")).containsExactly("zoneId");
        assertThat(requiredOf("Kshetra"))
                .containsExactlyInAnyOrder("kshetraId", "kshetraName", "candidateCount", "sabhas");
        assertThat(requiredOf("Sabha"))
                .containsExactlyInAnyOrder("sabhaId", "sabhaKind", "candidateCount");
    }

    /** A non-Sant gets {@code sant: false}, an empty list and no pick (Slice 17). */
    @Test
    void theCityChipAlwaysReportsWhetherThePickerIsInteractive() {
        assertThat(requiredOf("CityChip")).containsExactlyInAnyOrder("sant", "selectedCityId", "cities");
        assertThat(nullablePropertiesOf("CityChip")).containsExactly("selectedCityId");
        assertThat(requiredOf("CityOption")).containsExactlyInAnyOrder("id", "name");
    }

    // --- session (Slice 9, ADR-0022) / password reset (Slice 18) ----------

    @Test
    void theWebSessionAlwaysCarriesTheSectionsItUnlocks() {
        assertThat(requiredOf("WebSessionResponse"))
                .containsExactlyInAnyOrder("username", "madhyasthaKaryalaya", "regionalTeam", "sections");
    }

    @Test
    void theTwoStepPasswordResetAlwaysCarriesTheHandleForItsNextStep() {
        assertThat(requiredOf("RequestResponse")).containsExactly("resetId");
        assertThat(requiredOf("VerifyResponse")).containsExactly("resetToken");
        assertThat(requiredOf("WhoAppointedMeResponse")).containsExactly("contacts");
        assertThat(requiredOf("AppointerContact")).containsExactlyInAnyOrder("name", "mobile");
    }

    // --- audit feed operation naming --------------------------------------

    /**
     * Two controllers offered an operation called {@code list}, so the generators
     * kept one and renamed the other by appending an ordinal — and which one got
     * renamed depends on nothing the contract states. The audit viewer is the one
     * caller that names a generated operation in hand-written code, so a third
     * {@code list} appearing anywhere could silently re-point it at another
     * endpoint while still compiling. Naming the operation removes the ordinal.
     */
    @Test
    void theAuditFeedOperationIsNamedRatherThanNumbered() {
        assertThat(spec.at("/paths/~1bff~1audit-log/get/operationId").asText())
                .isEqualTo("listAuditEntries");
    }

    // --- the web Directory search (ADR-0013) ------------------------------

    /**
     * The one {@code /bff/directory/search} handler returned {@code Object} —
     * a Person for a mobile hit, a candidate list for a name hit — which springdoc
     * documents as a bare object and the generator types {@code Observable<object>}.
     * Splitting the name lookup onto its own path is what #104 did for the
     * {@code /api} twin, and gives each response a schema.
     */
    @Test
    void theWebDirectoryLookupsAreEachTypedRatherThanOneWildcardObject() {
        JsonNode byMobile = spec.at("/paths/~1bff~1directory~1search/get");
        assertThat(byMobile.at("/responses/200/content/application~1json/schema/$ref").asText())
                .isEqualTo("#/components/schemas/PersonResponse");
        assertThat(byMobile.at("/responses/404").isMissingNode())
                .as("the not-found outcome the picker treats as \"this number is new\" is documented")
                .isFalse();

        JsonNode byName = spec.at("/paths/~1bff~1directory~1name-search/get");
        assertThat(byName.at("/responses/200/content/application~1json/schema/items/$ref").asText())
                .isEqualTo("#/components/schemas/NameCandidate");
    }

    // --- helpers -----------------------------------------------------------

    private static List<String> requiredOf(String schema) {
        JsonNode required = propertiesHolder(schema).get("required");
        assertThat(required != null && required.isArray())
                .as("schema %s declares required properties", schema)
                .isTrue();
        List<String> names = new ArrayList<>();
        required.forEach(name -> names.add(name.asText()));
        return names;
    }

    /**
     * The properties this schema declares nullable. In OpenAPI 3.1 — the version
     * springdoc renders — that is a {@code "null"} member in the property's
     * {@code type} array, not the 3.0 {@code nullable: true} keyword, which the
     * generator ignores.
     */
    private static List<String> nullablePropertiesOf(String schema) {
        JsonNode properties = propertiesHolder(schema).get("properties");
        List<String> nullable = new ArrayList<>();
        properties.properties().forEach(property -> {
            JsonNode type = property.getValue().get("type");
            boolean declaresNull = type != null && type.isArray()
                    && anyMatches(type, "null");
            if (declaresNull) {
                nullable.add(property.getKey());
            }
        });
        return nullable;
    }

    /** The values a property is documented to be closed over. */
    private static List<String> enumOf(String schema, String property) {
        JsonNode values = propertiesHolder(schema).at("/properties/" + property + "/enum");
        assertThat(values.isArray())
                .as("property %s.%s declares its permitted values", schema, property)
                .isTrue();
        List<String> names = new ArrayList<>();
        values.forEach(value -> names.add(value.asText()));
        return names;
    }

    private static boolean anyMatches(JsonNode array, String value) {
        for (JsonNode element : array) {
            if (value.equals(element.asText())) {
                return true;
            }
        }
        return false;
    }

    private static JsonNode propertiesHolder(String schema) {
        JsonNode node = spec.at("/components/schemas/" + schema);
        assertThat(node.isMissingNode()).as("schema %s is in the document", schema).isFalse();
        return node;
    }
}
