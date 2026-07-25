package org.sabha.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.DomainEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The self-service password-reset state machine (ADR-0004) exercised directly:
 * the request → verify → complete progression, the reset-token window, and how
 * {@link PasswordResetStatus} is derived from the composed {@link OtpChallenge}.
 * The OTP internals themselves live in {@link OtpChallengeTest}; what is verified
 * here is the aggregate's own behaviour around them.
 */
class PasswordResetTest {

    private static final UUID RESET = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
    private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-0000000000d2");
    private static final UUID KEYCLOAK_USER = UUID.fromString("00000000-0000-0000-0000-0000000000d3");
    private static final String MOBILE = "+919820100200";
    private static final String CODE = "654321";
    private static final String RESET_TOKEN = "reset-token-abc";
    private static final Instant NOW = Instant.parse("2026-06-07T10:00:00Z");

    /** Deterministic stand-in for the production HMAC hasher. */
    private static final OtpHasher HASHER = (challengeId, code) -> "digest(" + challengeId + ":" + code + ")";

    @Test
    void requestOpensAPendingResetHoldingOnlyTheHashedCodeAndRegistersTheRequestedEvent() {
        PasswordReset reset = PasswordReset.request(RESET, USER, KEYCLOAK_USER, MOBILE, CODE, NOW, HASHER);

        assertThat(reset.status()).isEqualTo(PasswordResetStatus.PENDING);
        assertThat(reset.id()).isEqualTo(RESET);
        assertThat(reset.userId()).isEqualTo(USER);
        assertThat(reset.keycloakUserId()).isEqualTo(KEYCLOAK_USER);
        assertThat(reset.mobile()).isEqualTo(MOBILE);
        assertThat(reset.initiatedAt()).isEqualTo(NOW);
        assertThat(reset.resetToken()).isNull();
        assertThat(reset.challenge().codeHash()).isNotEqualTo(CODE);

        List<DomainEvent> events = reset.pullDomainEvents();
        assertThat(events).singleElement().isInstanceOf(PasswordResetRequested.class);
        PasswordResetRequested requested = (PasswordResetRequested) events.get(0);
        assertThat(requested.aggregateId()).isEqualTo(RESET);
        assertThat(requested.userId()).isEqualTo(USER);
        assertThat(requested.occurredAt()).isEqualTo(NOW);
    }

    @Test
    void markOtpSentRegistersTheSentEventWithoutChangingTheLifecycle() {
        PasswordReset reset = PasswordReset.request(RESET, USER, KEYCLOAK_USER, MOBILE, CODE, NOW, HASHER);
        reset.pullDomainEvents();

        reset.markOtpSent(NOW);

        assertThat(reset.status()).isEqualTo(PasswordResetStatus.PENDING);
        assertThat(reset.pullDomainEvents()).singleElement().isInstanceOf(PasswordResetOtpSent.class);
    }

    @Test
    void verifyWithTheCorrectCodeStoresTheResetTokenForItsOwnTtl() {
        PasswordReset reset = PasswordReset.request(RESET, USER, KEYCLOAK_USER, MOBILE, CODE, NOW, HASHER);
        reset.pullDomainEvents();
        Instant verifiedAt = NOW.plus(Duration.ofMinutes(1));

        reset.verify(CODE, RESET_TOKEN, verifiedAt, HASHER);

        assertThat(reset.status()).isEqualTo(PasswordResetStatus.VERIFIED);
        assertThat(reset.resetToken()).isEqualTo(RESET_TOKEN);
        assertThat(reset.resetTokenExpiresAt()).isEqualTo(verifiedAt.plus(PasswordReset.RESET_TOKEN_TTL));
        assertThat(reset.pullDomainEvents()).singleElement().isInstanceOf(PasswordResetVerified.class);
    }

    @Test
    void verifyWithAWrongCodeLeavesTheResetPendingAndIssuesNoToken() {
        PasswordReset reset = PasswordReset.request(RESET, USER, KEYCLOAK_USER, MOBILE, CODE, NOW, HASHER);
        reset.pullDomainEvents();

        assertThatThrownBy(() -> reset.verify("000000", RESET_TOKEN, NOW, HASHER))
                .isInstanceOf(WrongOtpException.class);

        assertThat(reset.status()).isEqualTo(PasswordResetStatus.PENDING);
        assertThat(reset.resetToken()).isNull();
        assertThat(reset.pullDomainEvents()).isEmpty();
    }

    @Test
    void statusFollowsTheChallengeIntoExpiredAndLocked() {
        PasswordReset expired = PasswordReset.request(RESET, USER, KEYCLOAK_USER, MOBILE, CODE, NOW, HASHER);
        assertThatThrownBy(() -> expired.verify(CODE, RESET_TOKEN, NOW.plus(OtpChallenge.TTL).plusSeconds(1), HASHER))
                .isInstanceOf(OtpExpiredException.class);
        assertThat(expired.status()).isEqualTo(PasswordResetStatus.EXPIRED);

        PasswordReset locked = PasswordReset.request(RESET, USER, KEYCLOAK_USER, MOBILE, CODE, NOW, HASHER);
        for (int attempt = 0; attempt < OtpChallenge.MAX_ATTEMPTS - 1; attempt++) {
            assertThatThrownBy(() -> locked.verify("000000", RESET_TOKEN, NOW, HASHER))
                    .isInstanceOf(WrongOtpException.class);
        }
        assertThatThrownBy(() -> locked.verify("000000", RESET_TOKEN, NOW, HASHER))
                .isInstanceOf(OtpAttemptsExhaustedException.class);
        assertThat(locked.status()).isEqualTo(PasswordResetStatus.LOCKED);
    }

    @Test
    void completeWithinTheResetTokenWindowFinishesTheResetOnce() {
        PasswordReset reset = verified();

        reset.complete(NOW.plus(Duration.ofMinutes(2)));

        assertThat(reset.status()).isEqualTo(PasswordResetStatus.COMPLETED);
        List<DomainEvent> events = reset.pullDomainEvents();
        assertThat(events).singleElement().isInstanceOf(PasswordResetCompleted.class);
        assertThat(((PasswordResetCompleted) events.get(0)).userId()).isEqualTo(USER);

        assertThatThrownBy(() -> reset.complete(NOW.plus(Duration.ofMinutes(3))))
                .isInstanceOf(ResetAlreadyCompletedException.class);
        assertThat(reset.pullDomainEvents()).isEmpty();
    }

    @Test
    void completeAfterTheResetTokenExpiresIsRejectedAndLeavesTheResetVerified() {
        PasswordReset reset = verified();

        assertThatThrownBy(() -> reset.complete(NOW.plus(PasswordReset.RESET_TOKEN_TTL).plusSeconds(1)))
                .isInstanceOf(ResetTokenExpiredException.class);

        assertThat(reset.status()).isEqualTo(PasswordResetStatus.VERIFIED);
        assertThat(reset.pullDomainEvents()).isEmpty();
    }

    @Test
    void rehydrateIsTheExactInverseOfStatusAndRegistersNoEvents() {
        for (PasswordResetStatus status : PasswordResetStatus.values()) {
            PasswordReset reset = PasswordReset.rehydrate(
                    RESET, USER, KEYCLOAK_USER, MOBILE, HASHER.hash(RESET, CODE),
                    NOW.plus(OtpChallenge.TTL), 1, status, NOW,
                    RESET_TOKEN, NOW.plus(PasswordReset.RESET_TOKEN_TTL));

            assertThat(reset.status()).isEqualTo(status);
            assertThat(reset.pullDomainEvents()).isEmpty();
        }
    }

    @Test
    void aRehydratedPendingResetStillVerifiesAgainstTheStoredHash() {
        PasswordReset reset = PasswordReset.rehydrate(
                RESET, USER, KEYCLOAK_USER, MOBILE, HASHER.hash(RESET, CODE),
                NOW.plus(OtpChallenge.TTL), 0, PasswordResetStatus.PENDING, NOW, null, null);

        reset.verify(CODE, RESET_TOKEN, NOW, HASHER);

        assertThat(reset.status()).isEqualTo(PasswordResetStatus.VERIFIED);
    }

    private static PasswordReset verified() {
        PasswordReset reset = PasswordReset.request(RESET, USER, KEYCLOAK_USER, MOBILE, CODE, NOW, HASHER);
        reset.verify(CODE, RESET_TOKEN, NOW, HASHER);
        reset.pullDomainEvents();
        return reset;
    }
}
