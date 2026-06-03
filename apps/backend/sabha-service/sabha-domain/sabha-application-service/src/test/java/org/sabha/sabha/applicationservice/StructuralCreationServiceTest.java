package org.sabha.sabha.applicationservice;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.AuthorizationDeniedException;
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
    private static final UUID ZONE = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

    private final FakeCities cities = new FakeCities();
    private final FakeZones zones = new FakeZones();
    private final FakeKshetras kshetras = new FakeKshetras();
    private final FakeSabhaKinds sabhaKinds = new FakeSabhaKinds();

    private final StructuralCreationService service = new StructuralCreationService(
            new StructuralCreationAuthorization(
                    userId -> userId.equals(MK),
                    userId -> userId.equals(SANYOJAK) ? java.util.List.of(ZONE) : java.util.List.of()),
            cities, zones, kshetras, sabhaKinds);

    @Test
    void mkMemberCreatesACityAttributedToThemselves() {
        UUID id = service.createCity(MK, "Surat");

        City saved = cities.saved.get(0);
        assertThat(saved.id()).isEqualTo(id);
        assertThat(saved.name()).isEqualTo("Surat");
        assertThat(saved.createdBy()).isEqualTo(MK);
    }

    @Test
    void nonMkCreatingACityIsDeniedAndNothingIsPersisted() {
        assertThatThrownBy(() -> service.createCity(SANYOJAK, "Surat"))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(cities.saved).isEmpty();
    }

    @Test
    void mkMemberCreatesAZoneWithinAnExistingCity() {
        UUID cityId = service.createCity(MK, "Mumbai");

        UUID zoneId = service.createZone(MK, cityId, "Mumbai South");

        Zone saved = zones.saved.get(0);
        assertThat(saved.id()).isEqualTo(zoneId);
        assertThat(saved.cityId()).isEqualTo(cityId);
        assertThat(saved.createdBy()).isEqualTo(MK);
    }

    @Test
    void mkMemberRegistersASabhaKind() {
        UUID id = service.createSabhaKind(MK, Demographic.YUVAK, Track.BSS);

        SabhaKind saved = sabhaKinds.saved.get(0);
        assertThat(saved.id()).isEqualTo(id);
        assertThat(saved.demographic()).isEqualTo(Demographic.YUVAK);
        assertThat(saved.track()).isEqualTo(Track.BSS);
        assertThat(saved.createdBy()).isEqualTo(MK);
    }

    @Test
    void registeringASanyuktaSelectiveKindIsRejected() {
        assertThatThrownBy(() -> service.createSabhaKind(MK, Demographic.SANYUKTA, Track.YSS))
                .isInstanceOf(SanyuktaMustBeRegularTrackException.class);
        assertThat(sabhaKinds.saved).isEmpty();
    }

    @Test
    void sanyojakCreatesAKshetraWithinTheirZone() {
        UUID id = service.createKshetra(SANYOJAK, ZONE, "Goregaon-2");

        Kshetra saved = kshetras.saved.get(0);
        assertThat(saved.id()).isEqualTo(id);
        assertThat(saved.zoneId()).isEqualTo(ZONE);
        assertThat(saved.createdBy()).isEqualTo(SANYOJAK);
    }

    @Test
    void nonSanyojakCreatingAKshetraIsDeniedAndNothingIsPersisted() {
        assertThatThrownBy(() -> service.createKshetra(MK, ZONE, "Goregaon-2"))
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
    }

    private static final class FakeKshetras implements KshetraRepository {
        final List<Kshetra> saved = new ArrayList<>();

        @Override
        public void save(Kshetra kshetra) {
            saved.add(kshetra);
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
    }
}
