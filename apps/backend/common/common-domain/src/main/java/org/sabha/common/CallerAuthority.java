package org.sabha.common;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The signed-in caller, and <b>exactly their rows in {@code role_assignments} —
 * nothing else</b>. Resolved once at the request edge alongside the {@code
 * users} row (ADR-0030, ADR-0032) and passed to every engine below it as a
 * parameter, so a request answers "what may this caller do?" with one query
 * regardless of how many authority questions it asks.
 *
 * <h2>The stopping rule</h2>
 *
 * <p>That first sentence is a boundary, not a description. One relation, one
 * key, one query: a candidate fact either is a projection of {@code SELECT *
 * FROM role_assignments WHERE user_id = ? AND revoked_at IS NULL} or it is not,
 * and a reviewer applies that without judgement. ADR-0027 rightly rejected a
 * shared <em>granted-scope</em> module; this type is not one, and stays not one
 * only while the rule holds. The danger is never the first fold — it is the
 * tenth, by which point every caller-adjacent fact has accreted here.</p>
 *
 * <p>The rule already costs something, and that is the point: {@code
 * WebSessionService.describe} re-fetches the caller's {@code users} row for
 * {@code username()} rather than carry it here. A rule that has never refused
 * anything has not been tested. If a caller-keyed question ever arrives that is
 * <em>not</em> a {@code role_assignments} projection and folding it is obviously
 * right, the rule is wrong and must be <b>re-drawn explicitly, not stretched
 * silently</b> (ADR-0032, "What would change our mind").</p>
 *
 * <h2>Why the rows are private</h2>
 *
 * <p>{@code role_assignments.role} holds nine distinct wire values and four
 * overlapping enums partition it three different ways, none covering it (issue
 * #232). Exposing {@link RoleAssignment} rows would make that untyped string a
 * precondition of four bounded contexts. Held privately and surfaced only as
 * named questions, the taxonomy problem stays sealed inside this class — and
 * re-deriving a predicate from raw rows becomes a <em>compile</em> error, which
 * is strictly stronger than the ArchUnit rule ADR-0032 considered and rejected
 * as inexpressible.</p>
 *
 * <h2>The structural tier table</h2>
 *
 * <p>Create and delete share one authority model: the holder of scope at a tier
 * may both create and delete the children one tier below it, by current scope
 * rather than {@code created_by} (ADR-0009, ADR-0024, ADR-0026). This table was
 * the javadoc of {@code StructuralScopeAuthority}, the only place in the backend
 * where the two were stated together; the engine was deleted for holding no
 * policy, so the table relocated here rather than being lost with it.</p>
 *
 * <ul>
 *   <li><b>State scope</b> — {@link #isMadhyasthaKaryalaya()} — owns Cities and
 *       Sabha Kinds.</li>
 *   <li><b>City scope</b> — {@link #isRegionalTeamMemberOfCity(UUID)}, ADR-0024 —
 *       owns its Zones.</li>
 *   <li><b>Zone scope</b> — {@link #isSanyojakOfZone(UUID)} — owns its
 *       Kshetras.</li>
 *   <li><b>Kshetra scope</b> — {@link #holdsNirdeshakIn(UUID, String)} — owns its
 *       Sabhas. Sabha <em>creation</em> is authorized in the identity context (it
 *       orchestrates the Sanchalak appointment, ADR-0012) and deletion in the
 *       sabha context; both now read the same method, so the tiers cannot
 *       drift.</li>
 * </ul>
 *
 * <h2>Reading the method pairs</h2>
 *
 * <p>{@link #sanyojakZones()} sits beside {@link #holdsSanyojakIn(UUID, String)};
 * {@link #regionalTeamCities()} beside {@link #holdsRegionalTeamIn(UUID,
 * String)}; {@link #nirdeshakScopes()} beside {@link #kshetrasUnderOversight()}.
 * Each pair is the same role read two ways, <b>deliberately</b> — ADR-0024
 * collapses the demographic away for geography, ADR-0023 drops it for the audit
 * read, and appointment authority (ADR-0011) keeps it. Side by side the
 * asymmetry is stated; in four different modules it was merely emergent.</p>
 *
 * <p>If a future question needs the demographic <em>sometimes</em>, <b>split the
 * method — do not add the branch.</b> A method whose body omits a filter term is
 * a rule written down; a branch inside one method is the fat interface ADR-0027
 * §1 warned about.</p>
 */
public final class CallerAuthority {

    private static final String NIRDESHAK = "NIRDESHAK";
    private static final String SAH_NIRDESHAK = "SAH_NIRDESHAK";
    private static final String SANYOJAK = "SANYOJAK";
    private static final String REGIONAL_TEAM = "REGIONAL_TEAM";

    private final UserId userId;
    private final List<RoleAssignment> assignments;

    public CallerAuthority(UserId userId, List<RoleAssignment> assignments) {
        this.userId = Objects.requireNonNull(userId, "CallerAuthority userId must not be null");
        this.assignments = List.copyOf(
                Objects.requireNonNull(assignments, "CallerAuthority assignments must not be null"));
    }

    /** A caller holding no role at all — the common case for a rank-and-file Karyakar. */
    public static CallerAuthority withNoRoles(UserId userId) {
        return new CallerAuthority(userId, List.of());
    }

    /** The local {@code users.id} this authority belongs to (ADR-0030). */
    public UserId userId() {
        return userId;
    }

    // --- State-level oversight (ADR-0005): null-scope rows, outside the Role enum ---

    /** Madhyastha Karyalaya membership — State-level oversight, null operational scope. */
    public boolean isMadhyasthaKaryalaya() {
        return holdsRole(OversightRole.MADHYASTHA_KARYALAYA.wireValue());
    }

    /**
     * Sant oversight. Answered here only for the <em>caller</em>; the four
     * caller-keyed sites that used to ask {@code SantLookup} read this instead.
     * {@code SantLookup} survives for the one site that asks about a
     * <em>target</em> — a Sant, having no appointer (ADR-0011), is reissued by an
     * MK member (ADR-0004).
     */
    public boolean isSant() {
        return holdsRole(OversightRole.SANT.wireValue());
    }

    // --- Operational roles (ADR-0005) ---

    /**
     * Every operational {@link Role} the caller holds, across all scopes — what
     * the web shell's section visibility reads, which is not Sabha-scoped.
     * Non-operational rows (Sant, MK) are not {@link Role}s and are skipped
     * rather than failing the read.
     */
    public Set<Role> operationalRoles() {
        return rolesMatching(row -> true);
    }

    /** The operational roles the caller holds on one Sabha. */
    public Set<Role> rolesOnSabha(UUID sabhaId) {
        return rolesMatching(row -> sabhaId.equals(row.sabhaId()));
    }

    /**
     * The operational roles the caller holds for a {@code (Kshetra,
     * demographic)}. The Kshetra-tier reopen authorities (Nirikshak, Nirdeshak,
     * Sah-Nirdeshak — ADR-0001) are stored against the Kshetra and demographic
     * rather than a single Sabha (ADR-0011), so the attendance engine resolves a
     * Sabha to its scope and asks this.
     */
    public Set<Role> rolesOnKshetra(UUID kshetraId, String demographic) {
        return rolesMatching(row -> kshetraId.equals(row.kshetraId())
                && demographic.equals(row.demographic()));
    }

    /**
     * Whether the caller runs the given Sabha — its Sanchalak or Sah-Sanchalak.
     * The one predicate behind both the home-Sabha transfer initiation and the
     * selection nomination, which held byte-identical copies of it before
     * ADR-0032 folded the rows to a parameter.
     *
     * <p>Not the same set as Sabha <em>shaping</em> authority, which excludes the
     * Sah-Sanchalak and admits an assigned Nirikshak proxy (ADR-0001) — that one
     * is policy and stays in the attendance Authorization Engine.</p>
     */
    public boolean runsSabha(UUID sabhaId) {
        Set<Role> roles = rolesOnSabha(sabhaId);
        return roles.contains(Role.SANCHALAK) || roles.contains(Role.SAH_SANCHALAK);
    }

    // --- Appointing tiers (ADR-0011): scope column plus demographic ---

    /**
     * Whether the caller holds Nirdeshak over a {@code (Kshetra, demographic)}.
     * One method, four call sites: appointment authority (ADR-0011), Sabha
     * definition (ADR-0012), Sabha deletion (ADR-0026) and the selection decision
     * (ADR-0006). Those four used to reach two different ports and the agreement
     * between them was an asserted invariant; now there is nothing to assert.
     */
    public boolean holdsNirdeshakIn(UUID kshetraId, String demographic) {
        return rolesOnKshetra(kshetraId, demographic).contains(Role.NIRDESHAK);
    }

    /** Whether the caller holds Sanyojak over a {@code (Zone, demographic)} (ADR-0011). */
    public boolean holdsSanyojakIn(UUID zoneId, String demographic) {
        return assignments.stream().anyMatch(row -> SANYOJAK.equals(row.role())
                && zoneId.equals(row.zoneId())
                && demographic.equals(row.demographic()));
    }

    /** Whether the caller holds a Regional Team role in a {@code (City, demographic)} (ADR-0011). */
    public boolean holdsRegionalTeamIn(UUID cityId, String demographic) {
        return assignments.stream().anyMatch(row -> REGIONAL_TEAM.equals(row.role())
                && cityId.equals(row.cityId())
                && demographic.equals(row.demographic()));
    }

    // --- Geographic ownership: the same rows with the demographic dropped ---

    /** The {@code (Kshetra, demographic)} scopes the caller is a Nirdeshak of (ADR-0026). */
    public List<NirdeshakScope> nirdeshakScopes() {
        return assignments.stream()
                .filter(row -> NIRDESHAK.equals(row.role())
                        && row.kshetraId() != null && row.demographic() != null)
                .map(row -> new NirdeshakScope(row.kshetraId(), row.demographic()))
                .toList();
    }

    /**
     * The Kshetras the caller oversees for the audit read — Nirdeshak <em>and</em>
     * Sah-Nirdeshak, demographic dropped. ADR-0023 makes the oversight read
     * deliberately broader than the caller's write authority, which is why this
     * is not {@link #nirdeshakScopes()}.
     */
    public Set<UUID> kshetrasUnderOversight() {
        return distinctScopes(row -> (NIRDESHAK.equals(row.role()) || SAH_NIRDESHAK.equals(row.role()))
                && row.kshetraId() != null, RoleAssignment::kshetraId);
    }

    /** The Zones the caller is a Sanyojak of (ADR-0009). */
    public List<UUID> sanyojakZones() {
        return List.copyOf(distinctScopes(
                row -> SANYOJAK.equals(row.role()) && row.zoneId() != null, RoleAssignment::zoneId));
    }

    /** Whether the caller is a Sanyojak of the given Zone — Kshetra-creation authority (ADR-0009). */
    public boolean isSanyojakOfZone(UUID zoneId) {
        return assignments.stream()
                .anyMatch(row -> SANYOJAK.equals(row.role()) && zoneId.equals(row.zoneId()));
    }

    /**
     * The Cities the caller holds a Regional Team role in. Regional Team
     * membership is recorded per {@code (City, demographic)}, but a Zone is
     * geography and therefore demographic-agnostic (ADR-0024), so this collapses
     * the demographic away and answers at City granularity.
     */
    public List<UUID> regionalTeamCities() {
        return List.copyOf(distinctScopes(
                row -> REGIONAL_TEAM.equals(row.role()) && row.cityId() != null, RoleAssignment::cityId));
    }

    /** Whether the caller is a Regional Team member of the City — Zone-creation authority (ADR-0024). */
    public boolean isRegionalTeamMemberOfCity(UUID cityId) {
        return assignments.stream()
                .anyMatch(row -> REGIONAL_TEAM.equals(row.role()) && cityId.equals(row.cityId()));
    }

    // --- The rows never leave ---

    private boolean holdsRole(String wireValue) {
        return assignments.stream().anyMatch(row -> wireValue.equals(row.role()));
    }

    private Set<Role> rolesMatching(java.util.function.Predicate<RoleAssignment> where) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        assignments.stream().filter(where).forEach(row -> toRole(row.role()).ifPresent(roles::add));
        return roles;
    }

    private Set<UUID> distinctScopes(java.util.function.Predicate<RoleAssignment> where,
                                     java.util.function.Function<RoleAssignment, UUID> column) {
        return new LinkedHashSet<>(assignments.stream().filter(where).map(column).toList());
    }

    /**
     * The tolerant parse the two surviving adapters already did: a {@code role}
     * string outside the operational {@link Role} enum (Sant, MK, Regional Team)
     * is skipped, not an error. This is the only place it is written now.
     */
    private static Optional<Role> toRole(String name) {
        try {
            return Optional.of(Role.valueOf(name));
        } catch (IllegalArgumentException notOperational) {
            return Optional.empty();
        }
    }
}
