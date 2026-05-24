package org.sabha.attendance.applicationservice;

import java.util.UUID;

import org.sabha.common.NotFoundException;

public class OccurrenceNotFoundException extends NotFoundException {

    private final UUID occurrenceId;

    public OccurrenceNotFoundException(UUID occurrenceId) {
        super("Occurrence not found: " + occurrenceId);
        this.occurrenceId = occurrenceId;
    }

    public UUID occurrenceId() {
        return occurrenceId;
    }
}
