package org.sabha.sabha.applicationservice;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.sabha.sabha.domain.Demographic;
import org.sabha.sabha.domain.Track;

/**
 * Read-side port for the structural-admin web screens (ADR-0009, ADR-0024).
 * Returns flat view records for listing the existing structure and for
 * populating the create forms (parent-City dropdown, the Sanyojak's own Zone,
 * the Regional Team's own City). Every query reads only sabha-owned tables;
 * resolving "which Zones is this user a Sanyojak of" is the identity context's
 * {@link org.sabha.common.SanyojakZoneLookup} and "which Cities is this user a
 * Regional Team member of" its {@link org.sabha.common.RegionalTeamCityLookup},
 * which yield the ids this port then hydrates via {@link #zonesByIds} /
 * {@link #citiesByIds}.
 */
public interface StructuralQueries {

    List<CityView> listCities();

    List<ZoneView> listZones();

    List<SabhaKindView> listSabhaKinds();

    List<KshetraView> listKshetras(UUID zoneId);

    /** Hydrates the given Zone ids to views (empty in, empty out). */
    List<ZoneView> zonesByIds(Collection<UUID> ids);

    /** Hydrates the given City ids to views (empty in, empty out). */
    List<CityView> citiesByIds(Collection<UUID> ids);

    record CityView(UUID id, String name) {
    }

    record ZoneView(UUID id, String name, UUID cityId, String cityName) {
    }

    /** {@code retiredAt} is null while the kind is active; set once soft-retired (ADR-0026). */
    record SabhaKindView(UUID id, Demographic demographic, Track track, Instant retiredAt) {
    }

    record KshetraView(UUID id, String name, UUID zoneId) {
    }
}
