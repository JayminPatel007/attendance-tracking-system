package org.sabha.bootstrap;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// Sanchalak marks attendance for a Person at an Occurrence. Crosses identity
// (resolve the JWT subject to a local users.id) and attendance (the marking
// itself). Like CurrentRosterController, lives in bootstrap as a tracer-slice
// compromise; refactor to context-respecting ports when Slice 3 introduces the
// proper Occurrence state machine.
@RestController
public class MarkAttendanceController {

    private final JdbcClient jdbc;

    public MarkAttendanceController(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping("/api/occurrences/{occurrenceId}/markings")
    @Transactional
    public ResponseEntity<Void> mark(
            @PathVariable UUID occurrenceId,
            @RequestBody MarkRequest req,
            @AuthenticationPrincipal Jwt jwt) {

        UUID keycloakUserId = UUID.fromString(jwt.getSubject());
        UUID markedBy = jdbc.sql("SELECT id FROM users WHERE keycloak_user_id = ?")
                .param(keycloakUserId)
                .query((rs, n) -> rs.getObject("id", UUID.class))
                .single();

        // Slice 2 gate: marking is only allowed while the Occurrence is OPEN_FOR_MARKING.
        // Slice 3 introduces the full state machine and replaces this raw read with a
        // domain-side check.
        String state = jdbc.sql("SELECT state FROM occurrences WHERE id = ?")
                .param(occurrenceId)
                .query(String.class)
                .optional()
                .orElseThrow(() -> new OccurrenceNotFound(occurrenceId));
        if (!"OPEN_FOR_MARKING".equals(state)) {
            return ResponseEntity.status(409).build();
        }

        jdbc.sql("""
                INSERT INTO attendance_markings (id, occurrence_id, person_id, present, marked_by_user_id)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (occurrence_id, person_id)
                DO UPDATE SET present = EXCLUDED.present,
                              marked_at = now(),
                              marked_by_user_id = EXCLUDED.marked_by_user_id
                """)
                .param(UUID.randomUUID())
                .param(occurrenceId)
                .param(req.personId())
                .param(req.present())
                .param(markedBy)
                .update();

        return ResponseEntity.ok().build();
    }

    public record MarkRequest(UUID personId, boolean present) {
    }

    static class OccurrenceNotFound extends RuntimeException {
        OccurrenceNotFound(UUID id) {
            super("Occurrence not found: " + id);
        }
    }
}
