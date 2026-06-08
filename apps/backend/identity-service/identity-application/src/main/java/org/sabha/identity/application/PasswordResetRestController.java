package org.sabha.identity.application;

import java.util.UUID;

import org.sabha.identity.applicationservice.PasswordResetService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-service password-reset endpoints (ADR-0004). Unauthenticated by design —
 * a locked-out User reaches these from the login screen of both the mobile and
 * web apps, so they sit on the public API chain (see {@code SecurityConfig}). The
 * two-step flow keeps the OTP off the final password-set call:
 *
 * <ul>
 *   <li>{@code POST /api/password-reset/request} — sends an OTP to the User's
 *       registered mobile and returns the reset id.</li>
 *   <li>{@code POST /api/password-reset/verify} — exchanges a correct OTP for a
 *       short-lived reset token.</li>
 *   <li>{@code POST /api/password-reset/complete} — sets the new password against
 *       the reset token.</li>
 * </ul>
 *
 * <p>Unknown username (404), no registered mobile (422), wrong/expired/over-limit
 * OTP, and expired/replayed tokens all surface as exceptions mapped by the global
 * exception handler.</p>
 */
@RestController
public class PasswordResetRestController {

    private final PasswordResetService resets;

    public PasswordResetRestController(PasswordResetService resets) {
        this.resets = resets;
    }

    @PostMapping("/api/password-reset/request")
    public RequestResponse request(@RequestBody RequestPayload req) {
        return new RequestResponse(resets.request(req.username()));
    }

    @PostMapping("/api/password-reset/verify")
    public VerifyResponse verify(@RequestBody VerifyPayload req) {
        return new VerifyResponse(resets.verify(req.resetId(), req.otpCode()));
    }

    @PostMapping("/api/password-reset/complete")
    public void complete(@RequestBody CompletePayload req) {
        resets.complete(req.resetToken(), req.newPassword());
    }

    public record RequestPayload(String username) {
    }

    public record RequestResponse(UUID resetId) {
    }

    public record VerifyPayload(UUID resetId, String otpCode) {
    }

    public record VerifyResponse(String resetToken) {
    }

    public record CompletePayload(String resetToken, String newPassword) {
    }
}
