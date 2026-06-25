package org.sabha.sabha.applicationservice;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.sabha.common.NirdeshakScopeLookup.NirdeshakScope;
import org.sabha.sabha.domain.Demographic;
import org.sabha.sabha.domain.Track;

/**
 * Read-side port for the structural-admin and Sabha-definition web screens
 * (ADR-0009, ADR-0024, ADR-0026). Returns flat view records for listing the
 * existing structure (each carrying its live child count for the delete guard),
 * for populating the create forms (parent-City dropdown, the Sanyojak's own Zone,
 * the Regional Team's own City), and for the Nirdeshak's deletable Sabhas. Every
 * query reads only sabha-owned tables; resolving "which Zones is this user a
 * Sanyojak of" is the identity context's {@link org.sabha.common.SanyojakZoneLookup},
 * "which Cities is this user a Regional Team member of" its
 * {@link org.sabha.common.RegionalTeamCityLookup}, and "which (Kshetra,demographic)
 * scopes does this user direct" its {@link org.sabha.common.NirdeshakScopeLookup},
 * which yield the ids/scopes this port then hydrates via {@link #zonesByIds} /
 * {@link #citiesByIds} / {@link #sabhasOwnedBy}.
 */
public interface StructuralQueries {

    List<CityView> listCities();

    List<ZoneView> listZones();

    List<SabhaKindView> listSabhaKinds();

    List<KshetraView> listKshetras(UUID zoneId);

    /**
     * The Sabhas sitting in the given Nirdeshak scopes, each with its recorded
     * Occurrence count so the web can disable delete on a non-empty Sabha
     * (ADR-0026). Empty in, empty out.
     */
    List<SabhaView> sabhasOwnedBy(Collection<NirdeshakScope> scopes);

    /** Hydrates the given Zone ids to views (empty in, empty out). */
    List<ZoneView> zonesByIds(Collection<UUID> ids);

    /** Hydrates the given City ids to views (empty in, empty out). */
    List<CityView> citiesByIds(Collection<UUID> ids);

    /** {@code zoneCount} is the live child Zones, so the web can disable delete when non-empty (ADR-0026). */
    record CityView(UUID id, String name, int zoneCount) {
    }

    /** {@code kshetraCount} is the live child Kshetras, gating delete in the web (ADR-0026). */
    record ZoneView(UUID id, String name, UUID cityId, String cityName, int kshetraCount) {
    }

    /** {@code retiredAt} is null while the kind is active; set once soft-retired (ADR-0026). */
    record SabhaKindView(UUID id, Demographic demographic, Track track, Instant retiredAt) {
    }

    /** {@code sabhaCount} is the live child Sabhas, gating delete in the web (ADR-0026). */
    record KshetraView(UUID id, String name, UUID zoneId, int sabhaCount) {
    }

    /**
     * A Sabha in the Nirdeshak's deletion list. {@code occurrenceCount} is the
     * recorded Occurrences beneath it; the web disables delete while it is non-zero
     * (ADR-0026 block-if-non-empty).
     */
    record SabhaView(
            UUID id,
            UUID kshetraId,
            String kshetraName,
            Demographic demographic,
            Track track,
            String standingVenue,
            int occurrenceCount) {
    }
}
