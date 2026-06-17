package org.sabha.identity.applicationservice.directory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.sabha.identity.domain.Person;
import org.springframework.stereotype.Service;

/**
 * Directory search (ADR-0013 "search Persons") backing the mobile-add-person
 * flow: an exact mobile lookup (step 1) and a Kshetra-scoped name lookup that
 * reuses the same phonetic/edit-distance matcher as the add soft-warn.
 */
@Service
public class SearchDirectoryUseCase {

    private static final int MAX_RESULTS = 5;

    private final PersonDirectory directory;

    public SearchDirectoryUseCase(PersonDirectory directory) {
        this.directory = directory;
    }

    public Optional<Person> byMobile(String mobile) {
        return directory.findByMobile(mobile);
    }

    public List<NameCandidate> byName(UUID kshetraId, String name) {
        return directory.findNameCandidates(kshetraId, name, MAX_RESULTS);
    }
}
