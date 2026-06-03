package org.sabha.sabha.dataaccess;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.sabha.sabha.applicationservice.StructuralQueries;
import org.sabha.sabha.domain.Demographic;
import org.sabha.sabha.domain.Track;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** JDBC read adapter for the structural-admin screens — reads sabha-owned tables only. */
@Repository
public class JdbcStructuralQueries implements StructuralQueries {

    private final JdbcClient jdbc;

    public JdbcStructuralQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<CityView> listCities() {
        return jdbc.sql("SELECT id, name FROM cities ORDER BY name")
                .query((rs, n) -> new CityView(rs.getObject("id", UUID.class), rs.getString("name")))
                .list();
    }

    @Override
    public List<ZoneView> listZones() {
        return jdbc.sql("""
                SELECT z.id, z.name, z.city_id, c.name AS city_name
                FROM zones z JOIN cities c ON c.id = z.city_id
                ORDER BY c.name, z.name
                """)
                .query((rs, n) -> new ZoneView(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getObject("city_id", UUID.class),
                        rs.getString("city_name")))
                .list();
    }

    @Override
    public List<SabhaKindView> listSabhaKinds() {
        return jdbc.sql("SELECT id, demographic, track FROM sabha_kinds ORDER BY demographic, track")
                .query((rs, n) -> new SabhaKindView(
                        rs.getObject("id", UUID.class),
                        Demographic.valueOf(rs.getString("demographic")),
                        Track.valueOf(rs.getString("track"))))
                .list();
    }

    @Override
    public List<KshetraView> listKshetras(UUID zoneId) {
        return jdbc.sql("SELECT id, name, zone_id FROM kshetras WHERE zone_id = ? ORDER BY name")
                .param(zoneId)
                .query((rs, n) -> new KshetraView(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getObject("zone_id", UUID.class)))
                .list();
    }

    @Override
    public List<ZoneView> zonesByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("""
                SELECT z.id, z.name, z.city_id, c.name AS city_name
                FROM zones z JOIN cities c ON c.id = z.city_id
                WHERE z.id IN (:ids)
                ORDER BY c.name, z.name
                """)
                .param("ids", ids)
                .query((rs, n) -> new ZoneView(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getObject("city_id", UUID.class),
                        rs.getString("city_name")))
                .list();
    }
}
