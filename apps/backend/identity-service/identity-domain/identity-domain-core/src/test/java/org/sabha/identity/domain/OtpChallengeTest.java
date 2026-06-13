package org.sabha.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The single home for the OTP consent state-machine internals (TTL, attempt
 * budget, lockout, expiry). The OTP-gated aggregates ({@link HomeSabhaTransfer},
 * {@link PasswordReset}) compose this challenge and are tested at the flow level;
 * the rules below are deliberately verified once, here.
 */
class OtpChallengeTest {

    private static final UUID CHALLENGE = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final String CODE = "123456";
    private static final Instant NOW = Instant.parse("2026-05-31T10:00:00Z");

    @Test
    void verifyWithTheCorrectCodeWithinTtlReturnsNormally() {
        OtpChallenge challenge = OtpChallenge.issue(CHALLENGE, CODE, NOW);

        assertThatCode(() -> challenge.verify(CODE, NOW.plus(Duration.ofMinutes(4))))
                .doesNotThrowAnyException();
        assertThat(challenge.attempts()).isZero();
        assertThat(challenge.isLocked()).isFalse();
        assertThat(challenge.isExpired()).isFalse();
    }

    @Test
    void verifyWithAWrongCodeCountsAnAttemptAndThrowsCarryingTheChallengeId() {
        OtpChallenge challenge = OtpChallenge.issue(CHALLENGE, CODE, NOW);

        assertThatThrownBy(() -> challenge.verify("000000", NOW))
                .isInstanceOf(WrongOtpException.class)
                .hasMessageContaining(CHALLENGE.toString());
        assertThat(challenge.attempts()).isEqualTo(1);
        assertThat(challenge.isLocked()).isFalse();
    }

    @Test
    void verifyAfterTheTtlMarksExpiredAndThrowsCarryingTheChallengeId() {
        OtpChallenge challenge = OtpChallenge.issue(CHALLENGE, CODE, NOW);

        assertThatThrownBy(() -> challenge.verify(CODE, NOW.plus(OtpChallenge.TTL).plusSeconds(1)))
                .isInstanceOf(OtpExpiredException.class)
                .hasMessageContaining(CHALLENGE.toString());
        assertThat(challenge.isExpired()).isTrue();
    }

    @Test
    void verifyLocksAfterTheAttemptBudgetIsExhaustedAndRejectsEvenTheCorrectCode() {
        OtpChallenge challenge = OtpChallenge.issue(CHALLENGE, CODE, NOW);

        for (int attempt = 0; attempt < OtpChallenge.MAX_ATTEMPTS - 1; attempt++) {
            assertThatThrownBy(() -> challenge.verify("000000", NOW))
                    .isInstanceOf(WrongOtpException.class);
        }
        // The budget-exhausting attempt locks the challenge.
        assertThatThrownBy(() -> challenge.verify("000000", NOW))
                .isInstanceOf(OtpAttemptsExhaustedException.class)
                .hasMessageContaining(CHALLENGE.toString());
        assertThat(challenge.isLocked()).isTrue();
        assertThat(challenge.attempts()).isEqualTo(OtpChallenge.MAX_ATTEMPTS);

        // Once locked, even the right code is refused.
        assertThatThrownBy(() -> challenge.verify(CODE, NOW))
                .isInstanceOf(OtpAttemptsExhaustedException.class);
    }

    @Test
    void rehydratePreservesAttemptsLockedAndExpiredState() {
        OtpChallenge locked = OtpChallenge.rehydrate(
                CHALLENGE, CODE, NOW.plus(OtpChallenge.TTL), OtpChallenge.MAX_ATTEMPTS, true, false);

        assertThat(locked.attempts()).isEqualTo(OtpChallenge.MAX_ATTEMPTS);
        assertThat(locked.isLocked()).isTrue();
        // A rehydrated locked challenge keeps refusing the correct code.
        assertThatThrownBy(() -> locked.verify(CODE, NOW))
                .isInstanceOf(OtpAttemptsExhaustedException.class);

        OtpChallenge expired = OtpChallenge.rehydrate(
                CHALLENGE, CODE, NOW.minus(Duration.ofMinutes(1)), 2, false, true);

        assertThat(expired.isExpired()).isTrue();
        assertThat(expired.attempts()).isEqualTo(2);
    }
}
