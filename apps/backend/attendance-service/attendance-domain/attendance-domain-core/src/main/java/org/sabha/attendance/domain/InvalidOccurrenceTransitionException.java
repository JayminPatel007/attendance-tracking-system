package org.sabha.attendance.domain;

import java.util.UUID;

public class InvalidOccurrenceTransitionException extends RuntimeException {

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
