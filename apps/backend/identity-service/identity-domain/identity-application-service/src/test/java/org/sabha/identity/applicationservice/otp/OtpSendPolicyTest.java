package org.sabha.identity.applicationservice.otp;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OtpSendPolicyTest {

    private static final String MOBILE = "+919820100200";
    private static final Instant NOW = Instant.parse("2026-06-14T10:00:00Z");

    private final OtpSendPolicy policy = new OtpSendPolicy();

    @Test
    void allowsTheFirstSendToAMobileWithNoHistory() {
        assertThatCode(() -> policy.enforce(MOBILE, new FakeOtpSendLog(), NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAResendWithinThirtySecondsOfTheLastSend() {
        FakeOtpSendLog log = new FakeOtpSendLog();
        log.lastSentAt = Optional.of(NOW.minus(Duration.ofSeconds(20)));

        assertThatThrownBy(() -> policy.enforce(MOBILE, log, NOW))
                .isInstanceOf(OtpResendCooldownException.class);
    }

    @Test
    void allowsAResendOnceTheCooldownHasElapsed() {
        FakeOtpSendLog log = new FakeOtpSendLog();
        log.lastSentAt = Optional.of(NOW.minus(Duration.ofSeconds(30)));

        assertThatCode(() -> policy.enforce(MOBILE, log, NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsTheSendOnceThreeOtpsHaveGoneOutInTheWindow() {
        FakeOtpSendLog log = new FakeOtpSendLog();
        log.lastSentAt = Optional.of(NOW.minus(Duration.ofMinutes(1)));
        log.countInWindow = 3;

        assertThatThrownBy(() -> policy.enforce(MOBILE, log, NOW))
                .isInstanceOf(OtpRateLimitExceededException.class);
    }

    @Test
    void allowsTheThirdSendWhileTwoAreStillInTheWindow() {
        FakeOtpSendLog log = new FakeOtpSendLog();
        log.lastSentAt = Optional.of(NOW.minus(Duration.ofMinutes(1)));
        log.countInWindow = 2;

        assertThatCode(() -> policy.enforce(MOBILE, log, NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void countsSendsOnlyWithinTheOneHourWindowBeforeNow() {
        FakeOtpSendLog log = new FakeOtpSendLog();

        policy.enforce(MOBILE, log, NOW);

        assertThat(log.queriedSince).isEqualTo(NOW.minus(Duration.ofHours(1)));
    }

    /** A controllable {@link OtpSendLog} that records the window it was asked to count over. */
    static final class FakeOtpSendLog implements OtpSendLog {
        Optional<Instant> lastSentAt = Optional.empty();
        int countInWindow;
        Instant queriedSince;

        @Override
        public Optional<Instant> lastInitiatedAt(String mobile) {
            return lastSentAt;
        }

        @Override
        public int sendCountSince(String mobile, Instant since) {
            this.queriedSince = since;
            return countInWindow;
        }
    }
}
