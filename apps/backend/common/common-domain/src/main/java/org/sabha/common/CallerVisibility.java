package org.sabha.common;

import java.util.EnumSet;
import java.util.Set;
import java.util.StringJoiner;

/**
 * The one canonical "which Sabhas can this caller see" predicate for role-scoped
 * CQRS read models (issue #66, ADR-0008). Both the re-engagement dashboard and the
 * Occurrence-reopen read adapters compose this instead of each restating the
 * role-tier {@code EXISTS} over {@code role_assignments} (plus the Nirikshak's
 * explicit Sabha assignments) and the {@code TRACK_DEMOGRAPHIC} demographic
 * derivation — those used to be duplicated SQL the two files warned each other to
 * "keep in step".
 *
 * <p>{@link #predicate} emits a boolean SQL fragment, true for a Sabha row inside the
 * caller's scope for the requested {@link VisibilityTier}s. The caller binds one
 * named parameter, {@link #USER_PARAM}, to the caller's user id. The fragment refers
 * to the Sabha, Kshetra and Zone tables through the {@link Aliases} the composing
 * query already declares; it only references the Kshetra alias when a Zone-tier is
 * granted and the Zone alias when a City-tier is granted, so a query that joins
 * only Sabha and Kshetra can still grant the Sabha- and Kshetra-level tiers.</p>
 *
 * <p>Each read model parameterises by the tiers it grants: the dashboard grants all
 * tiers; the reopen read model grants only {@code Role.REOPEN_TIERS} via {@link
 * #tiersFor}. The predicate is a pure SQL string — it carries no JDBC dependency, so
 * it lives in the domain ring next to {@link Role#REOPEN_TIERS} where every context's
 * data-access module can reach it (ADR-0019).</p>
 */
public final class CallerVisibility {

    /** The named parameter the predicate binds to the caller's user id. */
    public static final String USER_PARAM = "userId";

    private CallerVisibility() {
    }

    /** The table aliases the composing query declares for the Sabha, Kshetra and Zone rows. */
    public record Aliases(String sabha, String kshetra, String zone) {
    }

    /**
     * The {@link VisibilityTier}s a set of {@link Role}s grants in {@code
     * role_assignments}. The reopen read model passes {@link Role#REOPEN_TIERS} so a
     * Nirikshak there resolves via {@code role_assignments} ({@link
     * VisibilityTier#NIRIKSHAK}), not the proxy assignment.
     */
    public static Set<VisibilityTier> tiersFor(Set<Role> roles) {
        Set<VisibilityTier> tiers = EnumSet.noneOf(VisibilityTier.class);
        for (Role role : roles) {
            tiers.add(VisibilityTier.valueOf(role.name()));
        }
        return tiers;
    }

    /** True for a Sabha row inside the caller's scope for any of the granted {@code tiers}. */
    public static String predicate(Aliases aliases, Set<VisibilityTier> tiers) {
        StringJoiner roleClauses = new StringJoiner("\n    OR ");
        for (VisibilityTier tier : tiers) {
            if (tier != VisibilityTier.NIRIKSHAK_PROXY) {
                roleClauses.add(roleAssignmentClause(tier, aliases));
            }
        }

        StringJoiner branches = new StringJoiner("\n  OR ");
        if (roleClauses.length() > 0) {
            branches.add("""
                    EXISTS (
                      SELECT 1 FROM role_assignments ra
                      WHERE ra.user_id = :userId AND ra.revoked_at IS NULL AND (
                        %s
                      )
                    )""".formatted(roleClauses.toString()));
        }
        if (tiers.contains(VisibilityTier.NIRIKSHAK_PROXY)) {
            branches.add("""
                    EXISTS (
                      SELECT 1 FROM nirikshak_sabha_assignments na
                      WHERE na.nirikshak_user_id = :userId AND na.sabha_id = %s.id
                    )""".formatted(aliases.sabha()));
        }
        if (branches.length() == 0) {
            return "1 = 0";
        }
        return "(\n  " + branches.toString() + "\n)";
    }

    private static String roleAssignmentClause(VisibilityTier tier, Aliases a) {
        String demographic = SabhaKind.demographicSql(a.sabha());
        return switch (tier) {
            case SANCHALAK, SAH_SANCHALAK ->
                    "(ra.role = '%s' AND ra.sabha_id = %s.id)".formatted(tier, a.sabha());
            case NIRIKSHAK, NIRDESHAK, SAH_NIRDESHAK ->
                    "(ra.role = '%s' AND ra.kshetra_id = %s.kshetra_id AND ra.demographic = %s)"
                            .formatted(tier, a.sabha(), demographic);
            case SANYOJAK ->
                    "(ra.role = 'SANYOJAK' AND ra.zone_id = %s.zone_id AND ra.demographic = %s)"
                            .formatted(a.kshetra(), demographic);
            case REGIONAL_TEAM ->
                    "(ra.role = 'REGIONAL_TEAM' AND ra.city_id = %s.city_id AND ra.demographic = %s)"
                            .formatted(a.zone(), demographic);
            case MADHYASTHA_KARYALAYA -> "(ra.role = 'MADHYASTHA_KARYALAYA')";
            case NIRIKSHAK_PROXY ->
                    throw new IllegalArgumentException("NIRIKSHAK_PROXY has no role_assignments clause");
        };
    }
}
