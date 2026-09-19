package org.sabha.sabha.applicationservice;

import java.util.UUID;

import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.AuthorizedAction;
import org.sabha.common.CityNotFoundException;
import org.sabha.common.SabhaNotFoundException;
import org.sabha.common.SabhaScope;
import org.sabha.common.SabhaFacts;
import org.sabha.common.UserId;
import org.sabha.sabha.domain.KshetraNotFoundException;
import org.sabha.sabha.domain.StructuralNotEmptyException;
import org.sabha.sabha.domain.ZoneNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates structural deletion (ADR-0026): authorize the caller via the
 * {@link StructuralScopeAuthority} engine (by current scope, not creator),
 * then enforce <b>block-if-non-empty</b> — a delete is allowed only when the
 * entity has no live children and no recorded Occurrences beneath it. There is no
 * cascade; attendance history is never destroyed. A denial becomes an
 * {@link AuthorizationDeniedException} (HTTP 403); a non-empty entity a
 * {@link StructuralNotEmptyException} (HTTP 409) carrying the human-readable
 * blocking reason; an unknown id a 404.
 */
@Service
public class StructuralDeletionService {

    private final StructuralScopeAuthority authz;
    private final CityRepository cities;
    private final ZoneRepository zones;
    private final KshetraRepository kshetras;
    private final SabhaRepository sabhas;
    private final SabhaFacts sabhaFacts;

    public StructuralDeletionService(
            StructuralScopeAuthority authz, CityRepository cities,
            ZoneRepository zones, KshetraRepository kshetras,
            SabhaRepository sabhas, SabhaFacts sabhaFacts) {
        this.authz = authz;
        this.cities = cities;
        this.zones = zones;
        this.kshetras = kshetras;
        this.sabhas = sabhas;
        this.sabhaFacts = sabhaFacts;
    }

    @Transactional
    public void deleteCity(UserId caller, UUID cityId) {
        if (!cities.existsById(cityId)) {
            throw new CityNotFoundException(cityId);
        }
        if (!authz.holdsStateScope(caller.value())) {
            throw new AuthorizationDeniedException(caller.value(), AuthorizedAction.DELETE_CITY);
        }
        int childZones = cities.zoneCount(cityId);
        if (childZones > 0) {
            throw StructuralNotEmptyException.cityHasZones(childZones);
        }
        cities.deleteById(cityId);
    }

    @Transactional
    public void deleteZone(UserId caller, UUID zoneId) {
        UUID cityId = zones.cityIdOf(zoneId).orElseThrow(() -> new ZoneNotFoundException(zoneId));
        if (!authz.holdsCityScope(caller.value(), cityId)) {
            throw new AuthorizationDeniedException(caller.value(), AuthorizedAction.DELETE_ZONE);
        }
        int childKshetras = zones.kshetraCount(zoneId);
        if (childKshetras > 0) {
            throw StructuralNotEmptyException.zoneHasKshetras(childKshetras);
        }
        zones.deleteById(zoneId);
    }

    @Transactional
    public void deleteKshetra(UserId caller, UUID kshetraId) {
        UUID zoneId = kshetras.zoneIdOf(kshetraId).orElseThrow(() -> new KshetraNotFoundException(kshetraId));
        if (!authz.holdsZoneScope(caller.value(), zoneId)) {
            throw new AuthorizationDeniedException(caller.value(), AuthorizedAction.DELETE_KSHETRA);
        }
        int childSabhas = kshetras.sabhaCount(kshetraId);
        if (childSabhas > 0) {
            throw StructuralNotEmptyException.kshetraHasSabhas(childSabhas);
        }
        kshetras.deleteById(kshetraId);
    }

    @Transactional
    public void deleteSabha(UserId caller, UUID sabhaId) {
        SabhaScope scope = sabhaFacts.of(sabhaId)
                .orElseThrow(() -> new SabhaNotFoundException(sabhaId))
                .scope();
        if (!authz.holdsKshetraScope(caller.value(), scope.kshetraId(), scope.demographic())) {
            throw new AuthorizationDeniedException(caller.value(), AuthorizedAction.DELETE_SABHA);
        }
        int occurrences = sabhas.occurrenceCount(sabhaId);
        if (occurrences > 0) {
            throw StructuralNotEmptyException.sabhaHasOccurrences(occurrences);
        }
        sabhas.deleteById(sabhaId);
    }
}
