package org.sabha.identity.application;

import java.util.UUID;

import org.sabha.identity.domain.User;
import org.sabha.identity.domain.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IdentityRestController {

    private final UserRepository users;

    public IdentityRestController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/api/whoami")
    public ResponseEntity<WhoAmIResponse> whoami(@AuthenticationPrincipal Jwt jwt) {
        UUID keycloakUserId = UUID.fromString(jwt.getSubject());
        return users.findByKeycloakUserId(keycloakUserId)
                .map(IdentityRestController::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(403).build());
    }

    private static WhoAmIResponse toResponse(User user) {
        return new WhoAmIResponse(user.id(), user.personId(), user.username());
    }

    public record WhoAmIResponse(UUID userId, UUID personId, String username) {
    }
}
