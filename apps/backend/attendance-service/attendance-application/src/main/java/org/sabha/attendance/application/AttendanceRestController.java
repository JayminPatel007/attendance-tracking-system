package org.sabha.attendance.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.sabha.attendance.applicationservice.CurrentRoster;
import org.sabha.attendance.applicationservice.GetCurrentRosterUseCase;
import org.sabha.attendance.applicationservice.MarkAttendanceApplicationService;
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
    private final MarkAttendanceApplicationService markAttendance;
    private final SyncAttendanceApplicationService syncAttendance;

    public AttendanceRestController(
            GetCurrentRosterUseCase getCurrentRoster,
            MarkAttendanceApplicationService markAttendance,
            SyncAttendanceApplicationService syncAttendance) {
        this.getCurrentRoster = getCurrentRoster;
        this.markAttendance = markAttendance;
        this.syncAttendance = syncAttendance;
    }

    @GetMapping("/api/sanchalak/current-roster")
    public ResponseEntity<CurrentRoster> currentRoster(@AuthenticationPrincipal Jwt jwt) {
        UUID keycloakSubject = UUID.fromString(jwt.getSubject());
        return getCurrentRoster.execute(keycloakSubject)
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

    public record MarkRequest(UUID personId, boolean present, Instant clientMarkedAt) {
    }

    public record SyncRequest(Instant rosterVersion, List<MarkingItem> markings) {
    }

    public record MarkingItem(UUID occurrenceId, UUID personId, boolean present, Instant clientMarkedAt) {
    }

    public record SyncResponse(int appliedCount) {
    }
}
