package org.sabha.identity.applicationservice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.sabha.identity.domain.HomeSabhaRef;
import org.sabha.identity.domain.Person;

/**
 * Narrow driven port over the Directory's Home-Sabha membership, used by the
 * Verified Home Sabha Transfer orchestrator (ADR-0002). Kept separate from the
 * broad {@link PersonDirectory} so the orchestrator depends only on the four
 * operations it needs; the JDBC adapter implements both ports on one class.
 */
public interface HomeSabhaDirectory {

    /** The Person being transferred — read for their registered mobile. */
    Optional<Person> findById(UUID personId);

    /** The Person's current Home Sabhas, each tagged with its {@code sabha_kind}. */
    List<HomeSabhaRef> homeSabhasOf(UUID personId);

    /** The {@code sabha_kind} of a Sabha — the demographic+track the swap matches on. */
    Optional<String> kindOf(UUID sabhaId);

    /**
     * Atomically removes the Person's Home Sabha {@code previousSabhaId} and
     * inserts {@code destinationSabhaId}, leaving their other Home Sabhas intact.
     */
    void replaceHomeSabha(UUID personId, UUID previousSabhaId, UUID destinationSabhaId);
}
