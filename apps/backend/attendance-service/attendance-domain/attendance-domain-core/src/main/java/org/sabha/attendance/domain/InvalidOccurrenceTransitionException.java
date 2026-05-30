package org.sabha.attendance.domain;

import java.util.UUID;

import org.sabha.common.DomainException;

/**
 * Raised when an Occurrence state transition is attempted from a state that
 * does not permit it. Extends {@link DomainException} so user-driven invalid
 * transitions surface as HTTP 422 rather than a 500.
 */
public class InvalidOccurrenceTransitionException extends DomainException {

    private final UUID occurrenceId;
    private final OccurrenceState from;
    private final OccurrenceState to;

    public InvalidOccurrenceTransitionException(UUID occurrenceId, OccurrenceState from, OccurrenceState to) {
        super("Occurrence " + occurrenceId + " cannot transition from " + from + " to " + to);
        this.occurrenceId = occurrenceId;
        this.from = from;
        this.to = to;
    }

    public UUID occurrenceId() {
        return occurrenceId;
    }

    public OccurrenceState from() {
        return from;
    }

    public OccurrenceState to() {
        return to;
    }
}
