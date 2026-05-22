package org.sabha.attendance.domain;

import java.time.LocalDate;
import java.util.UUID;

public record Occurrence(UUID id, UUID sabhaId, LocalDate date, OccurrenceState state) {

    public boolean isOpenForMarking() {
        return state == OccurrenceState.OPEN_FOR_MARKING;
    }
}
