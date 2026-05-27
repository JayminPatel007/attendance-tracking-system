package org.sabha.attendance.applicationservice;

import java.time.LocalDate;
import java.util.UUID;

public record ScheduledOccurrenceRef(UUID occurrenceId, UUID sabhaId, LocalDate date) {
}
