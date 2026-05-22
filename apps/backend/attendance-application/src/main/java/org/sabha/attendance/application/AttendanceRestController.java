package org.sabha.attendance.application;

import java.util.UUID;

import org.sabha.attendance.domain.CurrentRoster;
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
    private final MarkAttendanceUseCase markAttendance;

    public AttendanceRestController(
            GetCurrentRosterUseCase getCurrentRoster,
            MarkAttendanceUseCase markAttendance) {
        this.getCurrentRoster = getCurrentRoster;
        this.markAttendance = markAttendance;
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
        try {
            markAttendance.execute(keycloakSubject, occurrenceId, req.personId(), req.present());
            return ResponseEntity.ok().build();
        } catch (OccurrenceNotOpenForMarkingException e) {
            return ResponseEntity.status(409).build();
        } catch (OccurrenceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    public record MarkRequest(UUID personId, boolean present) {
    }
}
