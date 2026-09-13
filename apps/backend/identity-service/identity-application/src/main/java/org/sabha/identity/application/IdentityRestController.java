package org.sabha.identity.application;

import java.util.UUID;

import org.sabha.common.UserId;
import org.sabha.common.web.CurrentUser;
import org.sabha.identity.applicationservice.UserRepository;
import org.sabha.identity.domain.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IdentityRestController {

    private final UserRepository users;

    public IdentityRestController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/api/whoami")
    public ResponseEntity<WhoAmIResponse> whoami(@CurrentUser UserId caller) {
        return users.findById(caller.value())
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
