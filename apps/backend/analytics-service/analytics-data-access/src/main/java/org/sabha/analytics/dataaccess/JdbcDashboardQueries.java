package org.sabha.analytics.dataaccess;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sabha.analytics.applicationservice.CandidateRow;
import org.sabha.analytics.applicationservice.DashboardOverview;
import org.sabha.analytics.applicationservice.DashboardQueries;
import org.sabha.analytics.applicationservice.DashboardScope;
import org.sabha.analytics.applicationservice.SabhaTree;
import org.sabha.common.CallerVisibility;
import org.sabha.common.SabhaKind;
import org.sabha.common.VisibilityTier;
import org.sabha.common.WhereClause;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * CQRS read adapter for the re-engagement dashboard (ADR-0008, ADR-0010). Reads
 * the candidate projection and joins it live to the structural tables, then keeps
 * only rows inside the caller's scope.
 *
 * <p>The {@link DashboardScope} the engine resolved decides the WHERE predicate.
 * A {@code RoleScoped} read is the canonical {@link CallerVisibility} predicate
 * granted every tier: a Sanchalak sees their Sabha, a Nirdeshak their Kshetra ×
 * demographic, a Sanyojak their Zone × demographic, the Regional Team their City ×
 * demographic, the MK everything, and a Nirikshak the Sabhas explicitly assigned to
 * them ({@link VisibilityTier#NIRIKSHAK_PROXY}). A {@code CityScoped} read is the
 * Sant's universal-read exception (Slice 17): every candidate in one City regardless
 * of role. A {@code NoCity} read (a Sant who has not picked yet) matches nothing.</p>
 */
@Repository
public class JdbcDashboardQueries implements DashboardQueries {

    private static final int HEADLINE_LIMIT = 100;
    private static final int PEOPLE_LIMIT = 500;

    /**
     * The tiers the dashboard grants: every operational and oversight tier, with the
     * Nirikshak resolving via its explicit proxy assignment ({@link
     * VisibilityTier#NIRIKSHAK_PROXY}) rather than the role_assignments Kshetra-tier
     * {@link VisibilityTier#NIRIKSHAK}. Listed explicitly (opt-in) so a tier added to
     * {@link VisibilityTier} is not silently granted dashboard visibility — extending
     * the grant is a deliberate edit here.
     */
    private static final EnumSet<VisibilityTier> GRANTED_TIERS = EnumSet.of(
            VisibilityTier.SANCHALAK,
            VisibilityTier.SAH_SANCHALAK,
            VisibilityTier.NIRDESHAK,
            VisibilityTier.SAH_NIRDESHAK,
            VisibilityTier.SANYOJAK,
            VisibilityTier.REGIONAL_TEAM,
            VisibilityTier.MADHYASTHA_KARYALAYA,
            VisibilityTier.NIRIKSHAK_PROXY);

    /**
     * True for candidates whose Home Sabha {@code s} (with Kshetra {@code k}, Zone
     * {@code z}) falls inside the caller {@code :userId}'s granted role scope.
     */
    private static final String ROLE_SCOPE = CallerVisibility.predicate(
            new CallerVisibility.Aliases("s", "k", "z"), GRANTED_TIERS);

    /** The Sant's universal read: every candidate whose Zone belongs to one City. */
    private static final String CITY_SCOPE = "z.city_id = :cityId";

    /** A Sant who has not chosen a City sees nothing. */
    private static final String NO_SCOPE = "1 = 0";

    private static final String FROM_SCOPED = """
            FROM reengagement_candidates rc
            JOIN sabhas s ON s.id = rc.home_sabha_id
            JOIN kshetras k ON k.id = s.kshetra_id
            LEFT JOIN zones z ON z.id = k.zone_id
            """;

    /*
     * The three section queries, with a single {@code %s} slot for the scope's
     * {@link WhereClause} (filled via {@link #bind}). The clause carries its own
     * {@code WHERE} keyword, so the slot is a bare {@code %s} and no template has to
     * keep the keyword and the predicate spaced apart by hand.
     */
    private static final String OVERVIEW_KPIS_SQL = """
            SELECT COUNT(*) AS total,
                   COUNT(*) FILTER (WHERE rc.tier = 'PRIORITY') AS priority,
                   COUNT(DISTINCT rc.home_sabha_id) AS sabhas
            """ + FROM_SCOPED + """
            %s
            """;

    private static final String SABHA_TREE_SQL = """
            SELECT z.id AS zone_id, z.name AS zone_name,
                   k.id AS kshetra_id, k.name AS kshetra_name,
                   s.id AS sabha_id, s.sabha_kind,
                   COUNT(*) AS candidate_count
            """ + FROM_SCOPED + """
            %s
            GROUP BY z.id, z.name, k.id, k.name, s.id, s.sabha_kind
            ORDER BY z.name NULLS LAST, k.name, s.sabha_kind
            """;

    private static final String PEOPLE_SQL = """
            SELECT rc.person_id, p.full_name, rc.home_sabha_id, s.sabha_kind,
                   k.name AS kshetra_name, %s AS demographic,
                   rc.missed_streak, rc.tier
            """.formatted(SabhaKind.demographicSql("s")) + FROM_SCOPED + """
            JOIN persons p ON p.id = rc.person_id
            %s
            ORDER BY rc.missed_streak DESC, p.full_name
            LIMIT :limit
            """;

    private final JdbcClient jdbc;

    public JdbcDashboardQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public DashboardOverview overview(DashboardScope scope) {
        WhereClause where = bind(scope);
        DashboardOverview.Kpis kpis = jdbc.sql(OVERVIEW_KPIS_SQL.formatted(where.sql()))
                .params(where.params())
                .query((rs, n) -> new DashboardOverview.Kpis(
                        rs.getInt("total"), rs.getInt("priority"), rs.getInt("sabhas")))
                .single();
        return new DashboardOverview(kpis, candidateRows(scope, HEADLINE_LIMIT));
    }

    @Override
    public List<CandidateRow> people(DashboardScope scope) {
        return candidateRows(scope, PEOPLE_LIMIT);
    }

    @Override
    public SabhaTree sabhaTree(DashboardScope scope) {
        WhereClause where = bind(scope);
        List<TreeRow> rows = jdbc.sql(SABHA_TREE_SQL.formatted(where.sql()))
                .params(where.params())
                .query((rs, n) -> new TreeRow(
                        rs.getObject("zone_id", UUID.class), rs.getString("zone_name"),
                        rs.getObject("kshetra_id", UUID.class), rs.getString("kshetra_name"),
                        rs.getObject("sabha_id", UUID.class), rs.getString("sabha_kind"),
                        rs.getInt("candidate_count")))
                .list();
        return assembleTree(rows);
    }

    private List<CandidateRow> candidateRows(DashboardScope scope, int limit) {
        WhereClause where = bind(scope);
        return jdbc.sql(PEOPLE_SQL.formatted(where.sql()))
                .params(where.params())
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

    /** The WHERE clause and its named params for one {@link DashboardScope}. */
    private static WhereClause bind(DashboardScope scope) {
        return switch (scope) {
            case DashboardScope.RoleScoped r ->
                    WhereClause.create().and(ROLE_SCOPE, CallerVisibility.USER_PARAM, r.userId());
            case DashboardScope.CityScoped c -> WhereClause.create().and(CITY_SCOPE, "cityId", c.cityId());
            case DashboardScope.NoCity ignored -> WhereClause.create().and(NO_SCOPE);
        };
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
