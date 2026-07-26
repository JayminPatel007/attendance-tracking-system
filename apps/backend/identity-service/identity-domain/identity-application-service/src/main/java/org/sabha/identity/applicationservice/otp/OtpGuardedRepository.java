package org.sabha.identity.applicationservice.otp;

import java.util.Optional;
import java.util.UUID;

import org.sabha.identity.domain.OtpGuarded;

/**
 * The store {@link OtpGuardedFlow} drives on a feature's behalf: load the
 * aggregate a code is being entered against, upsert it, and — as an
 * {@link OtpSendLog} — answer the send budget's questions about the mobile.
 * Each OTP-guarded feature's own repository port extends this with whatever
 * else that feature needs.
 *
 * @param <A> the feature's OTP-guarded aggregate
 */
public interface OtpGuardedRepository<A extends OtpGuarded> extends OtpSendLog {

    /** Upsert keyed on the aggregate's id. */
    void save(A aggregate);

    Optional<A> findById(UUID id);
}
