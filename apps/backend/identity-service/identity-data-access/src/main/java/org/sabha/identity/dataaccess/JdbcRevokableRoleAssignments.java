package org.sabha.identity.dataaccess;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.sabha.identity.applicationservice.appointment.AppointableRole;
import org.sabha.identity.applicationservice.appointment.AppointmentScope;
import org.sabha.identity.applicationservice.appointment.RevokableAssignment;
import org.sabha.identity.applicationservice.appointment.RevokableRoleAssignments;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter over {@code role_assignments} for revocation (ADR-0026, Issue #89).
 * Revocation is a state change: {@link #markRevoked} stamps {@code revoked_by} /
 * {@code revoked_at} on the row rather than deleting it, so its {@code appointed_by}
 * audit and any inherited structure survive. Every read is scoped to active rows
 * ({@code revoked_at IS NULL}).
 */
@Repository
public class JdbcRevokableRoleAssignments implements RevokableRoleAssignments {

    private final JdbcClient jdbc;

    public JdbcRevokableRoleAssignments(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<RevokableAssignment> findActive(UUID assignmentId) {
        return jdbc.sql("""
                SELECT id, user_id, role, sabha_id, kshetra_id, zone_id, city_id, demographic
                FROM role_assignments
                WHERE id = ? AND revoked_at IS NULL
                """)
                .param(assignmentId)
                .query((rs, n) -> new RevokableAssignment(
                        rs.getObject("id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        new AppointmentScope(
                                AppointableRole.valueOf(rs.getString("role")),
                                rs.getObject("sabha_id", UUID.class),
                                rs.getObject("kshetra_id", UUID.class),
                                rs.getObject("zone_id", UUID.class),
                                rs.getObject("city_id", UUID.class),
                                rs.getString("demographic"))))
                .optional();
    }

    @Override
    public void markRevoked(UUID assignmentId, UUID revokedBy, Instant revokedAt) {
        jdbc.sql("""
                UPDATE role_assignments
                SET revoked_by = ?, revoked_at = ?
                WHERE id = ? AND revoked_at IS NULL
                """)
                .param(revokedBy)
                .param(Timestamp.from(revokedAt))
                .param(assignmentId)
                .update();
    }

    @Override
    public int activeRegionalTeamCount(UUID cityId, String demographic) {
        return jdbc.sql("""
                SELECT COUNT(*) FROM role_assignments
                WHERE role = 'REGIONAL_TEAM' AND city_id = ? AND demographic = ?
                  AND revoked_at IS NULL
                """)
                .param(cityId)
                .param(demographic)
                .query(Integer.class)
                .single();
    }

    @Override
    public int activeRoleCountForUser(UUID userId) {
        return jdbc.sql("""
                SELECT COUNT(*) FROM role_assignments
                WHERE user_id = ? AND revoked_at IS NULL
                """)
                .param(userId)
                .query(Integer.class)
                .single();
    }
}
