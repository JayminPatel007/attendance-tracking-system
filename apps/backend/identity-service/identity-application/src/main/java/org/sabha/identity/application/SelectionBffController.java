package org.sabha.identity.application;

import java.util.List;
import java.util.UUID;

import org.sabha.common.CallerAuthority;
import org.sabha.common.web.CurrentUser;
import org.sabha.identity.applicationservice.selection.PendingNominationItem;
import org.sabha.identity.applicationservice.selection.SelectedPersonItem;
import org.sabha.identity.applicationservice.selection.SelectionQueries;
import org.sabha.identity.applicationservice.selection.SelectionService;
import org.springframework.http.ResponseEntity;
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

    public SelectionBffController(SelectionService selection, SelectionQueries queries) {
        this.selection = selection;
        this.queries = queries;
    }

    @GetMapping("/bff/selection/nominations")
    public ResponseEntity<List<PendingNominationItem>> queue(@CurrentUser CallerAuthority caller) {
        return ResponseEntity.ok(queries.pendingQueueFor(caller.userId().value()));
    }

    @GetMapping("/bff/selection/selected")
    public ResponseEntity<List<SelectedPersonItem>> selected(@CurrentUser CallerAuthority caller) {
        return ResponseEntity.ok(queries.selectedFor(caller.userId().value()));
    }

    @PostMapping("/bff/selection/nominations/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable UUID id, @CurrentUser CallerAuthority caller) {
        selection.approve(caller, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bff/selection/nominations/{id}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable UUID id,
            @RequestBody RejectRequest req,
            @CurrentUser CallerAuthority caller) {
        selection.reject(caller, id, req.reason());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bff/selection/deselect")
    public ResponseEntity<Void> deselect(
            @RequestBody DeselectRequest req, @CurrentUser CallerAuthority caller) {
        selection.deselect(caller, req.personId(), req.selectiveSabhaId());
        return ResponseEntity.noContent().build();
    }

    public record RejectRequest(String reason) {
    }

    public record DeselectRequest(UUID personId, UUID selectiveSabhaId) {
    }
}
