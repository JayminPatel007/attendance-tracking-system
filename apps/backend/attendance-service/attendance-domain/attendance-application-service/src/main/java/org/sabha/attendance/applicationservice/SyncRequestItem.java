package org.sabha.attendance.applicationservice;

import java.time.Instant;
import java.util.UUID;

public record SyncRequestItem(
        UUID occurrenceId,
        UUID personId,
        boolean present,
        Instant clientMarkedAt) {
}
