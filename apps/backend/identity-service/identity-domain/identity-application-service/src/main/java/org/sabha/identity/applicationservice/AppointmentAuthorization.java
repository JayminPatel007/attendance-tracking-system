package org.sabha.identity.applicationservice;

import java.util.UUID;

import org.sabha.common.MadhyasthaKaryalayaLookup;
import org.sabha.common.StructuralHierarchyLookup;
import org.springframework.stereotype.Service;

/**
 * The Authorization Engine for role appointment (ADR-0011): given an appointer
 * and the scope of the role being filled, it resolves the geographic containment
 * (Sabha → Kshetra → Zone → City) and checks the appointer holds the tier above
 * at the parent scope. Pure decision component — returns a boolean, never throws
 * or mutates; {@link RoleAppointmentService} turns a {@code false} into an
 * {@link org.sabha.common.AuthorizationDeniedException}.
 */
@Service
public class AppointmentAuthorization {

    private final StructuralHierarchyLookup hierarchy;
    private final AppointerAuthorityLookup appointer;
    private final MadhyasthaKaryalayaLookup madhyasthaKaryalaya;

    public AppointmentAuthorization(
            StructuralHierarchyLookup hierarchy,
            AppointerAuthorityLookup appointer,
            MadhyasthaKaryalayaLookup madhyasthaKaryalaya) {
        this.hierarchy = hierarchy;
        this.appointer = appointer;
        this.madhyasthaKaryalaya = madhyasthaKaryalaya;
    }

    public boolean canAppoint(UUID appointerId, AppointmentScope scope) {
        return switch (scope.role()) {
            case SANCHALAK, SAH_SANCHALAK -> nirdeshakOverSabha(appointerId, scope.sabhaId());
            case NIRIKSHAK, SAH_NIRDESHAK ->
                    appointer.holdsNirdeshak(appointerId, scope.kshetraId(), scope.demographic());
            case NIRDESHAK -> sanyojakOverKshetra(appointerId, scope.kshetraId(), scope.demographic());
            case SANYOJAK -> regionalTeamOverZone(appointerId, scope.zoneId(), scope.demographic());
            case REGIONAL_TEAM, SANT -> madhyasthaKaryalaya.isMember(appointerId);
        };
    }

    private boolean nirdeshakOverSabha(UUID appointerId, UUID sabhaId) {
        return hierarchy.sabhaScope(sabhaId)
                .map(s -> appointer.holdsNirdeshak(appointerId, s.kshetraId(), s.demographic()))
                .orElse(false);
    }

    private boolean sanyojakOverKshetra(UUID appointerId, UUID kshetraId, String demographic) {
        return hierarchy.zoneOfKshetra(kshetraId)
                .map(zoneId -> appointer.holdsSanyojak(appointerId, zoneId, demographic))
                .orElse(false);
    }

    private boolean regionalTeamOverZone(UUID appointerId, UUID zoneId, String demographic) {
        return hierarchy.cityOfZone(zoneId)
                .map(cityId -> appointer.holdsRegionalTeam(appointerId, cityId, demographic))
                .orElse(false);
    }
}
