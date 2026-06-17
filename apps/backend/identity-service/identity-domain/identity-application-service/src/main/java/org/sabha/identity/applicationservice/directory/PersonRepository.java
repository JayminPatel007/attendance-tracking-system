package org.sabha.identity.applicationservice.directory;

import org.sabha.identity.domain.Person;

/**
 * Driven port for persisting Directory {@link Person} records. Adapter in
 * {@code identity-data-access} (ADR-0019).
 */
public interface PersonRepository {

    void save(Person person);
}
