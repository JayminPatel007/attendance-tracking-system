package org.sabha.attendance.applicationservice;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-side port: for a given Sanchalak {@code users.id}, find the current
 * Occurrence that is open for marking on the Sabha they preside over, along
 * with the roster of expected attendees and any markings already made.
 *
 * <p>The adapter (in {@code attendance-data-access}) joins across identity,
 * sabha, and attendance tables — a legitimate cross-context <i>read</i>
 * projection. Cross-context <i>writes</i> are not permitted from this port.</p>
 */
public interface CurrentRosterQuery {

    Optional<CurrentRoster> findForSanchalak(UUID sanchalakUserId);
}
