package org.sabha.identity.domain;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HomeSabhaSwapTest {

    private static final UUID YUVAK = UUID.randomUUID();
    private static final UUID SANYUKTA = UUID.randomUUID();

    @Test
    void selectsTheHomeSabhaWhoseKindMatchesTheDestination() {
        List<HomeSabhaRef> current = List.of(
                new HomeSabhaRef(YUVAK, "REGULAR_YUVAK"),
                new HomeSabhaRef(SANYUKTA, "REGULAR_SANYUKTA"));

        UUID previous = HomeSabhaSwap.selectPrevious(current, "REGULAR_YUVAK");

        assertThat(previous).isEqualTo(YUVAK);
    }

    @Test
    void rejectsWhenThePersonHoldsNoHomeSabhaOfTheDestinationKind() {
        List<HomeSabhaRef> current = List.of(new HomeSabhaRef(SANYUKTA, "REGULAR_SANYUKTA"));

        assertThatThrownBy(() -> HomeSabhaSwap.selectPrevious(current, "REGULAR_YUVAK"))
                .isInstanceOf(NoMatchingHomeSabhaException.class);
    }
}
