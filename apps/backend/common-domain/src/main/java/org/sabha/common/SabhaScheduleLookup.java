package org.sabha.common;

import java.util.Optional;
import java.util.UUID;

/**
 * Cross-context port (ADR-0019). The attendance context calls this to resolve
 * a Sabha's standing weekly schedule when computing scheduled start/end times
 * for cron-driven Occurrence transitions. The implementation lives in
 * sabha-data-access.
 */
public interface SabhaScheduleLookup {

    Optional<SabhaSchedule> findSchedule(UUID sabhaId);
}
