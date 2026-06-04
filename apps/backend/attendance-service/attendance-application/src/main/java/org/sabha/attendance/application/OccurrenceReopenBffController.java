package org.sabha.attendance.application;

import java.util.List;
import java.util.UUID;

import org.sabha.attendance.applicationservice.OccurrenceReopenQueries;
import org.sabha.attendance.applicationservice.OccurrenceReopenService;
import org.sabha.attendance.applicationservice.ReopenListItem;
import org.sabha.common.CallerResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Occurrence-reopen BFF surface for the Angular web shell (Slice 13, ADR-0001,
 * ADR-0022). Cookie/session authenticated, so the caller is the Keycloak subject
 * in {@link Authentication#getName()}; an authenticated subject with no local
 * User is unauthorized (403).
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
    private final CallerResolver callers;

    public OccurrenceReopenBffController(
            OccurrenceReopenQueries queries,
            OccurrenceReopenService reopenService,
            CallerResolver callers) {
        this.queries = queries;
        this.reopenService = reopenService;
        this.callers = callers;
    }

    @GetMapping("/bff/occurrences")
    public ResponseEntity<List<ReopenListItem>> list(Authentication authentication) {
        UUID subject = UUID.fromString(authentication.getName());
        return callers.resolveUserId(subject)
                .map(userId -> ResponseEntity.ok(queries.listForReopener(userId)))
                .orElseGet(() -> ResponseEntity.status(403).build());
    }

    @PostMapping("/bff/occurrences/{occurrenceId}/reopen")
    public ResponseEntity<Void> reopen(
            @PathVariable UUID occurrenceId,
            @RequestBody ReopenRequest req,
            Authentication authentication) {
        UUID subject = UUID.fromString(authentication.getName());
        // An unknown caller surfaces as 403 via the global handler; no pre-check needed.
        reopenService.reopen(subject, occurrenceId, req.reason());
        return ResponseEntity.noContent().build();
    }

    public record ReopenRequest(String reason) {
    }
}
