package org.sabha.identity.application;

import java.util.UUID;

import org.sabha.common.CallerResolver;
import org.sabha.identity.applicationservice.PasswordReissueService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Assigner-reissue BFF endpoint (ADR-0004, ADR-0022): the appointing Karyakar (or
 * an MK member, for Sants) generates a fresh, force-change password for a User who
 * has lost their mobile entirely. An admin act, so it rides the authenticated
 * server-side OIDC session — the caller is the Keycloak subject in
 * {@link Authentication#getName()}, and an authenticated subject with no local
 * User is unauthorized (403). Whether the caller may reissue this target is
 * arbitrated by {@link PasswordReissueService} and mapped (403) by the global
 * exception handler.
 */
@RestController
public class PasswordReissueController {

    private final PasswordReissueService reissues;
    private final CallerResolver callers;

    public PasswordReissueController(PasswordReissueService reissues, CallerResolver callers) {
        this.reissues = reissues;
        this.callers = callers;
    }

    @PostMapping("/bff/password-reissue")
    public ResponseEntity<Void> reissue(@RequestBody ReissueRequest req, Authentication authentication) {
        UUID subject = UUID.fromString(authentication.getName());
        if (callers.resolveUserId(subject).isEmpty()) {
            return ResponseEntity.status(403).build();
        }

        reissues.reissue(subject, req.targetUserId(), req.newPassword());
        return ResponseEntity.noContent().build();
    }

    public record ReissueRequest(UUID targetUserId, String newPassword) {
    }
}
