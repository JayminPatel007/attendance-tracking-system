package org.sabha.attendance.applicationservice;

import java.time.Instant;
import java.util.UUID;

import org.sabha.common.DomainException;

/**
 * Raised when a revert of a Cancelled Occurrence is attempted after the 24h
 * grace window past its scheduled end has closed (ADR-0001). Mapped to HTTP
 * 422 — the Occurrence has locked and the revert path is no longer available.
 */
public class RevertWindowExpiredException extends DomainException {

    private final UUID occurrenceId;
    private final Instant cutoff;

    public RevertWindowExpiredException(UUID occurrenceId, Instant cutoff) {
        super("Revert window for occurrence " + occurrenceId + " closed at " + cutoff);
        this.occurrenceId = occurrenceId;
        this.cutoff = cutoff;
    }

    public UUID occurrenceId() {
        return occurrenceId;
    }

    public Instant cutoff() {
        return cutoff;
    }
}
