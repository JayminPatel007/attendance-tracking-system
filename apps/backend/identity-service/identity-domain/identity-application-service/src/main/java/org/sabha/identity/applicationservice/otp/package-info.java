/**
 * The shared OTP-guarded flow (issue #130) and the machinery it drives: the
 * {@link org.sabha.identity.applicationservice.otp.OtpGuardedFlow orchestration}
 * both OTP features run through, the send policy with its rate-limit / cooldown
 * signals, and the gateway and code-generation ports.
 *
 * <p>Password reset (ADR-0004) and Verified Home Sabha Transfer (ADR-0002) supply
 * only their own authorization and success effect; everything they would
 * otherwise both spell out lives here.
 *
 * <p>Feature subpackage of the Identity use-case ring (ADR-0019 + ADR-0020);
 * see the parent {@link org.sabha.identity.applicationservice} package.
 */
package org.sabha.identity.applicationservice.otp;
