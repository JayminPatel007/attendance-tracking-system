package org.sabha.attendance.applicationservice;

import java.time.LocalDate;
import java.util.UUID;

public record OpenOccurrenceRef(UUID occurrenceId, UUID sabhaId, LocalDate date) {
}
