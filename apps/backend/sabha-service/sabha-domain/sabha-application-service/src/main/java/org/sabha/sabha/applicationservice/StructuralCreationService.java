package org.sabha.sabha.applicationservice;

import java.util.UUID;

import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.AuthorizedAction;
import org.sabha.common.CityNotFoundException;
import org.sabha.common.UserId;
import org.sabha.sabha.domain.City;
import org.sabha.sabha.domain.Demographic;
import org.sabha.sabha.domain.Kshetra;
import org.sabha.sabha.domain.SabhaKind;
import org.sabha.sabha.domain.SabhaKindAlreadyRegisteredException;
import org.sabha.sabha.domain.Track;
import org.sabha.sabha.domain.Zone;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates structural creation (ADR-0009, ADR-0024): authorize the caller via the
 * {@link StructuralScopeAuthority} engine, build the aggregate (which
 * enforces its own invariants and stamps {@code createdBy} with the caller),
 * then persist. A denied decision becomes an
 * {@link AuthorizationDeniedException} (HTTP 403); domain-rule violations bubble
 * up from the aggregates.
 */
@Service
public class StructuralCreationService {

    private final StructuralScopeAuthority authz;
    private final CityRepository cities;
    private final ZoneRepository zones;
    private final KshetraRepository kshetras;
    private final SabhaKindRepository sabhaKinds;

    public StructuralCreationService(
            StructuralScopeAuthority authz,
            CityRepository cities,
            ZoneRepository zones,
            KshetraRepository kshetras,
            SabhaKindRepository sabhaKinds) {
        this.authz = authz;
        this.cities = cities;
        this.zones = zones;
        this.kshetras = kshetras;
        this.sabhaKinds = sabhaKinds;
    }

    @Transactional
    public UUID createCity(UserId caller, String name) {
        UUID callerId = caller.value();
        if (!authz.holdsStateScope(callerId)) {
            throw new AuthorizationDeniedException(callerId, AuthorizedAction.CREATE_CITY);
        }
        City city = City.create(name, callerId);
        cities.save(city);
        return city.id();
    }

    @Transactional
    public UUID createZone(UserId caller, UUID cityId, String name) {
        UUID callerId = caller.value();
        if (!authz.holdsCityScope(callerId, cityId)) {
            throw new AuthorizationDeniedException(callerId, AuthorizedAction.CREATE_ZONE);
        }
        if (!cities.existsById(cityId)) {
            throw new CityNotFoundException(cityId);
        }
        Zone zone = Zone.create(cityId, name, callerId);
        zones.save(zone);
        return zone.id();
    }

    @Transactional
    public UUID createSabhaKind(UserId caller, Demographic demographic, Track track) {
        UUID callerId = caller.value();
        if (!authz.holdsStateScope(callerId)) {
            throw new AuthorizationDeniedException(callerId, AuthorizedAction.CREATE_SABHA_KIND);
        }
        if (sabhaKinds.exists(demographic, track)) {
            throw new SabhaKindAlreadyRegisteredException(demographic, track);
        }
        SabhaKind kind = SabhaKind.register(demographic, track, callerId);
        sabhaKinds.save(kind);
        return kind.id();
    }

    @Transactional
    public UUID createKshetra(UserId caller, UUID zoneId, String name) {
        UUID callerId = caller.value();
        if (!authz.holdsZoneScope(callerId, zoneId)) {
            throw new AuthorizationDeniedException(callerId, AuthorizedAction.CREATE_KSHETRA);
        }
        Kshetra kshetra = Kshetra.create(zoneId, name, callerId);
        kshetras.save(kshetra);
        return kshetra.id();
    }
}
