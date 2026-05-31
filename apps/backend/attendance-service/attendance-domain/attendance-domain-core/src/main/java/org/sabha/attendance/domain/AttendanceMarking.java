package org.sabha.attendance.domain;

import java.time.Instant;
import java.util.UUID;

public record AttendanceMarking(
        UUID id,
        UUID occurrenceId,
        UUID personId,
        boolean present,
        UUID markedByUserId,
        Instant clientMarkedAt,
        MarkingType markingType) {
}
