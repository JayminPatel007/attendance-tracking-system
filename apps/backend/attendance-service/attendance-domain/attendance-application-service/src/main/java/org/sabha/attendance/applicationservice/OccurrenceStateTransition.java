package org.sabha.attendance.applicationservice;

import java.time.Instant;
import java.util.UUID;

import org.sabha.attendance.domain.OccurrenceState;

/**
 * One row of the Occurrence audit log (Slice 3). {@code actorUserId} is the User
 * who performed the transition; {@code onBehalfOfUserId} is the absent Sanchalak a
 * Nirikshak acted as a proxy for (Slice 14), or {@code null} when the actor was
 * acting under their own authority. Filtering {@code onBehalfOfUserId IS NOT NULL}
 * surfaces every proxy action.
 */
public record OccurrenceStateTransition(
        UUID id,
        UUID occurrenceId,
        OccurrenceState fromState,
        OccurrenceState toState,
        OccurrenceAction action,
        ActorKind actorKind,
        UUID actorUserId,
        UUID onBehalfOfUserId,
        String reason,
        Instant at) {
}
