package org.sabha.sabha.applicationservice;

import java.util.UUID;

import org.sabha.common.MadhyasthaKaryalayaLookup;
import org.sabha.common.RegionalTeamCityLookup;
import org.sabha.common.SanyojakZoneLookup;
import org.springframework.stereotype.Service;

/**
 * The Authorization Engine for structural creation (ADR-0009, ADR-0024): it
 * decides who may create each tier of the org structure. Authority lives at the
 * tier above —
 *
 * <ul>
 *   <li><b>State structure</b> — Cities and Sabha Kinds — is created by
 *       Madhyastha Karyalaya members, via {@link MadhyasthaKaryalayaLookup}.</li>
 *   <li><b>Zone</b> — a Regional Team member of the target City, via
 *       {@link RegionalTeamCityLookup}. Zone creation moved down from the
 *       Madhyastha Karyalaya to the Regional Team (ADR-0024, superseding the
 *       Zone row of ADR-0009); MK has no Zone-creation path at all.</li>
 *   <li><b>Kshetra</b> — the Sanyojak of the target Zone, via
 *       {@link SanyojakZoneLookup}.</li>
 * </ul>
 *
 * <p>Pure decision component: it returns booleans and never throws or mutates;
 * the application service turns a {@code false} into an
 * {@link org.sabha.common.AuthorizationDeniedException}.</p>
 */
@Service
public class StructuralCreationAuthorization {

    private final MadhyasthaKaryalayaLookup madhyasthaKaryalaya;
    private final SanyojakZoneLookup sanyojakZones;
    private final RegionalTeamCityLookup regionalTeamCities;

    public StructuralCreationAuthorization(
            MadhyasthaKaryalayaLookup madhyasthaKaryalaya,
            SanyojakZoneLookup sanyojakZones,
            RegionalTeamCityLookup regionalTeamCities) {
        this.madhyasthaKaryalaya = madhyasthaKaryalaya;
        this.sanyojakZones = sanyojakZones;
        this.regionalTeamCities = regionalTeamCities;
    }

    /** Cities and Sabha Kinds are State-level MK authority (Zones moved to the Regional Team). */
    public boolean canCreateStateStructure(UUID userId) {
        return madhyasthaKaryalaya.isMember(userId);
    }

    /** A Zone is created by any Regional Team member of its City (ADR-0024). */
    public boolean canCreateZone(UUID userId, UUID cityId) {
        return regionalTeamCities.isRegionalTeamMemberOfCity(userId, cityId);
    }

    public boolean canCreateKshetra(UUID userId, UUID zoneId) {
        return sanyojakZones.isSanyojakOfZone(userId, zoneId);
    }
}
