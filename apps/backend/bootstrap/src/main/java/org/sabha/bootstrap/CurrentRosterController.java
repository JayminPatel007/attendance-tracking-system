package org.sabha.bootstrap;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Cross-context read for the Sanchalak's mobile screen: resolves user -> sabha
// -> today's open occurrence -> roster (+ existing markings). Lives in
// bootstrap because the query spans identity, sabha, and attendance tables;
// ADR-0015 forbids reach-ins between context modules. When this view grows
// non-trivial, extract to a dedicated read-model module with ports per ADR-0015.
@RestController
public class CurrentRosterController {

    private final JdbcClient jdbc;

    public CurrentRosterController(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/sanchalak/current-roster")
    public ResponseEntity<CurrentRosterResponse> currentRoster(@AuthenticationPrincipal Jwt jwt) {
        UUID keycloakUserId = UUID.fromString(jwt.getSubject());

        Optional<OccurrenceView> openOccurrence = jdbc.sql("""
                SELECT o.id AS occurrence_id, o.occurrence_date, o.state, o.sabha_id
                FROM users u
                JOIN role_assignments ra ON ra.user_id = u.id AND ra.role = 'SANCHALAK'
                JOIN occurrences o ON o.sabha_id = ra.sabha_id AND o.state = 'OPEN_FOR_MARKING'
                WHERE u.keycloak_user_id = ?
                ORDER BY o.occurrence_date DESC
                LIMIT 1
                """)
                .param(keycloakUserId)
                .query((rs, n) -> new OccurrenceView(
                        rs.getObject("occurrence_id", UUID.class),
                        rs.getObject("occurrence_date", LocalDate.class),
                        rs.getString("state"),
                        rs.getObject("sabha_id", UUID.class)))
                .optional();

        if (openOccurrence.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        OccurrenceView occ = openOccurrence.get();

        List<RosterEntry> roster = jdbc.sql("""
                SELECT p.id AS person_id, p.full_name, am.present
                FROM home_sabhas hs
                JOIN persons p ON p.id = hs.person_id
                LEFT JOIN attendance_markings am
                       ON am.occurrence_id = ? AND am.person_id = p.id
                WHERE hs.sabha_id = ?
                ORDER BY p.id
                """)
                .param(occ.id())
                .param(occ.sabhaId())
                .query((rs, n) -> {
                    boolean present = rs.getBoolean("present");
                    Boolean nullableP = rs.wasNull() ? null : present;
                    return new RosterEntry(
                            rs.getObject("person_id", UUID.class),
                            rs.getString("full_name"),
                            nullableP);
                })
                .list();

        return ResponseEntity.ok(new CurrentRosterResponse(occ, roster));
    }

    public record CurrentRosterResponse(OccurrenceView occurrence, List<RosterEntry> roster) {
    }

    public record OccurrenceView(UUID id, LocalDate date, String state, UUID sabhaId) {
    }

    public record RosterEntry(UUID personId, String fullName, Boolean present) {
    }
}
