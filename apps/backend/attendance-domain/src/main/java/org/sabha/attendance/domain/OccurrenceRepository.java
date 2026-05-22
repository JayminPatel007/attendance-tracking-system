package org.sabha.attendance.domain;

import java.util.Optional;
import java.util.UUID;

public interface OccurrenceRepository {

    Optional<Occurrence> findById(UUID occurrenceId);
}
