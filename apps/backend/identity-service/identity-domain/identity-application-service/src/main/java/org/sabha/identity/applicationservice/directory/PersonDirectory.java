package org.sabha.identity.applicationservice.directory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.sabha.identity.domain.Person;

/**
 * Driven port over the Person Directory (ADR-0013). The JDBC implementation
 * lives in {@code identity-data-access}; unit tests drive an in-memory fake.
 */
public interface PersonDirectory {

    /** Exact match on the system-wide-unique own-mobile — drives the hard block. */
    Optional<Person> findByMobile(String mobile);

    Optional<Person> findById(UUID personId);

    /**
     * Phonetic + edit-distance name candidates within {@code kshetraId}, capped
     * at {@code limit} (ADR-0013 soft-warn). Empty when nothing is close enough.
     */
    List<NameCandidate> findNameCandidates(UUID kshetraId, String fullName, int limit);

    /**
     * Exact mobile match shaped for the Walk-in confirm sheet (Slice 7): like
     * {@link #findByMobile} but joined to the Person's current Home Sabha.
     */
    Optional<WalkInCandidate> findByMobileForWalkIn(String mobile);

    /** The Kshetra the given Sabha belongs to — the scope for the name soft-warn. */
    Optional<UUID> kshetraIdOfSabha(UUID sabhaId);

    /** Persists a new Person and registers their chosen Home Sabha, in one tx. */
    void add(Person person, UUID homeSabhaId);
}
