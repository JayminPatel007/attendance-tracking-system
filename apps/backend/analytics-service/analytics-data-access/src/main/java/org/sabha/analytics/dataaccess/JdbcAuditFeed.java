package org.sabha.analytics.dataaccess;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.sabha.analytics.applicationservice.AuditEntry;
import org.sabha.analytics.applicationservice.AuditFilter;
import org.sabha.analytics.applicationservice.AuditLogQueries;
import org.sabha.analytics.applicationservice.AuditScope;
import org.sabha.analytics.applicationservice.AuditTargetType;
import org.sabha.common.WhereClause;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.JdbcClient.StatementSpec;
import org.springframework.stereotype.Repository;

/**
 * CQRS read adapter for the audit feed (ADR-0023, Slice 19). UNIONs the five
 * audit-bearing source tables into the common projection
 * {@code (id, at, actor, on_behalf, target_type, target_id, action, detail,
 * kshetra_id, zone_id, city_id)}, then in an outer query joins {@code users} for
 * the actor / on-behalf display names and applies the caller's
 * {@link AuditScope} predicate plus the {@link AuditFilter} clauses.
 *
 * <p>The load-bearing detail is that each UNION branch resolves its own geography
 * by joining out to the structural tables: an Occurrence reaches it through its
 * Sabha, a role appointment through whichever of its sabha/kshetra/zone/city scope
 * columns is set, a structural creation <em>is</em> its own geography, a Sabha
 * definition through its Kshetra, a selection through its recorded Kshetra. State-
 * level acts (Sabha-kind creation, MK-scoped appointment) resolve to all-{@code
 * NULL} geography, so they fall outside any {@link AuditScope.Scoped} predicate
 * and surface only for {@link AuditScope.Unrestricted} callers.</p>
 *
 * <p>{@code attendance_markings} is deliberately not a branch — it is the
 * system's highest-frequency data and would drown the governance signal (ADR-0023).</p>
 */
@Repository
public class JdbcAuditFeed implements AuditLogQueries {

    /**
     * The audit feed before scoping/filtering: every audited act in the common
     * projection. {@code UNION ALL} (not {@code UNION}) — branches never collide,
     * and a selection row legitimately appears twice (nominate + decide).
     */
    private static final String FEED = """
            -- Occurrence lifecycle (the only proxy-bearing source, Slice 14)
            SELECT ost.id AS id, ost.at_timestamp AS at,
                   ost.actor_user_id AS actor_user_id, ost.on_behalf_of_user_id AS on_behalf_of_user_id,
                   'OCCURRENCE' AS target_type, ost.occurrence_id AS target_id,
                   ost.action AS action, ost.reason AS detail,
                   s.kshetra_id AS kshetra_id, k.zone_id AS zone_id, z.city_id AS city_id
            FROM occurrence_state_transitions ost
            JOIN occurrences o ON o.id = ost.occurrence_id
            JOIN sabhas s ON s.id = o.sabha_id
            LEFT JOIN kshetras k ON k.id = s.kshetra_id
            LEFT JOIN zones z ON z.id = k.zone_id

            UNION ALL

            -- Role appointment (ADR-0011); geography from whichever scope column is set
            SELECT ra.id, ra.appointed_at, ra.appointed_by, NULL,
                   'ROLE_ASSIGNMENT', ra.id, 'APPOINTED', ra.role,
                   COALESCE(ra.kshetra_id, rsk.id),
                   COALESCE(ra.zone_id, rk.zone_id, rsk.zone_id),
                   COALESCE(ra.city_id, rz.city_id, rkz.city_id, rskz.city_id)
            FROM role_assignments ra
            LEFT JOIN sabhas rs ON rs.id = ra.sabha_id
            LEFT JOIN kshetras rsk ON rsk.id = rs.kshetra_id
            LEFT JOIN zones rskz ON rskz.id = rsk.zone_id
            LEFT JOIN kshetras rk ON rk.id = ra.kshetra_id
            LEFT JOIN zones rkz ON rkz.id = rk.zone_id
            LEFT JOIN zones rz ON rz.id = ra.zone_id
            WHERE ra.appointed_at IS NOT NULL

            UNION ALL

            -- Sabha definition (Slice 12)
            SELECT s.id, s.created_at, s.created_by, NULL,
                   'SABHA', s.id, 'CREATED', s.sabha_kind,
                   s.kshetra_id, k.zone_id, z.city_id
            FROM sabhas s
            LEFT JOIN kshetras k ON k.id = s.kshetra_id
            LEFT JOIN zones z ON z.id = k.zone_id
            WHERE s.created_by IS NOT NULL

            UNION ALL

            -- Structural creation: Kshetra (ADR-0009)
            SELECT k.id, k.created_at, k.created_by, NULL,
                   'STRUCTURAL', k.id, 'KSHETRA_CREATED', k.name,
                   k.id, k.zone_id, z.city_id
            FROM kshetras k
            LEFT JOIN zones z ON z.id = k.zone_id
            WHERE k.created_by IS NOT NULL

            UNION ALL

            -- Structural creation: Zone
            SELECT z.id, z.created_at, z.created_by, NULL,
                   'STRUCTURAL', z.id, 'ZONE_CREATED', z.name,
                   NULL, z.id, z.city_id
            FROM zones z
            WHERE z.created_by IS NOT NULL

            UNION ALL

            -- Structural creation: City
            SELECT c.id, c.created_at, c.created_by, NULL,
                   'STRUCTURAL', c.id, 'CITY_CREATED', c.name,
                   NULL, NULL, c.id
            FROM cities c
            WHERE c.created_by IS NOT NULL

            UNION ALL

            -- Structural creation: Sabha kind (State-level, no geography)
            SELECT sk.id, sk.created_at, sk.created_by, NULL,
                   'STRUCTURAL', sk.id, 'SABHA_KIND_CREATED', sk.track || '_' || sk.demographic,
                   NULL, NULL, NULL
            FROM sabha_kinds sk
            WHERE sk.created_by IS NOT NULL

            UNION ALL

            -- Selection nominate (Slice 16)
            SELECT sn.id, sn.nominated_at, sn.nominated_by, NULL,
                   'PERSON', sn.person_id, 'SELECTION_NOMINATED', sn.track || ' ' || sn.demographic,
                   sn.kshetra_id, k.zone_id, z.city_id
            FROM selection_nominations sn
            LEFT JOIN kshetras k ON k.id = sn.kshetra_id
            LEFT JOIN zones z ON z.id = k.zone_id

            UNION ALL

            -- Selection decide (approve / reject / deselect)
            SELECT sn.id, sn.decided_at, sn.decided_by, NULL,
                   'PERSON', sn.person_id, 'SELECTION_' || sn.status, sn.rejection_reason,
                   sn.kshetra_id, k.zone_id, z.city_id
            FROM selection_nominations sn
            LEFT JOIN kshetras k ON k.id = sn.kshetra_id
            LEFT JOIN zones z ON z.id = k.zone_id
            WHERE sn.decided_at IS NOT NULL
            """;

    /**
     * The scoped/filtered query over {@link #FEED}: joins {@code users} for the
     * display names, then interpolates the assembled {@link WhereClause} through the
     * single {@code %s} slot, newest first.
     */
    private static final String SELECT_FROM_FEED = """
            SELECT f.id, f.at, f.actor_user_id, ua.username AS actor_name,
                   f.on_behalf_of_user_id, ub.username AS on_behalf_name,
                   f.target_type, f.target_id, f.action, f.detail
            FROM (
            """ + FEED + """
            ) f
            LEFT JOIN users ua ON ua.id = f.actor_user_id
            LEFT JOIN users ub ON ub.id = f.on_behalf_of_user_id
            %s
            ORDER BY f.at DESC NULLS LAST
            LIMIT :limit
            """;

    private final JdbcClient jdbc;

    public JdbcAuditFeed(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<AuditEntry> find(AuditScope scope, AuditFilter filter) {
        if (scope instanceof AuditScope.Denied) {
            return List.of();
        }
        WhereClause where = WhereClause.create();
        appendScope(scope, where);
        appendFilters(filter, where);

        String sql = SELECT_FROM_FEED.formatted(where.sql());
        StatementSpec spec = jdbc.sql(sql).param("limit", filter.limit());
        where.params().forEach(spec::param);
        return spec.query((rs, n) -> new AuditEntry(
                        rs.getObject("id", UUID.class),
                        instant(rs.getTimestamp("at")),
                        rs.getObject("actor_user_id", UUID.class),
                        rs.getString("actor_name"),
                        rs.getObject("on_behalf_of_user_id", UUID.class),
                        rs.getString("on_behalf_name"),
                        AuditTargetType.valueOf(rs.getString("target_type")),
                        rs.getObject("target_id", UUID.class),
                        rs.getString("action"),
                        rs.getString("detail")))
                .list();
    }

    /**
     * The geography predicate. An {@link AuditScope.Unrestricted} caller adds none
     * (sees everything, including the all-NULL state-level rows). A
     * {@link AuditScope.Scoped} caller's predicate ORs together one IN clause per
     * non-empty set; a NULL geography column matches no IN list, so state-level
     * rows stay invisible to scoped callers without a special case.
     */
    private static void appendScope(AuditScope scope, WhereClause where) {
        if (!(scope instanceof AuditScope.Scoped scoped)) {
            return;
        }
        List<String> ors = new ArrayList<>();
        if (!scoped.kshetraIds().isEmpty()) {
            ors.add("f.kshetra_id IN (:kshetraIds)");
            where.param("kshetraIds", scoped.kshetraIds());
        }
        if (!scoped.zoneIds().isEmpty()) {
            ors.add("f.zone_id IN (:zoneIds)");
            where.param("zoneIds", scoped.zoneIds());
        }
        if (!scoped.cityIds().isEmpty()) {
            ors.add("f.city_id IN (:cityIds)");
            where.param("cityIds", scoped.cityIds());
        }
        // A non-empty Scoped is guaranteed by AuditLogAccess (empty ⇒ Denied),
        // so `ors` is never empty here.
        where.and("(" + String.join(" OR ", ors) + ")");
    }

    private static void appendFilters(AuditFilter filter, WhereClause where) {
        if (filter.targetType() != null) {
            where.and("f.target_type = :targetType", "targetType", filter.targetType().name());
        }
        if (filter.targetId() != null) {
            where.and("f.target_id = :targetId", "targetId", filter.targetId());
        }
        if (filter.actorUserId() != null) {
            where.and("f.actor_user_id = :actorUserId", "actorUserId", filter.actorUserId());
        }
        if (filter.action() != null) {
            where.and("f.action = :action", "action", filter.action());
        }
        if (filter.from() != null) {
            where.and("f.at >= :from", "from", Timestamp.from(filter.from()));
        }
        if (filter.to() != null) {
            where.and("f.at < :to", "to", Timestamp.from(filter.to()));
        }
        if (filter.proxyOnly()) {
            where.and("f.on_behalf_of_user_id IS NOT NULL");
        }
    }

    private static Instant instant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
