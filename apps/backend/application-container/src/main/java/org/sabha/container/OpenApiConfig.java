package org.sabha.container;

import java.util.List;
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
     * field the backend always populates (issue #104). See
     * {@link #alwaysPopulatedResponseSchemasCustomizer()}.
     */
    private static final Set<String> ALWAYS_POPULATED_SCHEMAS = Set.of("WalkInCandidate", "NameCandidate");

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
     * Marks every property of the Directory candidate schemas required (issue
     * #104). A response field left optional in the document generates a nullable
     * field in the typed clients, which each caller then has to assert away at
     * its API seam — the asserts the mobile view models used to carry. These two
     * records are always fully populated: a candidate the Directory returns has
     * an id, a name, and a (possibly empty) list of Home Sabha kinds.
     *
     * <p>Their siblings on the same endpoints declare this inline with
     * {@code @Schema(requiredMode = REQUIRED)}. These two cannot: they are
     * use-case DTOs in {@code identity-application-service}, whose dependencies
     * ADR-0019 caps at {@code spring-context} + {@code spring-tx} — nothing else,
     * annotation-only jars included. So the wire-shape declaration for them lands
     * here, in the ring that already owns document shaping.
     */
    @Bean
    OpenApiCustomizer alwaysPopulatedResponseSchemasCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            ALWAYS_POPULATED_SCHEMAS.forEach(name -> {
                Schema<?> schema = openApi.getComponents().getSchemas().get(name);
                if (schema != null && schema.getProperties() != null) {
                    schema.setRequired(List.copyOf(schema.getProperties().keySet()));
                }
            });
        };
    }
}
