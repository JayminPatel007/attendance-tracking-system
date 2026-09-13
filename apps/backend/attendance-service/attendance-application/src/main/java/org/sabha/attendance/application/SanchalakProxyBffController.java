package org.sabha.attendance.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.sabha.attendance.applicationservice.OccurrenceShapingService;
import org.sabha.attendance.applicationservice.ProxyOccurrenceItem;
import org.sabha.attendance.applicationservice.ProxySabhaListItem;
import org.sabha.attendance.applicationservice.ProxySabhaQueries;
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
 * Nirikshak Sanchalak-proxy BFF surface for the Angular web shell (Slice 14,
 * ADR-0022). Cookie/session authenticated; the caller arrives already resolved
 * to the local User by the edge, which rejects an authenticated subject with no
 * local User (403, ADR-0030).
 *
 * <ul>
 *   <li>{@code GET /bff/proxy/sabhas} — the picker: Sabhas assigned to the caller
 *       Nirikshak, each with the informational "last seen" hint.</li>
 *   <li>{@code GET /bff/proxy/sabhas/{sabhaId}/occurrences} — the Occurrences of an
 *       assigned Sabha the Nirikshak may shape.</li>
 *   <li>{@code POST /bff/proxy/occurrences/{id}/cancel|reschedule|venue-override} —
 *       the proxy toolkit, delegating to the same {@link OccurrenceShapingService}
 *       the Sanchalak uses. Authority (assigned-Nirikshak scope) and the audit
 *       attribution to the absent Sanchalak are enforced there; out-of-scope or
 *       unknown callers surface as 403 via the global exception handler.</li>
 * </ul>
 */
@RestController
public class SanchalakProxyBffController {

    private final ProxySabhaQueries queries;
    private final OccurrenceShapingService shapeOccurrence;

    public SanchalakProxyBffController(ProxySabhaQueries queries, OccurrenceShapingService shapeOccurrence) {
        this.queries = queries;
        this.shapeOccurrence = shapeOccurrence;
    }

    @GetMapping("/bff/proxy/sabhas")
    public ResponseEntity<List<ProxySabhaListItem>> sabhas(@CurrentUser UserId caller) {
        return ResponseEntity.ok(queries.assignedSabhas(caller.value()));
    }

    @GetMapping("/bff/proxy/sabhas/{sabhaId}/occurrences")
    public ResponseEntity<List<ProxyOccurrenceItem>> occurrences(
            @PathVariable UUID sabhaId, @CurrentUser UserId caller) {
        return ResponseEntity.ok(queries.proxyOccurrences(caller.value(), sabhaId));
    }

    @PostMapping("/bff/proxy/occurrences/{occurrenceId}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID occurrenceId,
            @RequestBody CancelRequest req,
            @CurrentUser UserId caller) {
        shapeOccurrence.cancel(caller, occurrenceId, new Reason(req.reason()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bff/proxy/occurrences/{occurrenceId}/reschedule")
    public ResponseEntity<Void> reschedule(
            @PathVariable UUID occurrenceId,
            @RequestBody RescheduleRequest req,
            @CurrentUser UserId caller) {
        shapeOccurrence.reschedule(caller, occurrenceId, req.date(), req.startTime(), req.endTime());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bff/proxy/occurrences/{occurrenceId}/venue-override")
    public ResponseEntity<Void> venueOverride(
            @PathVariable UUID occurrenceId,
            @RequestBody VenueOverrideRequest req,
            @CurrentUser UserId caller) {
        shapeOccurrence.overrideVenue(caller, occurrenceId, req.venue());
        return ResponseEntity.noContent().build();
    }

    public record CancelRequest(String reason) {
    }

    public record RescheduleRequest(LocalDate date, LocalTime startTime, LocalTime endTime) {
    }

    public record VenueOverrideRequest(String venue) {
    }
}
