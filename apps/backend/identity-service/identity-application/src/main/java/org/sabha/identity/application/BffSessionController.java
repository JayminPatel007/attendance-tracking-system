package org.sabha.identity.application;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.sabha.identity.applicationservice.session.WebSessionService;
import org.sabha.identity.domain.Section;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Backend-for-Frontend session endpoint for the Angular web shell (ADR-0022).
 * The request is authenticated by the server-side OIDC session (oauth2Login),
 * not a Bearer token; {@link Authentication#getName()} yields the Keycloak
 * subject (the {@code sub} claim). Returns the shell view-model — username, MK
 * and Regional Team membership, and the visible {@link Section}s.
 */
@RestController
public class BffSessionController {

    private final WebSessionService sessions;

    public BffSessionController(WebSessionService sessions) {
        this.sessions = sessions;
    }

    @GetMapping("/bff/me")
    public ResponseEntity<WebSessionResponse> me(Authentication authentication) {
        UUID keycloakSubject = UUID.fromString(authentication.getName());
        return sessions.describe(keycloakSubject)
                .map(s -> new WebSessionResponse(
                        s.username(), s.madhyasthaKaryalaya(), s.regionalTeam(), inDeclarationOrder(s.sections())))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(403).build());
    }

    /**
     * The granted sections as the stable list the wire actually carries. The
     * authority engine deals in a {@link Set} — a section is granted or not — but
     * JSON has no set, and a {@code Set} in the document generates a client-side
     * {@code Set} that the shell (which asks {@code includes}) cannot read. The
     * shell orders the nav itself, so the order here only has to be deterministic.
     */
    private static List<Section> inDeclarationOrder(Set<Section> sections) {
        return Arrays.stream(Section.values()).filter(sections::contains).toList();
    }

    public record WebSessionResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String username,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean madhyasthaKaryalaya,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean regionalTeam,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Section> sections) {
    }
}
