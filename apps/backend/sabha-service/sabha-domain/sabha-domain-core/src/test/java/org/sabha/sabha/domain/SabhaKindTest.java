package org.sabha.sabha.domain;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class SabhaKindTest {

    private static final UUID CREATOR = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

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
}
