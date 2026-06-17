package org.sabha.identity.applicationservice.selection;

import java.time.Instant;
import java.util.UUID;

/**
 * A row in the demographic Nirdeshak's pending-nomination queue (ADR-0006): the
 * Person nominated, the selective Sabha they would join, who nominated them, and
 * when. Read-model shape for the web BFF — denormalized with the Person and
 * nominator names so the queue renders without further lookups.
 */
public record PendingNominationItem(
        UUID nominationId,
        UUID personId,
        String personName,
        UUID regularSabhaId,
        UUID selectiveSabhaId,
        String demographic,
        String track,
        UUID nominatedBy,
        String nominatedByName,
        Instant nominatedAt) {
}
