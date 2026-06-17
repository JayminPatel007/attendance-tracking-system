package org.sabha.identity.application;

import java.util.List;
import java.util.UUID;

import org.sabha.common.CallerResolver;
import org.sabha.identity.applicationservice.selection.PendingNominationItem;
import org.sabha.identity.applicationservice.selection.SelectedPersonItem;
import org.sabha.identity.applicationservice.selection.SelectionQueries;
import org.sabha.identity.applicationservice.selection.SelectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demographic Nirdeshak's selection BFF surface for the Angular web shell
 * (ADR-0006, ADR-0022). Cookie/session authenticated, so the caller is the
 * Keycloak subject in {@link Authentication#getName()}; an authenticated subject
 * with no local User is unauthorized (403).
 *
 * <ul>
 *   <li>{@code GET /bff/selection/nominations} — the pending queue scoped to the
 *       caller's NIRDESHAK (Kshetra, demographic) rows.</li>
 *   <li>{@code POST /bff/selection/nominations/{id}/approve} — approve; adds the
 *       selective Home Sabha.</li>
 *   <li>{@code POST /bff/selection/nominations/{id}/reject} — reject with an
 *       optional reason.</li>
 *   <li>{@code POST /bff/selection/deselect} — deselect a previously approved
 *       Person, removing only the selective Home Sabha.</li>
 * </ul>
 *
 * <p>Decision authority (the track-shared demographic Nirdeshak) is enforced in
 * {@link SelectionService}; out-of-scope or unknown callers surface as 403 via the
 * global exception handler.</p>
 */
@RestController
public class SelectionBffController {

    private final SelectionService selection;
    private final SelectionQueries queries;
    private final CallerResolver callers;

    public SelectionBffController(
            SelectionService selection, SelectionQueries queries, CallerResolver callers) {
        this.selection = selection;
        this.queries = queries;
        this.callers = callers;
    }

    @GetMapping("/bff/selection/nominations")
    public ResponseEntity<List<PendingNominationItem>> queue(Authentication authentication) {
        UUID subject = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(queries.pendingQueueFor(callers.requireUserId(subject)));
    }

    @GetMapping("/bff/selection/selected")
    public ResponseEntity<List<SelectedPersonItem>> selected(Authentication authentication) {
        UUID subject = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(queries.selectedFor(callers.requireUserId(subject)));
    }

    @PostMapping("/bff/selection/nominations/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable UUID id, Authentication authentication) {
        UUID subject = UUID.fromString(authentication.getName());
        selection.approve(subject, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bff/selection/nominations/{id}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable UUID id,
            @RequestBody RejectRequest req,
            Authentication authentication) {
        UUID subject = UUID.fromString(authentication.getName());
        selection.reject(subject, id, req.reason());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bff/selection/deselect")
    public ResponseEntity<Void> deselect(
            @RequestBody DeselectRequest req, Authentication authentication) {
        UUID subject = UUID.fromString(authentication.getName());
        selection.deselect(subject, req.personId(), req.selectiveSabhaId());
        return ResponseEntity.noContent().build();
    }

    public record RejectRequest(String reason) {
    }

    public record DeselectRequest(UUID personId, UUID selectiveSabhaId) {
    }
}
