/**
 * Shared OTP machinery (gateway, code generation, send policy and its rate-limit / cooldown signals) used by both password reset and Verified Home Sabha Transfer.
 *
 * <p>Feature subpackage of the Identity use-case ring (ADR-0019 + ADR-0020);
 * see the parent {@link org.sabha.identity.applicationservice} package.
 */
package org.sabha.identity.applicationservice.otp;
