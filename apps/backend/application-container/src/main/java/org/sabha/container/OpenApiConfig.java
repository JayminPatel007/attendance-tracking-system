package org.sabha.container;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;

/**
 * OpenAPI document shaping for the generated clients (issue #73). The REST surface
 * is JSON end to end, but endpoints that return {@code ResponseEntity<T>} don't
 * declare {@code produces}, so springdoc documents their response body with the
 * wildcard media type. The generated typed clients read that as a binary payload
 * and fall back to a Blob response type, which then fails to parse the JSON body.
 *
 * <p>Rewriting the wildcard response media type to {@code application/json} once,
 * here, keeps the controllers untouched (no per-endpoint {@code produces}
 * ceremony) and makes every current and future generated client deserialize
 * correctly. This is a deployment-tier wire-format concern, so — like
 * {@code GlobalExceptionHandler} — it lives in application-container. Void
 * responses carry no content and are left alone.
 */
@Configuration
public class OpenApiConfig {

    private static final String WILDCARD = "*/*";
    private static final String JSON = "application/json";

    /**
     * Use-case DTOs that controllers return straight to the wire and whose every
     * field the backend always serializes, each mapped to the subset of those
     * fields that may carry {@code null} (issues #104, #131). See
     * {@link #alwaysPopulatedResponseSchemasCustomizer()}.
     */
    private static final Map<String, Set<String>> ALWAYS_POPULATED_SCHEMAS = Map.ofEntries(
            // Person Directory (ADR-0013) — issue #104.
            Map.entry("WalkInCandidate", Set.of()),
            Map.entry("NameCandidate", Set.of()),

            // Structural admin (ADR-0009, ADR-0026); retiredAt is the soft-retire marker.
            Map.entry("CityView", Set.of()),
            Map.entry("ZoneView", Set.of()),
            Map.entry("KshetraView", Set.of()),
            Map.entry("SabhaView", Set.of()),
            Map.entry("SabhaKindView", Set.of("retiredAt")),

            // Occurrence reopen (Slice 13) and Sanchalak proxy (Slice 14); the
            // reopen reason is derived from the audit log, and a Sabha may sit
            // with its Sanchalak unassigned or never seen.
            Map.entry("ReopenListItem", Set.of("lastReopenReason")),
            Map.entry("ProxyOccurrenceItem", Set.of()),
            Map.entry("ProxySabhaListItem", Set.of("sanchalakUserId", "sanchalakName", "lastSeenAt")),

            // Audit feed (Slice 19, ADR-0023): a system act has no actor, an
            // unresolved actor no name, and only a proxy action an on-behalf pair.
            Map.entry("AuditEntry",
                    Set.of("actorUserId", "actorName", "onBehalfOfUserId", "onBehalfName", "detail")),

            // Re-engagement dashboard (Slices 15/17, ADR-0010); the Kshetras with
            // no Zone surface under a Zone node with a null id, and only a Sant
            // has a City pick.
            Map.entry("DashboardOverview", Set.of()),
            Map.entry("Kpis", Set.of()),
            Map.entry("CandidateRow", Set.of()),
            Map.entry("Thresholds", Set.of()),
            Map.entry("SabhaTree", Set.of()),
            Map.entry("Zone", Set.of("zoneId")),
            Map.entry("Kshetra", Set.of()),
            Map.entry("Sabha", Set.of()),
            Map.entry("CityChip", Set.of("selectedCityId")),
            Map.entry("CityOption", Set.of()),

            // Lost-mobile appointer lookup (Slice 18).
            Map.entry("AppointerContact", Set.of()));

    /** The OpenAPI 3.1 spelling of "this property may also be null". */
    private static final String NULL_TYPE = "null";

    @Bean
    OpenApiCustomizer jsonResponseMediaTypeCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                if (operation.getResponses() == null) {
                    return;
                }
                operation.getResponses().values().forEach(response -> {
                    Content content = response.getContent();
                    if (content != null && content.containsKey(WILDCARD)) {
                        MediaType wildcard = content.remove(WILDCARD);
                        content.addMediaType(JSON, wildcard);
                    }
                });
            }));
        };
    }

    /**
     * Marks every property of the listed response schemas required, and the
     * declared subset of them nullable (issues #104, #131). A response field left
     * optional in the document generates a possibly-undefined field in the typed
     * clients, which each caller then has to assert or default away at its API
     * seam — the asserts the mobile view models used to carry, and the reason
     * every web section hand-wrote a parallel type mirror instead of consuming
     * the generated one.
     *
     * <p>Required and nullable are orthogonal, and keeping them so is what makes
     * the generated model as strong as the mirror it replaces. A property that is
     * always serialized but sometimes carries {@code null} — an unresolved actor
     * name, a not-yet-retired Sabha Kind — is declared both, which the generator
     * renders {@code T | null}. Declaring it merely optional would lose the "the
     * server always tells you" half; declaring it required alone would lie.
     *
     * <p>Siblings of these records on the same endpoints declare the same thing
     * inline with {@code @Schema(requiredMode = REQUIRED)}. These cannot: they are
     * use-case DTOs in a {@code *-application-service} (or {@code *-domain-core})
     * module, whose dependencies ADR-0019 caps at {@code spring-context} +
     * {@code spring-tx} — nothing else, annotation-only jars included. So the
     * wire-shape declaration for them lands here, in the ring that already owns
     * document shaping.
     */
    @Bean
    OpenApiCustomizer alwaysPopulatedResponseSchemasCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            ALWAYS_POPULATED_SCHEMAS.forEach((name, nullableProperties) -> {
                Schema<?> schema = openApi.getComponents().getSchemas().get(name);
                if (schema == null || schema.getProperties() == null) {
                    return;
                }
                schema.setRequired(List.copyOf(schema.getProperties().keySet()));
                nullableProperties.forEach(property -> admitNull(schema.getProperties().get(property)));
            });
        };
    }

    /**
     * OpenAPI 3.1 — the version springdoc renders here — spells nullability as a
     * {@code "null"} member of the property's type array, not the 3.0
     * {@code nullable} keyword, which the client generators ignore. The 3.1
     * document carries the type in swagger-core's {@code types} set (serialized
     * as {@code type}, scalar while it holds one member); the singular
     * {@code type} field is the 3.0 spelling, so fall back to it only if the set
     * is empty.
     */
    private static void admitNull(Schema<?> property) {
        if (property == null) {
            return;
        }
        if (property.getTypes() == null || property.getTypes().isEmpty()) {
            if (property.getType() == null) {
                return;
            }
            property.setTypes(new LinkedHashSet<>(Set.of(property.getType())));
        }
        property.getTypes().add(NULL_TYPE);
    }
}
