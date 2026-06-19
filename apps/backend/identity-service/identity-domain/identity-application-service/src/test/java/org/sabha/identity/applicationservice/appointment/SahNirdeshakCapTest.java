package org.sabha.identity.applicationservice.appointment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The single source of truth for the Sah-Nirdeshak cap (ADR-0025 §3): how many a
 * (Kshetra, demographic) currently holds and whether the limit of two is reached.
 * Both the appointment write path (enforcement) and the web read path (the 2/2
 * indicator) ask this one component, so the threshold lives in exactly one place.
 */
class SahNirdeshakCapTest {

    private static final UUID KSHETRA = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final String YUVAK = "YUVAK";

    @Test
    void reportsTheActiveCountAgainstTheCapWithRoomToSpare() {
        SahNirdeshakCap cap = new SahNirdeshakCap(counts(KSHETRA, YUVAK, 1));

        SahNirdeshakCap.CapStatus status = cap.statusFor(KSHETRA, YUVAK);

        assertThat(status.active()).isEqualTo(1);
        assertThat(status.cap()).isEqualTo(2);
        assertThat(status.reached()).isFalse();
    }

    @Test
    void isReachedAtTheCap() {
        SahNirdeshakCap cap = new SahNirdeshakCap(counts(KSHETRA, YUVAK, 2));

        assertThat(cap.statusFor(KSHETRA, YUVAK).reached()).isTrue();
    }

    private static SahNirdeshakCountLookup counts(UUID kshetraId, String demographic, int count) {
        Map<String, Integer> byScope = new HashMap<>();
        byScope.put(kshetraId + "|" + demographic, count);
        return (k, d) -> byScope.getOrDefault(k + "|" + d, 0);
    }
}
