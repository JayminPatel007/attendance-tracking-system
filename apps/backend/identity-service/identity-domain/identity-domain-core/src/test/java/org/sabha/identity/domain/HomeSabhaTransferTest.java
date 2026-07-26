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
 * The Verified Home Sabha Transfer state machine (ADR-0002) exercised directly:
 * the initiate → confirm → swap-recorded progression and how
 * {@link TransferStatus} is derived from the composed {@link OtpChallenge}. The
 * OTP internals themselves live in {@link OtpChallengeTest}; the Roster swap is
 * the application service's to perform — the aggregate only records that it
 * happened.
 */
class HomeSabhaTransferTest {

    private static final UUID TRANSFER = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final UUID PERSON = UUID.fromString("00000000-0000-0000-0000-0000000000e2");
    private static final UUID DESTINATION_SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000e3");
    private static final UUID PREVIOUS_SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000e4");
    private static final UUID INITIATOR = UUID.fromString("00000000-0000-0000-0000-0000000000e5");
    private static final String MOBILE = "+919820100200";
    private static final String CODE = "123456";
    private static final Instant NOW = Instant.parse("2026-05-31T10:00:00Z");

    /** Deterministic stand-in for the production HMAC hasher. */
    private static final OtpHasher HASHER = (challengeId, code) -> "digest(" + challengeId + ":" + code + ")";

    @Test
    void initiateOpensAPendingTransferHoldingOnlyTheHashedCodeAndRegistersTheInitiatedEvent() {
        HomeSabhaTransfer transfer = initiated();

        assertThat(transfer.status()).isEqualTo(TransferStatus.PENDING);
        assertThat(transfer.id()).isEqualTo(TRANSFER);
        assertThat(transfer.personId()).isEqualTo(PERSON);
        assertThat(transfer.mobile()).isEqualTo(MOBILE);
        assertThat(transfer.destinationSabhaId()).isEqualTo(DESTINATION_SABHA);
        assertThat(transfer.initiatingUserId()).isEqualTo(INITIATOR);
        assertThat(transfer.initiatedAt()).isEqualTo(NOW);
        assertThat(transfer.challenge().codeHash()).isNotEqualTo(CODE);

        List<DomainEvent> events = transfer.pullDomainEvents();
        assertThat(events).singleElement().isInstanceOf(HomeSabhaTransferInitiated.class);
        HomeSabhaTransferInitiated event = (HomeSabhaTransferInitiated) events.get(0);
        assertThat(event.aggregateId()).isEqualTo(TRANSFER);
        assertThat(event.personId()).isEqualTo(PERSON);
        assertThat(event.destinationSabhaId()).isEqualTo(DESTINATION_SABHA);
        assertThat(event.initiatingUserId()).isEqualTo(INITIATOR);
        assertThat(event.occurredAt()).isEqualTo(NOW);
    }

    @Test
    void markOtpSentRegistersTheSentEventWithoutChangingTheLifecycle() {
        HomeSabhaTransfer transfer = initiated();
        transfer.pullDomainEvents();

        transfer.markOtpSent(NOW);

        assertThat(transfer.status()).isEqualTo(TransferStatus.PENDING);
        assertThat(transfer.pullDomainEvents()).singleElement().isInstanceOf(TransferOtpSent.class);
    }

    @Test
    void confirmWithTheCorrectCodeRecordsTheConsentWithoutTouchingTheRoster() {
        HomeSabhaTransfer transfer = initiated();
        transfer.pullDomainEvents();
        Instant confirmedAt = NOW.plus(Duration.ofMinutes(1));

        transfer.confirm(CODE, confirmedAt, HASHER);

        assertThat(transfer.status()).isEqualTo(TransferStatus.CONFIRMED);
        List<DomainEvent> events = transfer.pullDomainEvents();
        assertThat(events).singleElement().isInstanceOf(TransferOtpConfirmed.class);
        TransferOtpConfirmed confirmed = (TransferOtpConfirmed) events.get(0);
        assertThat(confirmed.aggregateId()).isEqualTo(TRANSFER);
        assertThat(confirmed.personId()).isEqualTo(PERSON);
        assertThat(confirmed.occurredAt()).isEqualTo(confirmedAt);
    }

    @Test
    void confirmWithAWrongCodeLeavesTheTransferPending() {
        HomeSabhaTransfer transfer = initiated();
        transfer.pullDomainEvents();

        assertThatThrownBy(() -> transfer.confirm("000000", NOW, HASHER))
                .isInstanceOf(WrongOtpException.class);

        assertThat(transfer.status()).isEqualTo(TransferStatus.PENDING);
        assertThat(transfer.pullDomainEvents()).isEmpty();
    }

    @Test
    void statusFollowsTheChallengeIntoExpiredAndLocked() {
        HomeSabhaTransfer expired = initiated();
        assertThatThrownBy(() -> expired.confirm(CODE, NOW.plus(OtpChallenge.TTL).plusSeconds(1), HASHER))
                .isInstanceOf(OtpExpiredException.class);
        assertThat(expired.status()).isEqualTo(TransferStatus.EXPIRED);

        HomeSabhaTransfer locked = initiated();
        for (int attempt = 0; attempt < OtpChallenge.MAX_ATTEMPTS - 1; attempt++) {
            assertThatThrownBy(() -> locked.confirm("000000", NOW, HASHER))
                    .isInstanceOf(WrongOtpException.class);
        }
        assertThatThrownBy(() -> locked.confirm("000000", NOW, HASHER))
                .isInstanceOf(OtpAttemptsExhaustedException.class);
        assertThat(locked.status()).isEqualTo(TransferStatus.LOCKED);
    }

    @Test
    void recordSwapNamesBothEndsOfTheMoveTheServiceJustCommitted() {
        HomeSabhaTransfer transfer = initiated();
        transfer.confirm(CODE, NOW, HASHER);
        transfer.pullDomainEvents();
        Instant swappedAt = NOW.plus(Duration.ofSeconds(1));

        transfer.recordSwap(PREVIOUS_SABHA, swappedAt);

        List<DomainEvent> events = transfer.pullDomainEvents();
        assertThat(events).singleElement().isInstanceOf(HomeSabhaSwapped.class);
        HomeSabhaSwapped swapped = (HomeSabhaSwapped) events.get(0);
        assertThat(swapped.aggregateId()).isEqualTo(TRANSFER);
        assertThat(swapped.personId()).isEqualTo(PERSON);
        assertThat(swapped.previousSabhaId()).isEqualTo(PREVIOUS_SABHA);
        assertThat(swapped.destinationSabhaId()).isEqualTo(DESTINATION_SABHA);
        assertThat(swapped.occurredAt()).isEqualTo(swappedAt);
    }

    @Test
    void rehydrateIsTheExactInverseOfStatusAndRegistersNoEvents() {
        for (TransferStatus status : TransferStatus.values()) {
            HomeSabhaTransfer transfer = HomeSabhaTransfer.rehydrate(
                    TRANSFER, PERSON, MOBILE, DESTINATION_SABHA, INITIATOR,
                    HASHER.hash(TRANSFER, CODE), NOW, NOW.plus(OtpChallenge.TTL), status, 1);

            assertThat(transfer.status()).isEqualTo(status);
            assertThat(transfer.pullDomainEvents()).isEmpty();
        }
    }

    @Test
    void aRehydratedPendingTransferStillConfirmsAgainstTheStoredHash() {
        HomeSabhaTransfer transfer = HomeSabhaTransfer.rehydrate(
                TRANSFER, PERSON, MOBILE, DESTINATION_SABHA, INITIATOR,
                HASHER.hash(TRANSFER, CODE), NOW, NOW.plus(OtpChallenge.TTL), TransferStatus.PENDING, 0);

        transfer.confirm(CODE, NOW, HASHER);

        assertThat(transfer.status()).isEqualTo(TransferStatus.CONFIRMED);
    }

    private static HomeSabhaTransfer initiated() {
        return HomeSabhaTransfer.initiate(
                TRANSFER, PERSON, MOBILE, DESTINATION_SABHA, INITIATOR, CODE, NOW, HASHER);
    }
}
