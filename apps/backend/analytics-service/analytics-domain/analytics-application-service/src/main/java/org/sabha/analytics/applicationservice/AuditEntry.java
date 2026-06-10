package org.sabha.analytics.applicationservice;

import java.time.Instant;
import java.util.UUID;

/**
 * One row of the audit feed (ADR-0023, Slice 19): a single audited act,
 * projected into a common shape from whichever source table recorded it. The
 * actor and on-behalf-of names are resolved from {@code users.username} for
 * display; a {@code null} actor name is a system act (e.g. an auto-materialised
 * Occurrence transition). {@code onBehalfOfUserId} is non-null only for a
 * Nirikshak-as-Sanchalak proxy action (Slice 14), which only ever occurs on an
 * {@link AuditTargetType#OCCURRENCE} entry.
 *
 * <p>A single source row may emit two entries (a selection nomination emits both
 * a nominate and a decide entry), so {@code id} is not unique across the feed;
 * consumers key on {@code (id, action)}.</p>
 */
public record AuditEntry(
        UUID id,
        Instant at,
        UUID actorUserId,
        String actorName,
        UUID onBehalfOfUserId,
        String onBehalfName,
        AuditTargetType targetType,
        UUID targetId,
        String action,
        String detail) {
}
