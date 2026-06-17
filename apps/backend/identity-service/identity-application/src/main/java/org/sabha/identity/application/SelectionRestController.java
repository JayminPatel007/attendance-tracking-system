package org.sabha.identity.application;

import java.util.UUID;

import org.sabha.identity.applicationservice.selection.SelectionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mobile-facing BSS/YSS nomination endpoint (ADR-0006). JWT authenticated: the
 * Regular Sanchalak nominates a Person from their Roster for the selective track.
 *
 * <ul>
 *   <li>{@code POST /api/sanchalak/nominations} — nominate; the selective Sabha is
 *       derived from the Person's Regular Sabha and the new nomination id is
 *       returned.</li>
 * </ul>
 *
 * <p>Authority, roster membership, missing selective Sabha, and duplicate
 * nominations surface as exceptions mapped to HTTP status by
 * {@code GlobalExceptionHandler}.</p>
 */
@RestController
public class SelectionRestController {

    private final SelectionService selection;

    public SelectionRestController(SelectionService selection) {
        this.selection = selection;
    }

    @PostMapping("/api/sanchalak/nominations")
    public NominateResponse nominate(
            @RequestBody NominateRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        UUID subject = UUID.fromString(jwt.getSubject());
        UUID nominationId = selection.nominate(subject, req.personId(), req.regularSabhaId());
        return new NominateResponse(nominationId);
    }

    public record NominateRequest(UUID personId, UUID regularSabhaId) {
    }

    public record NominateResponse(UUID nominationId) {
    }
}
