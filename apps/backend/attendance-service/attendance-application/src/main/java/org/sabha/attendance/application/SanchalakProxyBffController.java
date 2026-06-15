package org.sabha.attendance.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.sabha.attendance.applicationservice.OccurrenceShapingService;
import org.sabha.attendance.applicationservice.ProxyOccurrenceItem;
import org.sabha.attendance.applicationservice.ProxySabhaListItem;
import org.sabha.attendance.applicationservice.ProxySabhaQueries;
import org.sabha.common.CallerResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nirikshak Sanchalak-proxy BFF surface for the Angular web shell (Slice 14,
 * ADR-0022). Cookie/session authenticated, so the caller is the Keycloak subject
 * in {@link Authentication#getName()}; an authenticated subject with no local User
 * is unauthorized (403).
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
    private final CallerResolver callers;

    public SanchalakProxyBffController(
            ProxySabhaQueries queries,
            OccurrenceShapingService shapeOccurrence,
            CallerResolver callers) {
        this.queries = queries;
        this.shapeOccurrence = shapeOccurrence;
        this.callers = callers;
    }

    @GetMapping("/bff/proxy/sabhas")
    public ResponseEntity<List<ProxySabhaListItem>> sabhas(Authentication authentication) {
        UUID subject = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(queries.assignedSabhas(callers.requireUserId(subject)));
    }

    @GetMapping("/bff/proxy/sabhas/{sabhaId}/occurrences")
    public ResponseEntity<List<ProxyOccurrenceItem>> occurrences(
            @PathVariable UUID sabhaId, Authentication authentication) {
        UUID subject = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(queries.proxyOccurrences(callers.requireUserId(subject), sabhaId));
    }

    @PostMapping("/bff/proxy/occurrences/{occurrenceId}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID occurrenceId,
            @RequestBody CancelRequest req,
            Authentication authentication) {
        UUID subject = UUID.fromString(authentication.getName());
        shapeOccurrence.cancel(subject, occurrenceId, req.reason());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bff/proxy/occurrences/{occurrenceId}/reschedule")
    public ResponseEntity<Void> reschedule(
            @PathVariable UUID occurrenceId,
            @RequestBody RescheduleRequest req,
            Authentication authentication) {
        UUID subject = UUID.fromString(authentication.getName());
        shapeOccurrence.reschedule(subject, occurrenceId, req.date(), req.startTime(), req.endTime());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bff/proxy/occurrences/{occurrenceId}/venue-override")
    public ResponseEntity<Void> venueOverride(
            @PathVariable UUID occurrenceId,
            @RequestBody VenueOverrideRequest req,
            Authentication authentication) {
        UUID subject = UUID.fromString(authentication.getName());
        shapeOccurrence.overrideVenue(subject, occurrenceId, req.venue());
        return ResponseEntity.noContent().build();
    }

    public record CancelRequest(String reason) {
    }

    public record RescheduleRequest(LocalDate date, LocalTime startTime, LocalTime endTime) {
    }

    public record VenueOverrideRequest(String venue) {
    }
}
