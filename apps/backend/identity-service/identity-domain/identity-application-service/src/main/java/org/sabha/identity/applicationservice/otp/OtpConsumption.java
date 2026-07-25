package org.sabha.identity.applicationservice.otp;

import java.time.Instant;

import org.sabha.identity.domain.OtpGuarded;
import org.sabha.identity.domain.OtpHasher;

/**
 * What a feature does when a code is entered: consume it through the aggregate
 * (under the feature's own vocabulary — {@code verify}, {@code confirm}) and then
 * apply the success effect. Both halves run inside
 * {@link OtpGuardedFlow#consume}'s transaction, and the split between them
 * matters — see that method for which failures persist and which roll back.
 *
 * @param <A> the feature's OTP-guarded aggregate
 * @param <R> what the caller wants handed back, or {@code null} for a flow that
 *            produces nothing
 */
@FunctionalInterface
public interface OtpConsumption<A extends OtpGuarded, R> {

    R consume(A aggregate, Instant now, OtpHasher hasher);
}
