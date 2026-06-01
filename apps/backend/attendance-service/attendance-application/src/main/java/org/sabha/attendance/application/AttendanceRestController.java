package org.sabha.attendance.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.sabha.attendance.applicationservice.CurrentOccurrence;
import org.sabha.attendance.applicationservice.CurrentRoster;
import org.sabha.attendance.applicationservice.GetCurrentOccurrenceUseCase;
import org.sabha.attendance.applicationservice.GetCurrentRosterUseCase;
import org.sabha.attendance.applicationservice.MarkAttendanceApplicationService;
import org.sabha.attendance.applicationservice.MarkAttendanceApplicationService.MarkItem;
import org.sabha.attendance.applicationservice.OccurrenceShapingService;
import org.sabha.attendance.applicationservice.SyncAttendanceApplicationService;
import org.sabha.attendance.applicationservice.SyncRequestItem;
import org.sabha.attendance.applicationservice.SyncResult;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AttendanceRestController {

    private final GetCurrentRosterUseCase getCurrentRoster;
    private final GetCurrentOccurrenceUseCase getCurrentOccurrence;
    private final MarkAttendanceApplicationService markAttendance;
    private final SyncAttendanceApplicationService syncAttendance;
    private final OccurrenceShapingService shapeOccurrence;

    public AttendanceRestController(
            GetCurrentRosterUseCase getCurrentRoster,
            GetCurrentOccurrenceUseCase getCurrentOccurrence,
            MarkAttendanceApplicationService markAttendance,
            SyncAttendanceApplicationService syncAttendance,
            OccurrenceShapingService shapeOccurrence) {
        this.getCurrentRoster = getCurrentRoster;
        this.getCurrentOccurrence = getCurrentOccurrence;
        this.markAttendance = markAttendance;
        this.syncAttendance = syncAttendance;
        this.shapeOccurrence = shapeOccurrence;
    }

    @GetMapping("/api/sanchalak/current-roster")
    public ResponseEntity<CurrentRoster> currentRoster(@AuthenticationPrincipal Jwt jwt) {
        UUID keycloakSubject = UUID.fromString(jwt.getSubject());
        return getCurrentRoster.execute(keycloakSubject)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/api/sanchalak/current-occurrence")
    public ResponseEntity<CurrentOccurrence> currentOccurrence(@AuthenticationPrincipal Jwt jwt) {
        UUID keycloakSubject = UUID.fromString(jwt.getSubject());
        return getCurrentOccurrence.execute(keycloakSubject)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/api/occurrences/{occurrenceId}/markings")
    public ResponseEntity<Void> mark(
            @PathVariable UUID occurrenceId,
            @RequestBody MarkRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        UUID keycloakSubject = UUID.fromString(jwt.getSubject());
        Instant clientMarkedAt = req.clientMarkedAt() != null ? req.clientMarkedAt() : Instant.now();
        markAttendance.execute(keycloakSubject, occurrenceId, req.personId(), req.present(), clientMarkedAt);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/occurrences/{occurrenceId}/walk-ins")
    public ResponseEntity<Void> walkIn(
            @PathVariable UUID occurrenceId,
            @RequestBody WalkInRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        UUID keycloakSubject = UUID.fromString(jwt.getSubject());
        Instant clientMarkedAt = req.clientMarkedAt() != null ? req.clientMarkedAt() : Instant.now();
        markAttendance.executeBatch(keycloakSubject, occurrenceId,
                List.of(MarkItem.walkIn(req.personId(), clientMarkedAt)));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/sync")
    public ResponseEntity<SyncResponse> sync(
            @RequestBody SyncRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        UUID keycloakSubject = UUID.fromString(jwt.getSubject());
        List<SyncRequestItem> items = req.markings().stream()
                .map(m -> new SyncRequestItem(m.occurrenceId(), m.personId(), m.present(), m.clientMarkedAt()))
                .toList();
        SyncResult result = syncAttendance.execute(keycloakSubject, req.rosterVersion(), items);
        return ResponseEntity.ok(new SyncResponse(result.appliedCount()));
    }

    @PostMapping("/api/occurrences/{occurrenceId}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID occurrenceId,
            @RequestBody CancelRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        UUID keycloakSubject = UUID.fromString(jwt.getSubject());
        shapeOccurrence.cancel(keycloakSubject, occurrenceId, req.reason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/occurrences/{occurrenceId}/revert")
    public ResponseEntity<Void> revert(
            @PathVariable UUID occurrenceId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID keycloakSubject = UUID.fromString(jwt.getSubject());
        shapeOccurrence.revert(keycloakSubject, occurrenceId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/occurrences/{occurrenceId}/reschedule")
    public ResponseEntity<Void> reschedule(
            @PathVariable UUID occurrenceId,
            @RequestBody RescheduleRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        UUID keycloakSubject = UUID.fromString(jwt.getSubject());
        shapeOccurrence.reschedule(keycloakSubject, occurrenceId,
                req.date(), req.startTime(), req.endTime());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/occurrences/{occurrenceId}/venue-override")
    public ResponseEntity<Void> venueOverride(
            @PathVariable UUID occurrenceId,
            @RequestBody VenueOverrideRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        UUID keycloakSubject = UUID.fromString(jwt.getSubject());
        shapeOccurrence.overrideVenue(keycloakSubject, occurrenceId, req.venue());
        return ResponseEntity.ok().build();
    }

    public record CancelRequest(String reason) {
    }

    public record RescheduleRequest(LocalDate date, LocalTime startTime, LocalTime endTime) {
    }

    public record VenueOverrideRequest(String venue) {
    }

    public record MarkRequest(UUID personId, boolean present, Instant clientMarkedAt) {
    }

    public record WalkInRequest(UUID personId, Instant clientMarkedAt) {
    }

    public record SyncRequest(Instant rosterVersion, List<MarkingItem> markings) {
    }

    public record MarkingItem(UUID occurrenceId, UUID personId, boolean present, Instant clientMarkedAt) {
    }

    public record SyncResponse(int appliedCount) {
    }
}
