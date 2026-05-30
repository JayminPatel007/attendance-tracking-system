package org.sabha.attendance.applicationservice;

import java.util.UUID;

import org.sabha.common.DomainException;

/**
 * Raised when an Occurrence cancellation is attempted without a reason
 * (ADR-0001: cancel requires a reason). Mapped to HTTP 422.
 */
public class CancellationReasonRequiredException extends DomainException {

    private final UUID occurrenceId;

    public CancellationReasonRequiredException(UUID occurrenceId) {
        super("Cancelling occurrence " + occurrenceId + " requires a reason");
        this.occurrenceId = occurrenceId;
    }

    public UUID occurrenceId() {
        return occurrenceId;
    }
}
