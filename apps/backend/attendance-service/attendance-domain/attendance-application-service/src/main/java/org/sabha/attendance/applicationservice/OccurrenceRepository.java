package org.sabha.attendance.applicationservice;

import java.util.Optional;
import java.util.UUID;

import org.sabha.attendance.domain.Occurrence;

public interface OccurrenceRepository {

    Optional<Occurrence> findById(UUID occurrenceId);

    /**
     * Persist the aggregate, cascading to all its entities (markings). On
     * version conflict the implementation throws
     * {@link org.sabha.common.OptimisticLockException}.
     */
    void save(Occurrence occurrence);
}
