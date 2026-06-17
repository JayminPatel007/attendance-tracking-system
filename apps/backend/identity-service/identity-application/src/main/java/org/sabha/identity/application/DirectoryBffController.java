package org.sabha.identity.application;

import java.time.LocalDate;
import java.util.UUID;

import org.sabha.identity.applicationservice.directory.SearchDirectoryUseCase;
import org.sabha.identity.domain.Gender;
import org.sabha.identity.domain.Person;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Directory search for the web shell (ADR-0013, ADR-0022): an exact mobile lookup
 * and a Kshetra-scoped name lookup, backing the directory-first candidate list of
 * flows like role appointment (Slice 11). The mobile-bearer twin of this search
 * lives in {@code PersonDirectoryRestController} on the {@code /api} chain; this
 * is the cookie-session {@code /bff} surface. Both delegate to the one
 * {@link SearchDirectoryUseCase}, so the search logic itself is not duplicated.
 */
@RestController
public class DirectoryBffController {

    private final SearchDirectoryUseCase searchDirectory;

    public DirectoryBffController(SearchDirectoryUseCase searchDirectory) {
        this.searchDirectory = searchDirectory;
    }

    @GetMapping("/bff/directory/search")
    public ResponseEntity<Object> search(
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) UUID kshetraId) {
        if (mobile != null && !mobile.isBlank()) {
            return searchDirectory.byMobile(mobile)
                    .<ResponseEntity<Object>>map(p -> ResponseEntity.ok(PersonResponse.of(p)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }
        if (name != null && !name.isBlank() && kshetraId != null) {
            return ResponseEntity.ok(searchDirectory.byName(kshetraId, name));
        }
        return ResponseEntity.badRequest().build();
    }

    public record PersonResponse(
            UUID id, String fullName, Gender gender, LocalDate dateOfBirth, String mobile, UUID guardianPersonId) {

        static PersonResponse of(Person person) {
            return new PersonResponse(person.id(), person.fullName(), person.gender(),
                    person.dateOfBirth(), person.mobile(), person.guardianPersonId());
        }
    }
}
