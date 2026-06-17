package org.sabha.identity.applicationservice.directory;

import java.util.Optional;
import java.util.UUID;

import org.sabha.identity.domain.Person;
import org.springframework.stereotype.Service;

/**
 * Reads a single Person's Directory profile by id (ADR-0013 "get Person
 * detail"). Always online per ADR-0007.
 */
@Service
public class GetPersonDetailUseCase {

    private final PersonDirectory directory;

    public GetPersonDetailUseCase(PersonDirectory directory) {
        this.directory = directory;
    }

    public Optional<Person> byId(UUID personId) {
        return directory.findById(personId);
    }
}
