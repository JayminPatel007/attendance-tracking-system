package org.sabha.identity.domain;

/**
 * Lifecycle of a {@link HomeSabhaTransfer} (ADR-0002). A transfer starts
 * {@code PENDING} when the Sanchalak initiates and the OTP is sent; it reaches a
 * terminal state when the Person confirms ({@code CONFIRMED}), the OTP TTL lapses
 * ({@code EXPIRED}), or the attempt budget is exhausted ({@code LOCKED}).
 */
public enum TransferStatus {
    PENDING,
    CONFIRMED,
    EXPIRED,
    LOCKED
}
