package org.sabha.attendance.applicationservice;

import java.util.UUID;

import org.sabha.common.DomainException;

/**
 * Raised when an Occurrence reopen is attempted without a reason (ADR-0001:
 * every reopen records reopener, timestamp, and a free-text reason). Mapped to
 * HTTP 422.
 */
public class ReopenReasonRequiredException extends DomainException {

    private final UUID occurrenceId;

    public ReopenReasonRequiredException(UUID occurrenceId) {
        super("Reopening occurrence " + occurrenceId + " requires a reason");
        this.occurrenceId = occurrenceId;
    }

    public UUID occurrenceId() {
        return occurrenceId;
    }
}
