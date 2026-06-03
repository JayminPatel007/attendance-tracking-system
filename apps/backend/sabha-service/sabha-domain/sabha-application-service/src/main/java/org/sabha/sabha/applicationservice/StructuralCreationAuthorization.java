package org.sabha.sabha.applicationservice;

import java.util.UUID;

import org.sabha.common.MadhyasthaKaryalayaLookup;
import org.sabha.common.SanyojakZoneLookup;
import org.springframework.stereotype.Service;

/**
 * The Authorization Engine for structural creation (ADR-0009): it decides who
 * may create each tier of the org structure. Authority lives at the tier above —
 *
 * <ul>
 *   <li><b>State structure</b> — Cities, Zones, and Sabha Kinds — is created by
 *       Madhyastha Karyalaya members, via {@link MadhyasthaKaryalayaLookup}.</li>
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

    public StructuralCreationAuthorization(
            MadhyasthaKaryalayaLookup madhyasthaKaryalaya,
            SanyojakZoneLookup sanyojakZones) {
        this.madhyasthaKaryalaya = madhyasthaKaryalaya;
        this.sanyojakZones = sanyojakZones;
    }

    /** Cities, Zones, and Sabha Kinds are all State-level MK authority. */
    public boolean canCreateStateStructure(UUID userId) {
        return madhyasthaKaryalaya.isMember(userId);
    }

    public boolean canCreateKshetra(UUID userId, UUID zoneId) {
        return sanyojakZones.isSanyojakOfZone(userId, zoneId);
    }
}
