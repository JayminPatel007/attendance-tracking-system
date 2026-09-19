package org.sabha.identity.application;

import java.util.UUID;

import org.sabha.common.CallerAuthority;
import org.sabha.common.web.CurrentUser;
import org.sabha.identity.applicationservice.passwordreset.PasswordReissueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Assigner-reissue BFF endpoint (ADR-0004, ADR-0022): the appointing Karyakar (or
 * an MK member, for Sants) generates a fresh, force-change password for a User who
 * has lost their mobile entirely. An admin act, so it rides the authenticated
 * server-side OIDC session — the caller arrives already resolved to the local
 * User by the edge, which rejects an authenticated subject with no local User
 * (403, ADR-0030). Whether the caller may reissue this target is
 * arbitrated by {@link PasswordReissueService} and mapped (403) by the global
 * exception handler.
 */
@RestController
public class PasswordReissueController {

    private final PasswordReissueService reissues;

    public PasswordReissueController(PasswordReissueService reissues) {
        this.reissues = reissues;
    }

    @PostMapping("/bff/password-reissue")
    public ResponseEntity<Void> reissue(@RequestBody ReissueRequest req, @CurrentUser CallerAuthority caller) {
        reissues.reissue(caller, req.targetUserId(), req.newPassword());
        return ResponseEntity.noContent().build();
    }

    public record ReissueRequest(UUID targetUserId, String newPassword) {
    }
}
