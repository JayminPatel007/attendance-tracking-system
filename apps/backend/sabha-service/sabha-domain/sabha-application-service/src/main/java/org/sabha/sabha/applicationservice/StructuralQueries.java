package org.sabha.sabha.applicationservice;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.sabha.sabha.domain.Demographic;
import org.sabha.sabha.domain.Track;

/**
 * Read-side port for the structural-admin web screens (ADR-0009). Returns flat
 * view records for listing the existing structure and for populating the create
 * forms (parent-City dropdown, the Sanyojak's own Zone). Every query reads only
 * sabha-owned tables; resolving "which Zones is this user a Sanyojak of" is the
 * identity context's {@link org.sabha.common.SanyojakZoneLookup}, which yields
 * the ids this port then hydrates via {@link #zonesByIds}.
 */
public interface StructuralQueries {

    List<CityView> listCities();

    List<ZoneView> listZones();

    List<SabhaKindView> listSabhaKinds();

    List<KshetraView> listKshetras(UUID zoneId);

    /** Hydrates the given Zone ids to views (empty in, empty out). */
    List<ZoneView> zonesByIds(Collection<UUID> ids);

    record CityView(UUID id, String name) {
    }

    record ZoneView(UUID id, String name, UUID cityId, String cityName) {
    }

    record SabhaKindView(UUID id, Demographic demographic, Track track) {
    }

    record KshetraView(UUID id, String name, UUID zoneId) {
    }
}
