package org.sabha.attendance.applicationservice;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-side port: for a given Sanchalak {@code users.id}, find the Occurrence on
 * the Sabha they preside over that is currently shapeable — i.e. still
 * {@code SCHEDULED} / {@code RESCHEDULED}, or {@code CANCELLED} and thus
 * revertable (ADR-0001). The adapter lives in {@code attendance-data-access} and
 * joins identity and attendance tables — a legitimate cross-context <i>read</i>
 * projection (ADR-0019); cross-context writes are not permitted from this port.
 */
public interface CurrentOccurrenceQuery {

    Optional<CurrentOccurrence> findShapeableForSanchalak(UUID sanchalakUserId);
}
