package org.sabha.attendance.domain;

import java.util.UUID;

public record AttendanceMarking(
        UUID id,
        UUID occurrenceId,
        UUID personId,
        boolean present,
        UUID markedByUserId) {
}
