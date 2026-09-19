package org.sabha.sabha.applicationservice;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.CallerAuthority;
import org.sabha.sabha.domain.City;
import org.sabha.sabha.domain.Demographic;
import org.sabha.sabha.domain.Kshetra;
import org.sabha.sabha.domain.SabhaKind;
import org.sabha.sabha.domain.SanyuktaMustBeRegularTrackException;
import org.sabha.sabha.domain.Track;
import org.sabha.sabha.domain.Zone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StructuralCreationServiceTest {

    private static final UUID MK = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID SANYOJAK = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
    private static final UUID REGIONAL_TEAM = UUID.fromString("00000000-0000-0000-0000-0000000000dd");
    private static final UUID ZONE = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

    private final FakeCities cities = new FakeCities();
    private final FakeZones zones = new FakeZones();
    private final FakeKshetras kshetras = new FakeKshetras();
    private final FakeSabhaKinds sabhaKinds = new FakeSabhaKinds();

    // Four test doubles became none: ADR-0032 deleted StructuralScopeAuthority for
    // holding no policy, and the tier checks read the caller's own rows instead.
    private final StructuralCreationService service =
            new StructuralCreationService(cities, zones, kshetras, sabhaKinds);

    @Test
    void mkMemberCreatesACityAttributedToThemselves() {
        UUID id = service.createCity(Callers.madhyasthaKaryalaya(MK), "Surat");

        City saved = cities.saved.get(0);
        assertThat(saved.id()).isEqualTo(id);
        assertThat(saved.name()).isEqualTo("Surat");
        assertThat(saved.createdBy()).isEqualTo(MK);
    }

    @Test
    void nonMkCreatingACityIsDeniedAndNothingIsPersisted() {
        assertThatThrownBy(() -> service.createCity(Callers.of(SANYOJAK).sanyojakOf(ZONE).build(), "Surat"))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(cities.saved).isEmpty();
    }

    @Test
    void regionalTeamMemberCreatesAZoneWithinTheirCityAttributedToThemselves() {
        UUID cityId = service.createCity(Callers.madhyasthaKaryalaya(MK), "Mumbai");

        UUID zoneId = service.createZone(
                Callers.of(REGIONAL_TEAM).regionalTeamIn(cityId).build(), cityId, "Mumbai South");

        Zone saved = zones.saved.get(0);
        assertThat(saved.id()).isEqualTo(zoneId);
        assertThat(saved.cityId()).isEqualTo(cityId);
        assertThat(saved.createdBy()).isEqualTo(REGIONAL_TEAM);
    }

    @Test
    void mkCreatingAZoneIsDeniedAndNothingIsPersisted() {
        // Zone creation moved MK -> Regional Team (ADR-0024); MK has no path at all.
        UUID cityId = service.createCity(Callers.madhyasthaKaryalaya(MK), "Mumbai");

        assertThatThrownBy(() -> service.createZone(Callers.madhyasthaKaryalaya(MK), cityId, "Mumbai South"))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(zones.saved).isEmpty();
    }

    @Test
    void regionalTeamMemberOfAnotherCityCannotCreateAZoneHere() {
        UUID cityId = service.createCity(Callers.madhyasthaKaryalaya(MK), "Mumbai");
        // REGIONAL_TEAM is a member of some *other* City, not this one.
        CallerAuthority elsewhere = Callers.of(REGIONAL_TEAM).regionalTeamIn(UUID.randomUUID()).build();

        assertThatThrownBy(() -> service.createZone(elsewhere, cityId, "Mumbai South"))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(zones.saved).isEmpty();
    }

    @Test
    void mkMemberRegistersASabhaKind() {
        UUID id = service.createSabhaKind(Callers.madhyasthaKaryalaya(MK), Demographic.YUVAK, Track.BSS);

        SabhaKind saved = sabhaKinds.saved.get(0);
        assertThat(saved.id()).isEqualTo(id);
        assertThat(saved.demographic()).isEqualTo(Demographic.YUVAK);
        assertThat(saved.track()).isEqualTo(Track.BSS);
        assertThat(saved.createdBy()).isEqualTo(MK);
    }

    @Test
    void registeringASanyuktaSelectiveKindIsRejected() {
        assertThatThrownBy(() -> service.createSabhaKind(Callers.madhyasthaKaryalaya(MK), Demographic.SANYUKTA, Track.YSS))
                .isInstanceOf(SanyuktaMustBeRegularTrackException.class);
        assertThat(sabhaKinds.saved).isEmpty();
    }

    @Test
    void sanyojakCreatesAKshetraWithinTheirZone() {
        UUID id = service.createKshetra(Callers.of(SANYOJAK).sanyojakOf(ZONE).build(), ZONE, "Goregaon-2");

        Kshetra saved = kshetras.saved.get(0);
        assertThat(saved.id()).isEqualTo(id);
        assertThat(saved.zoneId()).isEqualTo(ZONE);
        assertThat(saved.createdBy()).isEqualTo(SANYOJAK);
    }

    @Test
    void nonSanyojakCreatingAKshetraIsDeniedAndNothingIsPersisted() {
        assertThatThrownBy(() -> service.createKshetra(Callers.madhyasthaKaryalaya(MK), ZONE, "Goregaon-2"))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(kshetras.saved).isEmpty();
    }

    private static final class FakeCities implements CityRepository {
        final List<City> saved = new ArrayList<>();

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
            return 0;
        }

        @Override
        public void deleteById(UUID id) {
            saved.removeIf(c -> c.id().equals(id));
        }
    }

    private static final class FakeZones implements ZoneRepository {
        final List<Zone> saved = new ArrayList<>();

        @Override
        public void save(Zone zone) {
            saved.add(zone);
        }

        @Override
        public boolean existsById(UUID id) {
            return saved.stream().anyMatch(z -> z.id().equals(id));
        }

        @Override
        public java.util.Optional<UUID> cityIdOf(UUID id) {
            return saved.stream().filter(z -> z.id().equals(id)).map(Zone::cityId).findFirst();
        }

        @Override
        public int kshetraCount(UUID id) {
            return 0;
        }

        @Override
        public void deleteById(UUID id) {
            saved.removeIf(z -> z.id().equals(id));
        }
    }

    private static final class FakeKshetras implements KshetraRepository {
        final List<Kshetra> saved = new ArrayList<>();

        @Override
        public void save(Kshetra kshetra) {
            saved.add(kshetra);
        }

        @Override
        public java.util.Optional<UUID> zoneIdOf(UUID id) {
            return saved.stream().filter(k -> k.id().equals(id)).map(Kshetra::zoneId).findFirst();
        }

        @Override
        public int sabhaCount(UUID id) {
            return 0;
        }

        @Override
        public void deleteById(UUID id) {
            saved.removeIf(k -> k.id().equals(id));
        }
    }

    private static final class FakeSabhaKinds implements SabhaKindRepository {
        final List<SabhaKind> saved = new ArrayList<>();

        @Override
        public void save(SabhaKind kind) {
            saved.add(kind);
        }

        @Override
        public boolean exists(Demographic demographic, Track track) {
            return saved.stream().anyMatch(k -> k.demographic() == demographic && k.track() == track);
        }

        @Override
        public java.util.Optional<SabhaKind> findById(UUID id) {
            return saved.stream().filter(k -> k.id().equals(id)).findFirst();
        }

        @Override
        public void update(SabhaKind kind) {
            saved.replaceAll(k -> k.id().equals(kind.id()) ? kind : k);
        }
    }
}
