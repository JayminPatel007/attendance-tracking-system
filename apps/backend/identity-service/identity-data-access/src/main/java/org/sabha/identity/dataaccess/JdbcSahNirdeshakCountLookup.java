package org.sabha.identity.dataaccess;

import java.util.UUID;

import org.sabha.identity.applicationservice.appointment.SahNirdeshakCountLookup;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Counts active Sah-Nirdeshaks for a (Kshetra, demographic) from
 * {@code role_assignments} (ADR-0025 §3), so {@code RoleAppointmentService} can
 * cap appointments at two. Revoked rows ({@code revoked_at} set) are excluded, so
 * a revocation frees a slot under the cap (ADR-0026).
 */
@Repository
public class JdbcSahNirdeshakCountLookup implements SahNirdeshakCountLookup {

    private final JdbcClient jdbc;

    public JdbcSahNirdeshakCountLookup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int activeCount(UUID kshetraId, String demographic) {
        return jdbc.sql("""
                SELECT COUNT(*) FROM role_assignments
                WHERE role = 'SAH_NIRDESHAK' AND kshetra_id = ? AND demographic = ?
                  AND revoked_at IS NULL
                """)
                .param(kshetraId)
                .param(demographic)
                .query(Integer.class)
                .single();
    }
}
