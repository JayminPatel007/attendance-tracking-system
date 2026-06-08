package org.sabha.identity.domain;

/**
 * Lifecycle of a self-service {@link PasswordReset} (ADR-0004). Opens at
 * {@code PENDING} when the OTP is sent; later slices of the red-green loop add
 * the verified / completed / expired / locked terminal states.
 */
public enum PasswordResetStatus {
    PENDING,
    VERIFIED,
    COMPLETED,
    EXPIRED,
    LOCKED
}
