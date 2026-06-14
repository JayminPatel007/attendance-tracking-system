package org.sabha.container;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;

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
}
