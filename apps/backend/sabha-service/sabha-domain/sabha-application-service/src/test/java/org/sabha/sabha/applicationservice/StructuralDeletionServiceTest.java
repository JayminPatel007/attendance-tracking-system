package org.sabha.sabha.applicationservice;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.Role;
import org.sabha.common.SabhaScope;
import org.sabha.sabha.domain.City;
import org.sabha.sabha.domain.CityNotFoundException;
import org.sabha.sabha.domain.Kshetra;
import org.sabha.sabha.domain.KshetraNotFoundException;
import org.sabha.sabha.domain.SabhaNotFoundException;
import org.sabha.sabha.domain.StructuralNotEmptyException;
import org.sabha.sabha.domain.Zone;
import org.sabha.sabha.domain.ZoneNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StructuralDeletionServiceTest {

    private static final UUID MK = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID REGIONAL_TEAM = UUID.fromString("00000000-0000-0000-0000-0000000000dd");
    private static final UUID SANYOJAK = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
    private static final UUID NIRDESHAK = UUID.fromString("00000000-0000-0000-0000-0000000000cc");
    private static final UUID SOMEONE_ELSE = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
    private static final String YUVAK = "YUVAK";

    private final FakeCities cities = new FakeCities();
    private final FakeZones zones = new FakeZones();
    private final FakeKshetras kshetras = new FakeKshetras();
    private final FakeSabhas sabhas = new FakeSabhas();
    private final FakeHierarchy hierarchy = new FakeHierarchy();
    private final FakeRoleAssignments roleAssignments = new FakeRoleAssignments();

    /** Scopes the test caller holds — populated per test. */
    private final java.util.Map<UUID, List<UUID>> sanyojakZones = new java.util.HashMap<>();
    private final java.util.Map<UUID, List<UUID>> regionalTeamCities = new java.util.HashMap<>();

    private final StructuralDeletionService service = new StructuralDeletionService(
            new StructuralScopeAuthority(
                    userId -> userId.equals(MK),
                    userId -> sanyojakZones.getOrDefault(userId, List.of()),
                    userId -> regionalTeamCities.getOrDefault(userId, List.of()),
                    roleAssignments),
            cities, zones, kshetras, sabhas, hierarchy);

    @Test
    void mkDeletesAnEmptyCity() {
        UUID cityId = cities.seed("Surat");

        service.deleteCity(MK, cityId);

        assertThat(cities.existsById(cityId)).isFalse();
    }

    @Test
    void deletingACityWithZonesIsBlockedWithItsChildCountAndNothingIsRemoved() {
        UUID cityId = cities.seed("Mumbai");
        cities.setZoneCount(cityId, 6);

        assertThatThrownBy(() -> service.deleteCity(MK, cityId))
                .isInstanceOf(StructuralNotEmptyException.class)
                .hasMessage("has 6 Zones");
        assertThat(cities.existsById(cityId)).isTrue();
    }

    @Test
    void aNonMkUserCannotDeleteACityAndNothingIsRemoved() {
        UUID cityId = cities.seed("Surat");

        assertThatThrownBy(() -> service.deleteCity(SOMEONE_ELSE, cityId))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(cities.existsById(cityId)).isTrue();
    }

    @Test
    void deletingAnUnknownCityIs404() {
        assertThatThrownBy(() -> service.deleteCity(MK, UUID.randomUUID()))
                .isInstanceOf(CityNotFoundException.class);
    }

    @Test
    void aRegionalTeamMemberOfTheZonesCityDeletesAnEmptyZone() {
        UUID cityId = UUID.randomUUID();
        UUID zoneId = zones.seed(cityId);
        regionalTeamCities.put(REGIONAL_TEAM, List.of(cityId));

        service.deleteZone(REGIONAL_TEAM, zoneId);

        assertThat(zones.existsById(zoneId)).isFalse();
    }

    @Test
    void aRegionalTeamMemberOfAnotherCityCannotDeleteThisZone() {
        UUID cityId = UUID.randomUUID();
        UUID zoneId = zones.seed(cityId);
        regionalTeamCities.put(REGIONAL_TEAM, List.of(UUID.randomUUID()));

        assertThatThrownBy(() -> service.deleteZone(REGIONAL_TEAM, zoneId))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(zones.existsById(zoneId)).isTrue();
    }

    @Test
    void deletingAZoneWithKshetrasIsBlockedWithItsChildCountAndNothingIsRemoved() {
        UUID cityId = UUID.randomUUID();
        UUID zoneId = zones.seed(cityId);
        zones.setKshetraCount(zoneId, 3);
        regionalTeamCities.put(REGIONAL_TEAM, List.of(cityId));

        assertThatThrownBy(() -> service.deleteZone(REGIONAL_TEAM, zoneId))
                .isInstanceOf(StructuralNotEmptyException.class)
                .hasMessage("has 3 Kshetras");
        assertThat(zones.existsById(zoneId)).isTrue();
    }

    @Test
    void deletingAnUnknownZoneIs404() {
        assertThatThrownBy(() -> service.deleteZone(REGIONAL_TEAM, UUID.randomUUID()))
                .isInstanceOf(ZoneNotFoundException.class);
    }

    @Test
    void theZonesSanyojakDeletesAnEmptyKshetra() {
        UUID zoneId = UUID.randomUUID();
        UUID kshetraId = kshetras.seed(zoneId);
        sanyojakZones.put(SANYOJAK, List.of(zoneId));

        service.deleteKshetra(SANYOJAK, kshetraId);

        assertThat(kshetras.existsById(kshetraId)).isFalse();
    }

    @Test
    void aSanyojakOfAnotherZoneCannotDeleteThisKshetra() {
        UUID zoneId = UUID.randomUUID();
        UUID kshetraId = kshetras.seed(zoneId);
        sanyojakZones.put(SANYOJAK, List.of(UUID.randomUUID()));

        assertThatThrownBy(() -> service.deleteKshetra(SANYOJAK, kshetraId))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(kshetras.existsById(kshetraId)).isTrue();
    }

    @Test
    void deletingAKshetraWithSabhasIsBlockedWithItsChildCountAndNothingIsRemoved() {
        UUID zoneId = UUID.randomUUID();
        UUID kshetraId = kshetras.seed(zoneId);
        kshetras.setSabhaCount(kshetraId, 1);
        sanyojakZones.put(SANYOJAK, List.of(zoneId));

        assertThatThrownBy(() -> service.deleteKshetra(SANYOJAK, kshetraId))
                .isInstanceOf(StructuralNotEmptyException.class)
                .hasMessage("has 1 Sabha");
        assertThat(kshetras.existsById(kshetraId)).isTrue();
    }

    @Test
    void deletingAnUnknownKshetraIs404() {
        assertThatThrownBy(() -> service.deleteKshetra(SANYOJAK, UUID.randomUUID()))
                .isInstanceOf(KshetraNotFoundException.class);
    }

    @Test
    void theNirdeshakOverTheSabhasScopeDeletesAnEmptySabha() {
        UUID kshetraId = UUID.randomUUID();
        UUID sabhaId = hierarchy.seedSabha(kshetraId, YUVAK);
        roleAssignments.grant(NIRDESHAK, kshetraId, YUVAK, Role.NIRDESHAK);

        service.deleteSabha(NIRDESHAK, sabhaId);

        assertThat(sabhas.deleted).contains(sabhaId);
    }

    @Test
    void aNirdeshakOfAnotherScopeCannotDeleteThisSabha() {
        UUID kshetraId = UUID.randomUUID();
        UUID sabhaId = hierarchy.seedSabha(kshetraId, YUVAK);
        // Holds Nirdeshak over a different demographic in the same Kshetra.
        roleAssignments.grant(NIRDESHAK, kshetraId, "BAAL", Role.NIRDESHAK);

        assertThatThrownBy(() -> service.deleteSabha(NIRDESHAK, sabhaId))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(sabhas.deleted).isEmpty();
    }

    @Test
    void deletingASabhaWithOccurrencesIsBlockedWithItsCountAndNothingIsRemoved() {
        UUID kshetraId = UUID.randomUUID();
        UUID sabhaId = hierarchy.seedSabha(kshetraId, YUVAK);
        sabhas.setOccurrenceCount(sabhaId, 12);
        roleAssignments.grant(NIRDESHAK, kshetraId, YUVAK, Role.NIRDESHAK);

        assertThatThrownBy(() -> service.deleteSabha(NIRDESHAK, sabhaId))
                .isInstanceOf(StructuralNotEmptyException.class)
                .hasMessage("has 12 Occurrences");
        assertThat(sabhas.deleted).isEmpty();
    }

    @Test
    void deletingAnUnknownSabhaIs404() {
        assertThatThrownBy(() -> service.deleteSabha(NIRDESHAK, UUID.randomUUID()))
                .isInstanceOf(SabhaNotFoundException.class);
    }

    private static final class FakeCities implements CityRepository {
        final List<City> saved = new ArrayList<>();
        final java.util.Map<UUID, Integer> zoneCounts = new java.util.HashMap<>();

        UUID seed(String name) {
            City city = City.create(name, MK);
            saved.add(city);
            return city.id();
        }

        void setZoneCount(UUID cityId, int count) {
            zoneCounts.put(cityId, count);
        }

        @Override
        public void save(City city) {
            saved.add(city);
        }

        @Override
        public boolean existsById(UUID id) {
            return saved.stream().anyMatch(c -> c.id().equals(id));
        }

        @Override
        public int zoneCount(UUID cityId) {
            return zoneCounts.getOrDefault(cityId, 0);
        }

        @Override
        public void deleteById(UUID id) {
            saved.removeIf(c -> c.id().equals(id));
        }
    }

    private static final class FakeZones implements ZoneRepository {
        final List<Zone> saved = new ArrayList<>();
        final java.util.Map<UUID, Integer> kshetraCounts = new java.util.HashMap<>();

        UUID seed(UUID cityId) {
            Zone zone = Zone.create(cityId, "Zone", MK);
            saved.add(zone);
            return zone.id();
        }

        void setKshetraCount(UUID zoneId, int count) {
            kshetraCounts.put(zoneId, count);
        }

        @Override
        public void save(Zone zone) {
            saved.add(zone);
        }

        @Override
        public boolean existsById(UUID id) {
            return saved.stream().anyMatch(z -> z.id().equals(id));
        }

        @Override
        public Optional<UUID> cityIdOf(UUID id) {
            return saved.stream().filter(z -> z.id().equals(id)).map(Zone::cityId).findFirst();
        }

        @Override
        public int kshetraCount(UUID id) {
            return kshetraCounts.getOrDefault(id, 0);
        }

        @Override
        public void deleteById(UUID id) {
            saved.removeIf(z -> z.id().equals(id));
        }
    }

    private static final class FakeKshetras implements KshetraRepository {
        final List<Kshetra> saved = new ArrayList<>();
        final java.util.Map<UUID, Integer> sabhaCounts = new java.util.HashMap<>();

        UUID seed(UUID zoneId) {
            Kshetra kshetra = Kshetra.create(zoneId, "Kshetra", MK);
            saved.add(kshetra);
            return kshetra.id();
        }

        void setSabhaCount(UUID kshetraId, int count) {
            sabhaCounts.put(kshetraId, count);
        }

        boolean existsById(UUID id) {
            return saved.stream().anyMatch(k -> k.id().equals(id));
        }

        @Override
        public void save(Kshetra kshetra) {
            saved.add(kshetra);
        }

        @Override
        public Optional<UUID> zoneIdOf(UUID id) {
            return saved.stream().filter(k -> k.id().equals(id)).map(Kshetra::zoneId).findFirst();
        }

        @Override
        public int sabhaCount(UUID id) {
            return sabhaCounts.getOrDefault(id, 0);
        }

        @Override
        public void deleteById(UUID id) {
            saved.removeIf(k -> k.id().equals(id));
        }
    }

    private static final class FakeSabhas implements SabhaRepository {
        final java.util.Map<UUID, Integer> occurrenceCounts = new java.util.HashMap<>();
        final List<UUID> deleted = new ArrayList<>();

        void setOccurrenceCount(UUID sabhaId, int count) {
            occurrenceCounts.put(sabhaId, count);
        }

        @Override
        public int occurrenceCount(UUID sabhaId) {
            return occurrenceCounts.getOrDefault(sabhaId, 0);
        }

        @Override
        public void deleteById(UUID sabhaId) {
            deleted.add(sabhaId);
        }
    }

    /** Resolves a seeded Sabha to its (Kshetra, demographic) scope; other walks are unused here. */
    private static final class FakeHierarchy implements org.sabha.common.StructuralHierarchyLookup {
        final java.util.Map<UUID, SabhaScope> scopes = new java.util.HashMap<>();

        UUID seedSabha(UUID kshetraId, String demographic) {
            UUID sabhaId = UUID.randomUUID();
            scopes.put(sabhaId, new SabhaScope(kshetraId, demographic, "REGULAR"));
            return sabhaId;
        }

        @Override
        public Optional<SabhaScope> sabhaScope(UUID sabhaId) {
            return Optional.ofNullable(scopes.get(sabhaId));
        }

        @Override
        public Optional<UUID> zoneOfKshetra(UUID kshetraId) {
            return Optional.empty();
        }

        @Override
        public Optional<UUID> cityOfZone(UUID zoneId) {
            return Optional.empty();
        }
    }

}
