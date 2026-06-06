package org.sabha.identity.applicationservice;

import java.util.List;
import java.util.UUID;

/**
 * Read port backing the demographic Nirdeshak's pending-nomination queue
 * (ADR-0006). Scoped to the caller's NIRDESHAK role rows: only nominations whose
 * (Kshetra, demographic) the caller is Nirdeshak over are returned — the
 * track-shared higher tier sees both Regular-sourced BSS and YSS nominations for
 * their demographic.
 */
public interface SelectionQueries {

    /** The PENDING nominations the given user may decide as a demographic Nirdeshak. */
    List<PendingNominationItem> pendingQueueFor(UUID nirdeshakUserId);
}
