package org.sabha.sabha.applicationservice;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.AuthorizationDeniedException;
import org.sabha.sabha.domain.Demographic;
import org.sabha.sabha.domain.SabhaKind;
import org.sabha.sabha.domain.SabhaKindAlreadyRetiredException;
import org.sabha.sabha.domain.SabhaKindNotFoundException;
import org.sabha.sabha.domain.SabhaKindNotRetiredException;
import org.sabha.sabha.domain.Track;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SabhaKindLifecycleServiceTest {

    private static final UUID MK = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID NON_MK = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
    private static final Instant NOW = Instant.parse("2026-06-19T10:00:00Z");

    private final FakeSabhaKinds sabhaKinds = new FakeSabhaKinds();

    private final SabhaKindLifecycleService service = new SabhaKindLifecycleService(
            new StructuralScopeAuthority(
                    userId -> userId.equals(MK), userId -> List.of(), userId -> List.of(),
                    new FakeRoleAssignments()),
            sabhaKinds,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void mkRetiresAnActiveKindAttributedToThemselves() {
        UUID kindId = sabhaKinds.seedActive(Demographic.YUVAK, Track.BSS);

        service.retire(MK, kindId);

        SabhaKind retired = sabhaKinds.byId.get(kindId);
        assertThat(retired.isRetired()).isTrue();
        assertThat(retired.retiredAt()).isEqualTo(NOW);
        assertThat(retired.retiredBy()).isEqualTo(MK);
    }

    @Test
    void mkReactivatesARetiredKind() {
        UUID kindId = sabhaKinds.seedActive(Demographic.YUVAK, Track.BSS);
        service.retire(MK, kindId);

        service.reactivate(MK, kindId);

        assertThat(sabhaKinds.byId.get(kindId).isRetired()).isFalse();
    }

    @Test
    void nonMkRetiringIsDeniedAndNothingChanges() {
        UUID kindId = sabhaKinds.seedActive(Demographic.YUVAK, Track.BSS);

        assertThatThrownBy(() -> service.retire(NON_MK, kindId))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(sabhaKinds.byId.get(kindId).isRetired()).isFalse();
    }

    @Test
    void nonMkReactivatingIsDenied() {
        UUID kindId = sabhaKinds.seedActive(Demographic.YUVAK, Track.BSS);
        service.retire(MK, kindId);

        assertThatThrownBy(() -> service.reactivate(NON_MK, kindId))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(sabhaKinds.byId.get(kindId).isRetired()).isTrue();
    }

    @Test
    void retiringAnUnknownKindIsNotFound() {
        assertThatThrownBy(() -> service.retire(MK, UUID.randomUUID()))
                .isInstanceOf(SabhaKindNotFoundException.class);
    }

    @Test
    void retiringAnAlreadyRetiredKindIsRejected() {
        UUID kindId = sabhaKinds.seedActive(Demographic.YUVAK, Track.BSS);
        service.retire(MK, kindId);

        assertThatThrownBy(() -> service.retire(MK, kindId))
                .isInstanceOf(SabhaKindAlreadyRetiredException.class);
    }

    @Test
    void reactivatingAnActiveKindIsRejected() {
        UUID kindId = sabhaKinds.seedActive(Demographic.YUVAK, Track.BSS);

        assertThatThrownBy(() -> service.reactivate(MK, kindId))
                .isInstanceOf(SabhaKindNotRetiredException.class);
    }

    private static final class FakeSabhaKinds implements SabhaKindRepository {
        final Map<UUID, SabhaKind> byId = new HashMap<>();

        UUID seedActive(Demographic demographic, Track track) {
            SabhaKind kind = SabhaKind.register(demographic, track, MK);
            byId.put(kind.id(), kind);
            return kind.id();
        }

        @Override
        public void save(SabhaKind kind) {
            byId.put(kind.id(), kind);
        }

        @Override
        public boolean exists(Demographic demographic, Track track) {
            return byId.values().stream().anyMatch(k -> k.demographic() == demographic && k.track() == track);
        }

        @Override
        public Optional<SabhaKind> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public void update(SabhaKind kind) {
            byId.put(kind.id(), kind);
        }
    }
}
