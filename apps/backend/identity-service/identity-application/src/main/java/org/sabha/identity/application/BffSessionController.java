package org.sabha.identity.application;

import java.util.Set;
import java.util.UUID;

import org.sabha.identity.applicationservice.session.WebSessionService;
import org.sabha.identity.domain.Section;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Backend-for-Frontend session endpoint for the Angular web shell (ADR-0022).
 * The request is authenticated by the server-side OIDC session (oauth2Login),
 * not a Bearer token; {@link Authentication#getName()} yields the Keycloak
 * subject (the {@code sub} claim). Returns the shell view-model — username, MK
 * membership, and the visible {@link Section}s.
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
                .map(s -> new WebSessionResponse(s.username(), s.madhyasthaKaryalaya(), s.sections()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(403).build());
    }

    public record WebSessionResponse(String username, boolean madhyasthaKaryalaya, Set<Section> sections) {
    }
}
