package org.sabha.analytics.dataaccess;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sabha.analytics.applicationservice.CandidateRow;
import org.sabha.analytics.applicationservice.DashboardOverview;
import org.sabha.analytics.applicationservice.DashboardQueries;
import org.sabha.analytics.applicationservice.SabhaTree;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * CQRS read adapter for the re-engagement dashboard (ADR-0008, ADR-0010). Reads
 * the candidate projection and joins it live to the structural tables, then keeps
 * only rows inside the caller's scope.
 *
 * <p>Scope is one SQL predicate over {@code role_assignments} (plus the
 * Nirikshak's explicit Sabha assignments), mirroring the Occurrence-reopen read:
 * a Sanchalak sees their Sabha, a Nirdeshak their Kshetra × demographic, a
 * Sanyojak their Zone × demographic, the Regional Team their City × demographic,
 * and the MK everything. The demographic is the token after the first underscore
 * of {@code sabha_kind} ({@code TRACK_DEMOGRAPHIC}) — the same derivation
 * {@code JdbcOccurrenceReopenQueries} uses; keep them in step.</p>
 */
@Repository
public class JdbcDashboardQueries implements DashboardQueries {

    private static final int HEADLINE_LIMIT = 100;
    private static final int PEOPLE_LIMIT = 500;

    /**
     * True for candidates whose Home Sabha {@code s} (with Kshetra {@code k}, Zone
     * {@code z}) falls inside the caller {@code :userId}'s granted scope.
     */
    private static final String SCOPE = """
            (
              EXISTS (
                SELECT 1 FROM role_assignments ra
                WHERE ra.user_id = :userId AND (
                       (ra.role IN ('SANCHALAK', 'SAH_SANCHALAK') AND ra.sabha_id = s.id)
                    OR (ra.role IN ('NIRDESHAK', 'SAH_NIRDESHAK') AND ra.kshetra_id = s.kshetra_id
                            AND ra.demographic = split_part(s.sabha_kind, '_', 2))
                    OR (ra.role = 'SANYOJAK' AND ra.zone_id = k.zone_id
                            AND ra.demographic = split_part(s.sabha_kind, '_', 2))
                    OR (ra.role = 'REGIONAL_TEAM' AND ra.city_id = z.city_id
                            AND ra.demographic = split_part(s.sabha_kind, '_', 2))
                    OR (ra.role = 'MADHYASTHA_KARYALAYA')
                )
              )
              OR EXISTS (
                SELECT 1 FROM nirikshak_sabha_assignments na
                WHERE na.nirikshak_user_id = :userId AND na.sabha_id = s.id
              )
            )
            """;

    private static final String FROM_SCOPED = """
            FROM reengagement_candidates rc
            JOIN sabhas s ON s.id = rc.home_sabha_id
            JOIN kshetras k ON k.id = s.kshetra_id
            LEFT JOIN zones z ON z.id = k.zone_id
            """;

    private final JdbcClient jdbc;

    public JdbcDashboardQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public DashboardOverview overview(UUID userId) {
        DashboardOverview.Kpis kpis = jdbc.sql("""
                SELECT COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE rc.tier = 'PRIORITY') AS priority,
                       COUNT(DISTINCT rc.home_sabha_id) AS sabhas
                """ + FROM_SCOPED + "WHERE " + SCOPE)
                .param("userId", userId)
                .query((rs, n) -> new DashboardOverview.Kpis(
                        rs.getInt("total"), rs.getInt("priority"), rs.getInt("sabhas")))
                .single();
        return new DashboardOverview(kpis, candidateRows(userId, HEADLINE_LIMIT));
    }

    @Override
    public List<CandidateRow> people(UUID userId) {
        return candidateRows(userId, PEOPLE_LIMIT);
    }

    @Override
    public SabhaTree sabhaTree(UUID userId) {
        List<TreeRow> rows = jdbc.sql("""
                SELECT z.id AS zone_id, z.name AS zone_name,
                       k.id AS kshetra_id, k.name AS kshetra_name,
                       s.id AS sabha_id, s.sabha_kind,
                       COUNT(*) AS candidate_count
                """ + FROM_SCOPED + "WHERE " + SCOPE + """
                GROUP BY z.id, z.name, k.id, k.name, s.id, s.sabha_kind
                ORDER BY z.name NULLS LAST, k.name, s.sabha_kind
                """)
                .param("userId", userId)
                .query((rs, n) -> new TreeRow(
                        rs.getObject("zone_id", UUID.class), rs.getString("zone_name"),
                        rs.getObject("kshetra_id", UUID.class), rs.getString("kshetra_name"),
                        rs.getObject("sabha_id", UUID.class), rs.getString("sabha_kind"),
                        rs.getInt("candidate_count")))
                .list();
        return assembleTree(rows);
    }

    private List<CandidateRow> candidateRows(UUID userId, int limit) {
        return jdbc.sql("""
                SELECT rc.person_id, p.full_name, rc.home_sabha_id, s.sabha_kind,
                       k.name AS kshetra_name, split_part(s.sabha_kind, '_', 2) AS demographic,
                       rc.missed_streak, rc.tier
                """ + FROM_SCOPED + """
                JOIN persons p ON p.id = rc.person_id
                WHERE """ + SCOPE + """
                ORDER BY rc.missed_streak DESC, p.full_name
                LIMIT :limit
                """)
                .param("userId", userId)
                .param("limit", limit)
                .query((rs, n) -> new CandidateRow(
                        rs.getObject("person_id", UUID.class),
                        rs.getString("full_name"),
                        rs.getObject("home_sabha_id", UUID.class),
                        rs.getString("sabha_kind"),
                        rs.getString("kshetra_name"),
                        rs.getString("demographic"),
                        rs.getInt("missed_streak"),
                        rs.getString("tier")))
                .list();
    }

    /** Folds the flat, pre-ordered grouping rows into the nested Zone → Kshetra → Sabha tree. */
    private static SabhaTree assembleTree(List<TreeRow> rows) {
        Map<UUID, ZoneBuilder> zones = new LinkedHashMap<>();
        for (TreeRow row : rows) {
            zones.computeIfAbsent(row.zoneId(), id -> new ZoneBuilder(id, row.zoneName()))
                    .add(row);
        }
        return new SabhaTree(zones.values().stream().map(ZoneBuilder::build).toList());
    }

    private record TreeRow(UUID zoneId, String zoneName, UUID kshetraId, String kshetraName,
                           UUID sabhaId, String sabhaKind, int candidateCount) {
    }

    private static final class ZoneBuilder {
        private final UUID zoneId;
        private final String zoneName;
        private final Map<UUID, KshetraBuilder> kshetras = new LinkedHashMap<>();

        ZoneBuilder(UUID zoneId, String zoneName) {
            this.zoneId = zoneId;
            this.zoneName = zoneName;
        }

        void add(TreeRow row) {
            kshetras.computeIfAbsent(row.kshetraId(), id -> new KshetraBuilder(id, row.kshetraName()))
                    .add(row);
        }

        SabhaTree.Zone build() {
            List<SabhaTree.Kshetra> built = kshetras.values().stream().map(KshetraBuilder::build).toList();
            int total = built.stream().mapToInt(SabhaTree.Kshetra::candidateCount).sum();
            return new SabhaTree.Zone(zoneId, zoneName, total, built);
        }
    }

    private static final class KshetraBuilder {
        private final UUID kshetraId;
        private final String kshetraName;
        private final List<SabhaTree.Sabha> sabhas = new ArrayList<>();

        KshetraBuilder(UUID kshetraId, String kshetraName) {
            this.kshetraId = kshetraId;
            this.kshetraName = kshetraName;
        }

        void add(TreeRow row) {
            sabhas.add(new SabhaTree.Sabha(row.sabhaId(), row.sabhaKind(), row.candidateCount()));
        }

        SabhaTree.Kshetra build() {
            int total = sabhas.stream().mapToInt(SabhaTree.Sabha::candidateCount).sum();
            return new SabhaTree.Kshetra(kshetraId, kshetraName, total, sabhas);
        }
    }
}
