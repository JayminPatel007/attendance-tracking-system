package org.sabha.attendance.applicationservice;

import java.time.Duration;
import java.time.Instant;

import org.sabha.common.ConflictException;

/**
 * Raised when the mobile client posts a sync batch whose roster snapshot is
 * older than the 7-day freshness window (ADR-0007). The caller must refresh
 * the Roster before re-attempting the sync.
 */
public class StaleRosterException extends ConflictException {
    private final Instant clientRosterVersion;
    private final Instant serverNow;
    private final Duration maxAge;

    public StaleRosterException(Instant clientRosterVersion, Instant serverNow, Duration maxAge) {
        super("Roster snapshot at " + clientRosterVersion + " is older than " + maxAge
                + " relative to " + serverNow, "ROSTER_STALE");
        this.clientRosterVersion = clientRosterVersion;
        this.serverNow = serverNow;
        this.maxAge = maxAge;
    }

    public Instant clientRosterVersion() {
        return clientRosterVersion;
    }

    public Instant serverNow() {
        return serverNow;
    }

    public Duration maxAge() {
        return maxAge;
    }
}
