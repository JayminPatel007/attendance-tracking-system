package org.sabha.sabha.applicationservice;

import java.util.UUID;

import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.AuthorizedAction;
import org.sabha.sabha.domain.City;
import org.sabha.sabha.domain.CityNotFoundException;
import org.sabha.sabha.domain.Demographic;
import org.sabha.sabha.domain.Kshetra;
import org.sabha.sabha.domain.SabhaKind;
import org.sabha.sabha.domain.SabhaKindAlreadyRegisteredException;
import org.sabha.sabha.domain.Track;
import org.sabha.sabha.domain.Zone;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates structural creation (ADR-0009): authorize the caller via the
 * {@link StructuralCreationAuthorization} engine, build the aggregate (which
 * enforces its own invariants and stamps {@code createdBy} with the caller),
 * then persist. A denied decision becomes an
 * {@link AuthorizationDeniedException} (HTTP 403); domain-rule violations bubble
 * up from the aggregates.
 */
@Service
public class StructuralCreationService {

    private final StructuralCreationAuthorization authz;
    private final CityRepository cities;
    private final ZoneRepository zones;
    private final KshetraRepository kshetras;
    private final SabhaKindRepository sabhaKinds;

    public StructuralCreationService(
            StructuralCreationAuthorization authz,
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
    public UUID createCity(UUID caller, String name) {
        if (!authz.canCreateStateStructure(caller)) {
            throw new AuthorizationDeniedException(caller, AuthorizedAction.CREATE_CITY);
        }
        City city = City.create(name, caller);
        cities.save(city);
        return city.id();
    }

    @Transactional
    public UUID createZone(UUID caller, UUID cityId, String name) {
        if (!authz.canCreateStateStructure(caller)) {
            throw new AuthorizationDeniedException(caller, AuthorizedAction.CREATE_ZONE);
        }
        if (!cities.existsById(cityId)) {
            throw new CityNotFoundException(cityId);
        }
        Zone zone = Zone.create(cityId, name, caller);
        zones.save(zone);
        return zone.id();
    }

    @Transactional
    public UUID createSabhaKind(UUID caller, Demographic demographic, Track track) {
        if (!authz.canCreateStateStructure(caller)) {
            throw new AuthorizationDeniedException(caller, AuthorizedAction.CREATE_SABHA_KIND);
        }
        if (sabhaKinds.exists(demographic, track)) {
            throw new SabhaKindAlreadyRegisteredException(demographic, track);
        }
        SabhaKind kind = SabhaKind.register(demographic, track, caller);
        sabhaKinds.save(kind);
        return kind.id();
    }

    @Transactional
    public UUID createKshetra(UUID caller, UUID zoneId, String name) {
        if (!authz.canCreateKshetra(caller, zoneId)) {
            throw new AuthorizationDeniedException(caller, AuthorizedAction.CREATE_KSHETRA);
        }
        Kshetra kshetra = Kshetra.create(zoneId, name, caller);
        kshetras.save(kshetra);
        return kshetra.id();
    }
}
