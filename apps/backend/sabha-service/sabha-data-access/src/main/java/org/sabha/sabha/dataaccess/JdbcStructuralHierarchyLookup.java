package org.sabha.sabha.dataaccess;

import java.util.Optional;
import java.util.UUID;

import org.sabha.common.SabhaScope;
import org.sabha.common.StructuralHierarchyLookup;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of the cross-context {@link StructuralHierarchyLookup} port
 * (ADR-0019): walks the geographic containment chain over the sabha-owned tables
 * so the identity context's appointment Authorization Engine can resolve where a
 * role sits. A Sabha's {@code (demographic, track)} is carried denormalized in
 * {@code sabhas.sabha_kind} as {@code TRACK_DEMOGRAPHIC} (e.g. {@code REGULAR_YUVAK});
 * this splits it on the first underscore into the string tokens the engine matches.
 */
@Repository
public class JdbcStructuralHierarchyLookup implements StructuralHierarchyLookup {

    private final JdbcClient jdbc;

    public JdbcStructuralHierarchyLookup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<SabhaScope> sabhaScope(UUID sabhaId) {
        return jdbc.sql("SELECT kshetra_id, sabha_kind FROM sabhas WHERE id = ?")
                .param(sabhaId)
                .query((rs, n) -> {
                    UUID kshetraId = rs.getObject("kshetra_id", UUID.class);
                    String kind = rs.getString("sabha_kind");
                    int split = kind.indexOf('_');
                    String track = kind.substring(0, split);
                    String demographic = kind.substring(split + 1);
                    return new SabhaScope(kshetraId, demographic, track);
                })
                .optional();
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
