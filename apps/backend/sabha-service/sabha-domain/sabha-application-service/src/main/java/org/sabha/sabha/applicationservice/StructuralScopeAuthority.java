package org.sabha.sabha.applicationservice;

import java.util.UUID;

import org.sabha.common.MadhyasthaKaryalayaLookup;
import org.sabha.common.RegionalTeamCityLookup;
import org.sabha.common.Role;
import org.sabha.common.RoleAssignmentLookup;
import org.sabha.common.SanyojakZoneLookup;
import org.springframework.stereotype.Service;

/**
 * The Authorization Engine for the structural hierarchy (ADR-0009, ADR-0024,
 * ADR-0026). Create and delete share one authority model: the holder of scope at
 * a tier may both create and delete the children one tier below it, by current
 * scope rather than {@code created_by}. So this one engine answers both
 * questions, and a tier's authority can never drift between the two paths.
 *
 * <ul>
 *   <li><b>State scope</b> — a Madhyastha Karyalaya member ({@link
 *       MadhyasthaKaryalayaLookup}) — owns Cities and Sabha Kinds.</li>
 *   <li><b>City scope</b> — a Regional Team member of that City ({@link
 *       RegionalTeamCityLookup}, ADR-0024) — owns its Zones.</li>
 *   <li><b>Zone scope</b> — the Sanyojak of that Zone ({@link
 *       SanyojakZoneLookup}) — owns its Kshetras.</li>
 *   <li><b>Kshetra scope</b> — the Nirdeshak over that {@code (Kshetra,
 *       demographic)} ({@link RoleAssignmentLookup#rolesForUserOnKshetra}) — owns
 *       its Sabhas. Sabha <em>creation</em> is authorized in the identity context
 *       (it orchestrates the Sanchalak appointment, ADR-0012); this engine carries
 *       the same Nirdeshak predicate for the sabha-context delete path.</li>
 * </ul>
 *
 * <p>Pure decision component: it returns booleans and never throws or mutates;
 * the creation and deletion application services turn a {@code false} into an
 * {@link org.sabha.common.AuthorizationDeniedException}.</p>
 */
@Service
public class StructuralScopeAuthority {

    private final MadhyasthaKaryalayaLookup madhyasthaKaryalaya;
    private final SanyojakZoneLookup sanyojakZones;
    private final RegionalTeamCityLookup regionalTeamCities;
    private final RoleAssignmentLookup roleAssignments;

    public StructuralScopeAuthority(
            MadhyasthaKaryalayaLookup madhyasthaKaryalaya,
            SanyojakZoneLookup sanyojakZones,
            RegionalTeamCityLookup regionalTeamCities,
            RoleAssignmentLookup roleAssignments) {
        this.madhyasthaKaryalaya = madhyasthaKaryalaya;
        this.sanyojakZones = sanyojakZones;
        this.regionalTeamCities = regionalTeamCities;
        this.roleAssignments = roleAssignments;
    }

    /** State scope — Madhyastha Karyalaya — owns Cities and Sabha Kinds. */
    public boolean holdsStateScope(UUID userId) {
        return madhyasthaKaryalaya.isMember(userId);
    }

    /** City scope — any Regional Team member of the City — owns its Zones (ADR-0024). */
    public boolean holdsCityScope(UUID userId, UUID cityId) {
        return regionalTeamCities.isRegionalTeamMemberOfCity(userId, cityId);
    }

    /** Zone scope — the Zone's Sanyojak — owns its Kshetras. */
    public boolean holdsZoneScope(UUID userId, UUID zoneId) {
        return sanyojakZones.isSanyojakOfZone(userId, zoneId);
    }

    /** Kshetra scope — the Nirdeshak over {@code (Kshetra, demographic)} — owns its Sabhas. */
    public boolean holdsKshetraScope(UUID userId, UUID kshetraId, String demographic) {
        return roleAssignments.rolesForUserOnKshetra(userId, kshetraId, demographic).contains(Role.NIRDESHAK);
    }
}
