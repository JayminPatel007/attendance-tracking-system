package org.sabha.identity.application;

import java.util.UUID;

import org.sabha.identity.applicationservice.HomeSabhaTransferService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verified Home Sabha Transfer endpoints (ADR-0002):
 *
 * <ul>
 *   <li>{@code POST /api/home-sabha-transfers} — a Sanchalak/Sah-Sanchalak of the
 *       destination Sabha initiates a transfer for a Directory Person; an OTP is
 *       sent to the Person's mobile and the new transfer id is returned.</li>
 *   <li>{@code POST /api/home-sabha-transfers/{id}/confirm} — the Person's OTP is
 *       submitted; on success the Roster swap commits.</li>
 * </ul>
 *
 * <p>Authority, OTP TTL / wrong-code / lockout, rate limit, and cooldown all
 * surface as exceptions mapped to HTTP status by {@code GlobalExceptionHandler}.
 */
@RestController
public class HomeSabhaTransferRestController {

    private final HomeSabhaTransferService transfers;

    public HomeSabhaTransferRestController(HomeSabhaTransferService transfers) {
        this.transfers = transfers;
    }

    @PostMapping("/api/home-sabha-transfers")
    public InitiateTransferResponse initiate(
            @RequestBody InitiateTransferRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        UUID subject = UUID.fromString(jwt.getSubject());
        UUID transferId = transfers.initiate(subject, req.personId(), req.destinationSabhaId());
        return new InitiateTransferResponse(transferId);
    }

    @PostMapping("/api/home-sabha-transfers/{id}/confirm")
    public void confirm(@PathVariable UUID id, @RequestBody ConfirmTransferRequest req) {
        transfers.confirm(id, req.otpCode());
    }

    public record InitiateTransferRequest(UUID personId, UUID destinationSabhaId) {
    }

    public record InitiateTransferResponse(UUID transferId) {
    }

    public record ConfirmTransferRequest(String otpCode) {
    }
}
