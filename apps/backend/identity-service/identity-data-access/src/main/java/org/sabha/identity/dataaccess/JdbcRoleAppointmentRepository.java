package org.sabha.identity.dataaccess;

import java.sql.Timestamp;

import org.sabha.identity.applicationservice.AppointmentScope;
import org.sabha.identity.applicationservice.RoleAppointmentRepository;
import org.sabha.identity.applicationservice.RoleAppointmentRow;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC write adapter for appointment-produced {@code role_assignments} rows
 * (ADR-0011, Slice 11). Sets only the scope columns relevant to the role; the
 * rest stay null. {@code appointed_by} / {@code appointed_at} carry the audit.
 */
@Repository
public class JdbcRoleAppointmentRepository implements RoleAppointmentRepository {

    private final JdbcClient jdbc;

    public JdbcRoleAppointmentRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(RoleAppointmentRow row) {
        AppointmentScope scope = row.scope();
        jdbc.sql("""
                INSERT INTO role_assignments
                    (id, user_id, role, sabha_id, kshetra_id, zone_id, city_id,
                     demographic, appointed_by, appointed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)
                .param(row.id())
                .param(row.userId())
                .param(scope.role().name())
                .param(scope.sabhaId())
                .param(scope.kshetraId())
                .param(scope.zoneId())
                .param(scope.cityId())
                .param(scope.demographic())
                .param(row.appointedBy())
                .param(Timestamp.from(row.appointedAt()))
                .update();
    }
}
