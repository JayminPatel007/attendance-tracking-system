package org.sabha.identity.applicationservice.otp;

import java.time.Instant;

import org.sabha.identity.domain.OtpGuarded;
import org.sabha.identity.domain.OtpHasher;

/**
 * How a feature opens its aggregate around a freshly generated OTP — the only
 * thing {@link OtpGuardedFlow#begin} cannot do itself. The flow supplies the
 * code, the moment and the hasher; the feature supplies the rest of the
 * aggregate's identity.
 *
 * @param <A> the feature's OTP-guarded aggregate
 */
@FunctionalInterface
public interface OtpIssuance<A extends OtpGuarded> {

    A issue(String otpCode, Instant now, OtpHasher hasher);
}
