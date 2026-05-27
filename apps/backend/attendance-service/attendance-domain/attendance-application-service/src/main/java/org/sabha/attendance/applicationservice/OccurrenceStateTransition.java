package org.sabha.attendance.applicationservice;

import java.time.Instant;
import java.util.UUID;

import org.sabha.attendance.domain.OccurrenceState;

public record OccurrenceStateTransition(
        UUID id,
        UUID occurrenceId,
        OccurrenceState fromState,
        OccurrenceState toState,
        OccurrenceAction action,
        ActorKind actorKind,
        UUID actorUserId,
        String reason,
        Instant at) {
}
