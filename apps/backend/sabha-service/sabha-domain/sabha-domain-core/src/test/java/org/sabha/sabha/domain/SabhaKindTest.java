package org.sabha.sabha.domain;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class SabhaKindTest {

    private static final UUID CREATOR = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID MK = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
    private static final Instant WHEN = Instant.parse("2026-06-19T10:00:00Z");

    @Test
    void registersADemographicTrackKindAttributedToItsCreator() {
        SabhaKind kind = SabhaKind.register(Demographic.YUVAK, Track.BSS, CREATOR);

        assertThat(kind.id()).isNotNull();
        assertThat(kind.demographic()).isEqualTo(Demographic.YUVAK);
        assertThat(kind.track()).isEqualTo(Track.BSS);
        assertThat(kind.createdBy()).isEqualTo(CREATOR);
    }

    @Test
    void rejectsSanyuktaOnTheBssTrack() {
        assertThatThrownBy(() -> SabhaKind.register(Demographic.SANYUKTA, Track.BSS, CREATOR))
                .isInstanceOf(SanyuktaMustBeRegularTrackException.class);
    }

    @Test
    void rejectsSanyuktaOnTheYssTrack() {
        assertThatThrownBy(() -> SabhaKind.register(Demographic.SANYUKTA, Track.YSS, CREATOR))
                .isInstanceOf(SanyuktaMustBeRegularTrackException.class);
    }

    @Test
    void allowsSanyuktaOnTheRegularTrack() {
        assertThatCode(() -> SabhaKind.register(Demographic.SANYUKTA, Track.REGULAR, CREATOR))
                .doesNotThrowAnyException();
    }

    @Test
    void registersActiveByDefault() {
        SabhaKind kind = SabhaKind.register(Demographic.YUVAK, Track.REGULAR, CREATOR);

        assertThat(kind.isRetired()).isFalse();
        assertThat(kind.retiredAt()).isNull();
        assertThat(kind.retiredBy()).isNull();
    }

    @Test
    void retiresAnActiveKindAttributedToTheActingMember() {
        SabhaKind retired = SabhaKind.register(Demographic.YUVAK, Track.REGULAR, CREATOR)
                .retire(MK, WHEN);

        assertThat(retired.isRetired()).isTrue();
        assertThat(retired.retiredAt()).isEqualTo(WHEN);
        assertThat(retired.retiredBy()).isEqualTo(MK);
    }

    @Test
    void reactivatesARetiredKindBackToActive() {
        SabhaKind reactivated = SabhaKind.register(Demographic.YUVAK, Track.REGULAR, CREATOR)
                .retire(MK, WHEN)
                .reactivate();

        assertThat(reactivated.isRetired()).isFalse();
        assertThat(reactivated.retiredAt()).isNull();
        assertThat(reactivated.retiredBy()).isNull();
    }

    @Test
    void rejectsRetiringAnAlreadyRetiredKind() {
        SabhaKind retired = SabhaKind.register(Demographic.YUVAK, Track.REGULAR, CREATOR)
                .retire(MK, WHEN);

        assertThatThrownBy(() -> retired.retire(MK, WHEN))
                .isInstanceOf(SabhaKindAlreadyRetiredException.class);
    }

    @Test
    void rejectsReactivatingAnActiveKind() {
        SabhaKind active = SabhaKind.register(Demographic.YUVAK, Track.REGULAR, CREATOR);

        assertThatThrownBy(active::reactivate)
                .isInstanceOf(SabhaKindNotRetiredException.class);
    }
}
