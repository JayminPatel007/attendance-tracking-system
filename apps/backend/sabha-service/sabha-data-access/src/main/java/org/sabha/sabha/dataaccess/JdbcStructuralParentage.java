package org.sabha.sabha.dataaccess;

import java.util.Optional;
import java.util.UUID;

import org.sabha.common.StructuralParentage;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of the cross-context {@link StructuralParentage} port
 * (ADR-0019, ADR-0033): one hop each up the geographic containment chain over the
 * sabha-owned tables, so the identity context's appointment Authorization Engine
 * can resolve the scope above the one a role is being filled at.
 */
@Repository
public class JdbcStructuralParentage implements StructuralParentage {

    private final JdbcClient jdbc;

    public JdbcStructuralParentage(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UUID> zoneOfKshetra(UUID kshetraId) {
        return jdbc.sql("SELECT zone_id FROM kshetras WHERE id = ?")
                .param(kshetraId)
                .query((rs, n) -> rs.getObject("zone_id", UUID.class))
                .optional()
                .filter(z -> z != null);
    }

    @Override
    public Optional<UUID> cityOfZone(UUID zoneId) {
        return jdbc.sql("SELECT city_id FROM zones WHERE id = ?")
                .param(zoneId)
                .query((rs, n) -> rs.getObject("city_id", UUID.class))
                .optional();
    }
}
