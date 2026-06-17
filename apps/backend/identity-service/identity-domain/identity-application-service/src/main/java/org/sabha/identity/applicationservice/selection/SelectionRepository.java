package org.sabha.identity.applicationservice.selection;

import java.util.Optional;
import java.util.UUID;

import org.sabha.identity.domain.SelectionNomination;

/**
 * Driven port over the {@code selection_nominations} store backing the BSS/YSS
 * selection workflow (ADR-0006). The nomination record doubles as the audit
 * trail, so the repository persists the full lifecycle (nominate → approve/reject)
 * and answers the de-duplication question the orchestrator asks before opening a
 * new nomination.
 */
public interface SelectionRepository {

    void save(SelectionNomination nomination);

    Optional<SelectionNomination> findById(UUID nominationId);

    /** Whether a {@code PENDING} nomination already exists for this Person on this track. */
    boolean hasPendingFor(UUID personId, String track);

    /** The currently {@code APPROVED} nomination tying the Person to the selective Sabha, for deselection. */
    Optional<SelectionNomination> findApproved(UUID personId, UUID selectiveSabhaId);
}
