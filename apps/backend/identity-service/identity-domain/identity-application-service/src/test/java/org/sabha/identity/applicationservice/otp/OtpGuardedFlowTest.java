package org.sabha.identity.applicationservice.otp;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.DomainEvent;
import org.sabha.common.DomainEventPublisher;
import org.sabha.identity.domain.OtpAttemptsExhaustedException;
import org.sabha.identity.domain.OtpChallenge;
import org.sabha.identity.domain.OtpExpiredException;
import org.sabha.identity.domain.OtpGuarded;
import org.sabha.identity.domain.OtpHasher;
import org.sabha.identity.domain.WrongOtpException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The shared OTP-guarded flow (issue #130), exercised against a stand-in
 * aggregate rather than either of the two real features — the point of the module
 * is that it knows nothing about password reset or Home Sabha transfer.
 */
class OtpGuardedFlowTest {

    private static final UUID AGGREGATE = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
    private static final String MOBILE = "+919820100200";
    private static final String CODE = "424242";
    private static final Instant NOW = Instant.parse("2026-07-25T10:00:00Z");

    @Test
    void beginConsultsTheSendPolicyWithTheCallersSendLogBeforeGeneratingAnything() {
        Fixture f = new Fixture();

        f.flow().begin(MOBILE, f.store, Stub::issue);

        assertThat(f.policy.mobile).isEqualTo(MOBILE);
        assertThat(f.policy.log).isSameAs(f.store);
        assertThat(f.policy.now).isEqualTo(NOW);
    }

    @Test
    void beginSendsTheGeneratedCodeMarksItSentSavesAndPublishes() {
        Fixture f = new Fixture();

        Stub issued = f.flow().begin(MOBILE, f.store, Stub::issue);

        assertThat(f.gateway.sentTo).isEqualTo(MOBILE);
        assertThat(f.gateway.sentCode).isEqualTo(CODE);
        assertThat(issued.otpSentAt).isEqualTo(NOW);
        assertThat(f.store.findById(issued.id())).containsSame(issued);
        assertThat(f.publisher.events).extracting(DomainEvent::aggregateId).containsExactly(issued.id());
        // The aggregate is handed the hasher, so only a digest is retained.
        assertThat(issued.challenge.codeHash()).isNotEqualTo(CODE);
    }

    @Test
    void beginSendsNothingAndSavesNothingWhenThePolicyDeniesTheSend() {
        Fixture f = new Fixture();
        f.policy.toThrow = new OtpRateLimitExceededException(MOBILE);

        assertThatThrownBy(() -> f.flow().begin(MOBILE, f.store, Stub::issue))
                .isInstanceOf(OtpRateLimitExceededException.class);

        assertThat(f.gateway.sentCode).isNull();
        assertThat(f.store.saved).isEmpty();
        assertThat(f.publisher.events).isEmpty();
    }

    @Test
    void consumeAppliesTheSuccessEffectSavesPublishesAndReturnsTheCallersResult() {
        Fixture f = new Fixture();
        Stub stub = f.flow().begin(MOBILE, f.store, Stub::issue);
        f.publisher.events.clear();
        f.store.saved.clear();

        String outcome = f.flow().consume(stub.id(), f.store, (aggregate, now, hasher) -> {
            aggregate.verify(CODE, now, hasher);
            return "effect@" + now;
        });

        assertThat(outcome).isEqualTo("effect@" + NOW);
        assertThat(stub.verified).isTrue();
        assertThat(f.store.saved).containsExactly(stub.id());
        assertThat(f.publisher.events).hasSize(1);
    }

    @Test
    void aRejectedOtpStillSavesItsConsequenceSoTheAttemptBudgetAccumulates() {
        Fixture f = new Fixture();
        Stub stub = f.flow().begin(MOBILE, f.store, Stub::issue);
        f.publisher.events.clear();
        f.store.saved.clear();

        assertThatThrownBy(() -> f.flow().consume(stub.id(), f.store, (aggregate, now, hasher) -> {
            aggregate.verify("000000", now, hasher);
            return "unreachable";
        })).isInstanceOf(WrongOtpException.class);

        assertThat(stub.challenge.attempts()).isEqualTo(1);
        assertThat(f.store.saved).containsExactly(stub.id());
        // The rejection is not a domain event; nothing is published.
        assertThat(f.publisher.events).isEmpty();
    }

    /**
     * The rollback exemption is what makes the save above survive: it is declared
     * once, here, and must stay narrow — a success-effect failure has to roll back
     * so it can never leave a half-applied effect behind.
     */
    @Test
    void consumeExemptsExactlyTheThreeOtpRejectionsFromRollback() throws NoSuchMethodException {
        Method consume = OtpGuardedFlow.class.getMethod(
                "consume", UUID.class, OtpGuardedRepository.class, OtpConsumption.class);

        Transactional tx = consume.getAnnotation(Transactional.class);

        assertThat(tx).isNotNull();
        assertThat(tx.noRollbackFor()).containsExactlyInAnyOrder(
                WrongOtpException.class, OtpExpiredException.class, OtpAttemptsExhaustedException.class);
    }

    @Test
    void aSuccessEffectFailureAfterTheOtpIsConsumedSavesNothingAndPublishesNothing() {
        Fixture f = new Fixture();
        Stub stub = f.flow().begin(MOBILE, f.store, Stub::issue);
        f.publisher.events.clear();
        f.store.saved.clear();

        assertThatThrownBy(() -> f.flow().consume(stub.id(), f.store, (aggregate, now, hasher) -> {
            aggregate.verify(CODE, now, hasher);
            throw new IllegalStateException("the effect failed");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(f.store.saved).isEmpty();
        assertThat(f.publisher.events).isEmpty();
    }

    @Test
    void consumingAnUnknownAggregateIsRejectedBeforeAnyEffect() {
        Fixture f = new Fixture();

        assertThatThrownBy(() -> f.flow().consume(AGGREGATE, f.store, (aggregate, now, hasher) -> "unreachable"))
                .isInstanceOf(NoSuchElementException.class);

        assertThat(f.store.saved).isEmpty();
    }

    // ---- test fixtures -------------------------------------------------------

    /** Wires the flow against fakes for every port it drives. */
    static final class Fixture {
        final RecordingOtpSendPolicy policy = new RecordingOtpSendPolicy();
        final OtpFlowFixture.RecordingOtpGateway gateway = new OtpFlowFixture.RecordingOtpGateway();
        final RecordingPublisher publisher = new RecordingPublisher();
        final InMemoryStore store = new InMemoryStore();
        final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        OtpGuardedFlow flow() {
            return new OtpGuardedFlow(
                    policy, () -> CODE, gateway, OtpFlowFixture.HASHER, publisher, clock);
        }
    }

    /**
     * A minimal OTP-guarded aggregate: enough state machine to prove the flow
     * drives it, and nothing from either real feature.
     */
    static final class Stub implements OtpGuarded {
        private final UUID id = UUID.randomUUID();
        private final List<DomainEvent> events = new ArrayList<>();
        final OtpChallenge challenge;
        Instant otpSentAt;
        boolean verified;

        private Stub(OtpChallenge challenge) {
            this.challenge = challenge;
        }

        static Stub issue(String otpCode, Instant now, OtpHasher hasher) {
            return new Stub(OtpChallenge.issue(UUID.randomUUID(), otpCode, now, hasher));
        }

        void verify(String candidate, Instant now, OtpHasher hasher) {
            challenge.verify(candidate, now, hasher);
            verified = true;
            events.add(new StubEvent(id, now));
        }

        UUID id() {
            return id;
        }

        @Override
        public void markOtpSent(Instant now) {
            otpSentAt = now;
            events.add(new StubEvent(id, now));
        }

        @Override
        public List<DomainEvent> pullDomainEvents() {
            List<DomainEvent> drained = List.copyOf(events);
            events.clear();
            return drained;
        }
    }

    record StubEvent(UUID aggregateId, Instant occurredAt) implements DomainEvent {
    }

    static final class InMemoryStore implements OtpGuardedRepository<Stub> {
        private final Map<UUID, Stub> byId = new HashMap<>();
        final List<UUID> saved = new ArrayList<>();

        @Override
        public void save(Stub aggregate) {
            byId.put(aggregate.id(), aggregate);
            saved.add(aggregate.id());
        }

        @Override
        public Optional<Stub> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<Instant> lastInitiatedAt(String mobile) {
            return Optional.empty();
        }

        @Override
        public int sendCountSince(String mobile, Instant since) {
            return 0;
        }
    }

    static final class RecordingPublisher implements DomainEventPublisher {
        final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void publishAll(List<? extends DomainEvent> toPublish) {
            events.addAll(toPublish);
        }
    }
}
