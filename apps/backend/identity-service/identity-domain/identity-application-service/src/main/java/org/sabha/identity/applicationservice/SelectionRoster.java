package org.sabha.identity.applicationservice;

import java.util.UUID;

/**
 * Narrow read port over Home-Sabha membership for the selection workflow
 * (ADR-0006): a nomination is only valid for a Person already on the nominating
 * Regular Sabha's Roster, and a Person already on the selective Sabha's Roster
 * cannot be re-nominated. Kept separate from the broad directory ports so the
 * orchestrator depends only on the membership question it needs.
 */
public interface SelectionRoster {

    /** Whether {@code personId} has {@code sabhaId} among their Home Sabhas. */
    boolean isOnRoster(UUID personId, UUID sabhaId);

    /**
     * Adds the selective Sabha to the Person's Home Sabhas on approval — additive,
     * leaving the Regular Home Sabha intact (ADR-0006). Idempotent.
     */
    void addHomeSabha(UUID personId, UUID sabhaId);

    /**
     * Removes the selective Sabha from the Person's Home Sabhas on deselection,
     * the inverse of approval; the Regular Home Sabha is unaffected (ADR-0006).
     */
    void removeHomeSabha(UUID personId, UUID sabhaId);
}
