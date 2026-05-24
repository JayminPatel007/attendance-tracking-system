package org.sabha.attendance.domain;

import java.util.UUID;

import org.sabha.common.DomainException;

public class OccurrenceNotOpenForMarkingException extends DomainException {

    private final UUID occurrenceId;
    private final OccurrenceState state;

    public OccurrenceNotOpenForMarkingException(UUID occurrenceId, OccurrenceState state) {
        super("Occurrence " + occurrenceId + " is not open for marking (state=" + state + ")");
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
