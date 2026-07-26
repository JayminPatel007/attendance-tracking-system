package org.sabha.identity.applicationservice.otp;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.sabha.common.DomainEventPublisher;
import org.sabha.identity.domain.OtpAttemptsExhaustedException;
import org.sabha.identity.domain.OtpExpiredException;
import org.sabha.identity.domain.OtpGuarded;
import org.sabha.identity.domain.OtpHasher;
import org.sabha.identity.domain.WrongOtpException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The one orchestration of an OTP-guarded flow (issue #130). Self-service
 * password reset (ADR-0004) and Verified Home Sabha Transfer (ADR-0002) both run
 * through here, so the two halves of the flow — issue a challenge and send it,
 * then consume a code and act on it — are written down exactly once.
 *
 * <p>Callers supply only what makes their flow different: who may start it and
 * where the mobile comes from (before calling {@link #begin}), how their
 * aggregate opens around the code, and what success does. The send budget, code
 * generation, gateway dispatch, hashing, persistence, event publication and the
 * rollback semantics below never diverge between them because there is one copy
 * of each.</p>
 *
 * <p>The {@link OtpChallenge} state machine one layer down is a different seam:
 * it owns TTL, attempt budget, lockout and expiry <em>inside</em> an aggregate.
 * This module owns the orchestration <em>around</em> it.</p>
 *
 * <p>The two halves sit differently in the transaction (ADR-0018), and the
 * difference is load-bearing. {@link #begin} simply <em>joins</em> whatever
 * transaction its caller opened. {@link #consume} must <em>own</em> its
 * transaction, because the rollback rules that let a rejected OTP keep its
 * consequence live on that method — a caller that wrapped it in a
 * {@code @Transactional} method of its own would substitute that method's rules.
 * An architecture rule holds callers to this.</p>
 *
 * @see org.sabha.identity.domain.OtpChallenge
 */
@Service
public class OtpGuardedFlow {

    private final OtpSendPolicy sendPolicy;
    private final OtpCodeGenerator codes;
    private final OtpGateway gateway;
    private final OtpHasher hasher;
    private final DomainEventPublisher events;
    private final Clock clock;

    public OtpGuardedFlow(
            OtpSendPolicy sendPolicy,
            OtpCodeGenerator codes,
            OtpGateway gateway,
            OtpHasher hasher,
            DomainEventPublisher events,
            Clock clock) {
        this.sendPolicy = sendPolicy;
        this.codes = codes;
        this.gateway = gateway;
        this.hasher = hasher;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Opens a challenge and sends its code to {@code mobile}: the send budget is
     * enforced first, so a denied send generates no code and leaves no aggregate
     * behind. Only a hash of the code reaches the aggregate (issue #77) — the
     * plaintext goes to the gateway and nowhere else.
     *
     * @param store the feature's repository, doubling as the {@link OtpSendLog}
     *              the budget is counted against
     * @param issue how the feature opens its aggregate around the generated code
     * @return the saved aggregate, its events already published
     */
    @Transactional
    public <A extends OtpGuarded> A begin(String mobile, OtpGuardedRepository<A> store, OtpIssuance<A> issue) {
        Instant now = clock.instant();
        sendPolicy.enforce(mobile, store, now);

        String code = codes.generate();
        A aggregate = issue.issue(code, now, hasher);

        gateway.send(mobile, code);
        aggregate.markOtpSent(now);
        store.save(aggregate);
        events.publishAll(aggregate.pullDomainEvents());
        return aggregate;
    }

    /**
     * Loads the aggregate a code is being entered against and runs the caller's
     * {@code consumption} — the code check plus whatever success means for that
     * feature — then saves and publishes.
     *
     * <p>A rejected OTP must still persist its consequence (the incremented
     * attempt count, the EXPIRED / LOCKED status) so the attempt budget
     * accumulates across calls rather than resetting on every wrong guess: the
     * three OTP rejections are therefore saved explicitly <em>and</em> exempted
     * from transaction rollback. The exemption is deliberately narrow — a failure
     * in the success effect after the OTP was consumed (a
     * {@code NoMatchingHomeSabhaException}, say) rolls back normally, so it can
     * never leave a half-applied effect behind.</p>
     *
     * <p>Because the rollback rules live on this method, it is the transaction
     * boundary for the whole consume step; callers must not wrap it in a
     * {@code @Transactional} method of their own, whose rules would decide the
     * rollback instead.</p>
     *
     * @return whatever {@code consumption} produced
     */
    @Transactional(noRollbackFor = {
            WrongOtpException.class,
            OtpExpiredException.class,
            OtpAttemptsExhaustedException.class })
    public <A extends OtpGuarded, R> R consume(UUID aggregateId, OtpGuardedRepository<A> store,
                                               OtpConsumption<A, R> consumption) {
        A aggregate = store.findById(aggregateId).orElseThrow();

        R result;
        try {
            result = consumption.consume(aggregate, clock.instant(), hasher);
        } catch (WrongOtpException | OtpExpiredException | OtpAttemptsExhaustedException rejected) {
            store.save(aggregate);
            throw rejected;
        }

        store.save(aggregate);
        events.publishAll(aggregate.pullDomainEvents());
        return result;
    }
}
