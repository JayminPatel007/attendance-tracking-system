package org.sabha.analytics.applicationservice;

import java.time.Instant;
import java.util.UUID;

/**
 * The filters a caller applies to the audit feed (ADR-0023, Slice 19). Every
 * field is optional (a {@code null} means "no constraint"); {@link #from} is
 * inclusive and {@link #to} is exclusive (the BFF maps a calendar end-date to the
 * start of the following day). {@link #proxyOnly} keeps only entries with an
 * on-behalf-of attribution — which, by construction, are all Occurrence
 * transitions (Slice 14). {@link #targetId} is set when the viewer drills into a
 * single entity's history.
 */
public record AuditFilter(
        AuditTargetType targetType,
        UUID targetId,
        UUID actorUserId,
        String action,
        Instant from,
        Instant to,
        boolean proxyOnly,
        int limit) {

    /** The default page size when the caller does not narrow the window. */
    public static final int DEFAULT_LIMIT = 200;
}
