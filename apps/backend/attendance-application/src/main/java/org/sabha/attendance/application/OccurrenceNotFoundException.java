package org.sabha.attendance.application;

import java.util.UUID;

public class OccurrenceNotFoundException extends RuntimeException {

    private final UUID occurrenceId;

    public OccurrenceNotFoundException(UUID occurrenceId) {
        super("Occurrence not found: " + occurrenceId);
        this.occurrenceId = occurrenceId;
    }

    public UUID occurrenceId() {
        return occurrenceId;
    }
}
