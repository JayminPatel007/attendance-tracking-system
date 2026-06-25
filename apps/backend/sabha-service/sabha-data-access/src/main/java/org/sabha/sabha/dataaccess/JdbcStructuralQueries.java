package org.sabha.sabha.dataaccess;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.sabha.common.NirdeshakScopeLookup.NirdeshakScope;
import org.sabha.sabha.applicationservice.StructuralQueries;
import org.sabha.sabha.domain.Demographic;
import org.sabha.sabha.domain.Track;
import org.springframework.jdbc.core.RowMapper;
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
        return jdbc.sql("""
                SELECT id, name, (SELECT count(*) FROM zones z WHERE z.city_id = c.id) AS zone_count
                FROM cities c ORDER BY name
                """)
                .query(CITY_VIEW)
                .list();
    }

    @Override
    public List<ZoneView> listZones() {
        return jdbc.sql("""
                SELECT z.id, z.name, z.city_id, c.name AS city_name,
                       (SELECT count(*) FROM kshetras k WHERE k.zone_id = z.id) AS kshetra_count
                FROM zones z JOIN cities c ON c.id = z.city_id
                ORDER BY c.name, z.name
                """)
                .query(ZONE_VIEW)
                .list();
    }

    @Override
    public List<SabhaKindView> listSabhaKinds() {
        return jdbc.sql("SELECT id, demographic, track, retired_at FROM sabha_kinds ORDER BY demographic, track")
                .query((rs, n) -> new SabhaKindView(
                        rs.getObject("id", UUID.class),
                        Demographic.valueOf(rs.getString("demographic")),
                        Track.valueOf(rs.getString("track")),
                        toInstant(rs.getTimestamp("retired_at"))))
                .list();
    }

    @Override
    public List<KshetraView> listKshetras(UUID zoneId) {
        return jdbc.sql("""
                SELECT id, name, zone_id, (SELECT count(*) FROM sabhas s WHERE s.kshetra_id = k.id) AS sabha_count
                FROM kshetras k WHERE zone_id = ? ORDER BY name
                """)
                .param(zoneId)
                .query(KSHETRA_VIEW)
                .list();
    }

    @Override
    public List<SabhaView> sabhasOwnedBy(Collection<NirdeshakScope> scopes) {
        if (scopes.isEmpty()) {
            return List.of();
        }
        String tuples = scopes.stream().map(s -> "(?, ?)").collect(Collectors.joining(", "));
        var spec = jdbc.sql("""
                SELECT s.id, s.kshetra_id, ks.name AS kshetra_name, k.demographic, k.track,
                       s.standing_venue,
                       (SELECT count(*) FROM occurrences o WHERE o.sabha_id = s.id) AS occurrence_count
                FROM sabhas s
                JOIN sabha_kinds k ON k.id = s.sabha_kind_id
                JOIN kshetras ks ON ks.id = s.kshetra_id
                WHERE (s.kshetra_id, k.demographic) IN (%s)
                ORDER BY ks.name, k.demographic, k.track
                """.formatted(tuples));
        for (NirdeshakScope scope : scopes) {
            spec = spec.param(scope.kshetraId()).param(scope.demographic());
        }
        return spec.query(SABHA_VIEW).list();
    }

    @Override
    public List<ZoneView> zonesByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("""
                SELECT z.id, z.name, z.city_id, c.name AS city_name,
                       (SELECT count(*) FROM kshetras k WHERE k.zone_id = z.id) AS kshetra_count
                FROM zones z JOIN cities c ON c.id = z.city_id
                WHERE z.id IN (:ids)
                ORDER BY c.name, z.name
                """)
                .param("ids", ids)
                .query(ZONE_VIEW)
                .list();
    }

    @Override
    public List<CityView> citiesByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("""
                SELECT id, name, (SELECT count(*) FROM zones z WHERE z.city_id = c.id) AS zone_count
                FROM cities c WHERE id IN (:ids) ORDER BY name
                """)
                .param("ids", ids)
                .query(CITY_VIEW)
                .list();
    }

    private static final RowMapper<CityView> CITY_VIEW = (rs, n) -> new CityView(
            rs.getObject("id", UUID.class), rs.getString("name"), rs.getInt("zone_count"));

    private static final RowMapper<ZoneView> ZONE_VIEW = (rs, n) -> new ZoneView(
            rs.getObject("id", UUID.class),
            rs.getString("name"),
            rs.getObject("city_id", UUID.class),
            rs.getString("city_name"),
            rs.getInt("kshetra_count"));

    private static final RowMapper<KshetraView> KSHETRA_VIEW = (rs, n) -> new KshetraView(
            rs.getObject("id", UUID.class),
            rs.getString("name"),
            rs.getObject("zone_id", UUID.class),
            rs.getInt("sabha_count"));

    private static final RowMapper<SabhaView> SABHA_VIEW = (rs, n) -> new SabhaView(
            rs.getObject("id", UUID.class),
            rs.getObject("kshetra_id", UUID.class),
            rs.getString("kshetra_name"),
            Demographic.valueOf(rs.getString("demographic")),
            Track.valueOf(rs.getString("track")),
            rs.getString("standing_venue"),
            rs.getInt("occurrence_count"));

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
