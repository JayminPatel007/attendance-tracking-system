package org.sabha.identity.dataaccess;

import java.util.List;
import java.util.UUID;

import org.sabha.common.RegionalTeamCityLookup;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Resolves the Cities a user is a Regional Team member of, from the
 * {@code role_assignments} table (owned by identity). Drives Zone-creation
 * authorization in the sabha context across the bounded-context seam (ADR-0019,
 * ADR-0024), mirroring {@link JdbcSanyojakZoneLookup} for Kshetras.
 *
 * <p>Regional Team rows are scoped per {@code (City, demographic)}, but a Zone is
 * demographic-agnostic geography, so this collapses the demographic away with a
 * {@code DISTINCT city_id} — any one Regional Team row in a City is enough to
 * authorize. A user with no Regional Team scope resolves to an empty list.</p>
 */
@Repository
public class JdbcRegionalTeamCityLookup implements RegionalTeamCityLookup {

    private static final String REGIONAL_TEAM_ROLE = "REGIONAL_TEAM";

    private final JdbcClient jdbc;

    public JdbcRegionalTeamCityLookup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<UUID> citiesOf(UUID userId) {
        return jdbc.sql("""
                SELECT DISTINCT city_id FROM role_assignments
                WHERE user_id = ? AND role = ? AND city_id IS NOT NULL
                """)
                .param(userId)
                .param(REGIONAL_TEAM_ROLE)
                .query((rs, n) -> rs.getObject("city_id", UUID.class))
                .list();
    }
}
