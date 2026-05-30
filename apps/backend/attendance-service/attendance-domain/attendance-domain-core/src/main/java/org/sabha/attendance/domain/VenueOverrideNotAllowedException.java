package org.sabha.attendance.domain;

import java.util.UUID;

import org.sabha.common.DomainException;

/**
 * Raised when a venue override is attempted after the Occurrence has left the
 * pre-open shaping window (ADR-0001: allowed up until Open for Marking). Mapped
 * to HTTP 422 by the global exception handler.
 */
public class VenueOverrideNotAllowedException extends DomainException {

    private final UUID occurrenceId;
    private final OccurrenceState state;

    public VenueOverrideNotAllowedException(UUID occurrenceId, OccurrenceState state) {
        super("Occurrence " + occurrenceId + " cannot accept a venue override in state " + state);
        this.occurrenceId = occurrenceId;
        this.state = state;
    }

    public UUID occurrenceId() {
        return occurrenceId;
    }

    public OccurrenceState state() {
        return state;
    }
}
