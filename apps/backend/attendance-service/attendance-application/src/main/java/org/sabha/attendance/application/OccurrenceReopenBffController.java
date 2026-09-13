package org.sabha.attendance.application;

import java.util.List;
import java.util.UUID;

import org.sabha.attendance.applicationservice.OccurrenceReopenQueries;
import org.sabha.attendance.applicationservice.OccurrenceReopenService;
import org.sabha.attendance.applicationservice.ReopenListItem;
import org.sabha.attendance.domain.Reason;
import org.sabha.common.UserId;
import org.sabha.common.web.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Occurrence-reopen BFF surface for the Angular web shell (Slice 13, ADR-0001,
 * ADR-0022). Cookie/session authenticated; the caller arrives already resolved
 * to the local User by the edge, which rejects an authenticated subject with no
 * local User (403, ADR-0030).
 *
 * <ul>
 *   <li>{@code GET /bff/occurrences} — the two-pane list of Occurrences the caller
 *       may reopen, scoped by their Kshetra-tier role and demographic.</li>
 *   <li>{@code POST /bff/occurrences/{id}/reopen} — reopen a Finalized Occurrence
 *       with a required reason; authority and the reason invariant are enforced by
 *       {@link OccurrenceReopenService} and mapped by the global exception handler.</li>
 * </ul>
 */
@RestController
public class OccurrenceReopenBffController {

    private final OccurrenceReopenQueries queries;
    private final OccurrenceReopenService reopenService;

    public OccurrenceReopenBffController(
            OccurrenceReopenQueries queries, OccurrenceReopenService reopenService) {
        this.queries = queries;
        this.reopenService = reopenService;
    }

    @GetMapping("/bff/occurrences")
    public ResponseEntity<List<ReopenListItem>> list(@CurrentUser UserId caller) {
        return ResponseEntity.ok(queries.listForReopener(caller.value()));
    }

    @PostMapping("/bff/occurrences/{occurrenceId}/reopen")
    public ResponseEntity<Void> reopen(
            @PathVariable UUID occurrenceId,
            @RequestBody ReopenRequest req,
            @CurrentUser UserId caller) {
        reopenService.reopen(caller, occurrenceId, new Reason(req.reason()));
        return ResponseEntity.noContent().build();
    }

    public record ReopenRequest(String reason) {
    }
}
