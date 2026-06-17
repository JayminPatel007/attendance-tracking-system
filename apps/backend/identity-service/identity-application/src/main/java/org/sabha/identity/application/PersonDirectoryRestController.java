package org.sabha.identity.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.sabha.identity.applicationservice.directory.AddPersonApplicationService;
import org.sabha.identity.applicationservice.directory.AddPersonCommand;
import org.sabha.identity.applicationservice.directory.AddResult;
import org.sabha.identity.applicationservice.directory.GetPersonDetailUseCase;
import org.sabha.identity.applicationservice.directory.NameCandidate;
import org.sabha.identity.applicationservice.directory.SearchDirectoryUseCase;
import org.sabha.identity.applicationservice.directory.SearchWalkInCandidatesUseCase;
import org.sabha.identity.applicationservice.directory.WalkInCandidate;
import org.sabha.identity.domain.Gender;
import org.sabha.identity.domain.Person;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Person Directory endpoints (ADR-0013): add a Person, search the Directory
 * (mobile exact / name fuzzy), and read a Person's detail. Mobile hard block
 * and the neither-mobile-nor-guardian rule surface through the domain
 * exceptions mapped in {@code GlobalExceptionHandler}; the name soft-warn comes
 * back as a {@code 200} candidate list. All endpoints are online-only (ADR-0007).
 */
@RestController
public class PersonDirectoryRestController {

    private final AddPersonApplicationService addPerson;
    private final SearchDirectoryUseCase searchDirectory;
    private final SearchWalkInCandidatesUseCase searchWalkInCandidates;
    private final GetPersonDetailUseCase getPersonDetail;

    public PersonDirectoryRestController(
            AddPersonApplicationService addPerson,
            SearchDirectoryUseCase searchDirectory,
            SearchWalkInCandidatesUseCase searchWalkInCandidates,
            GetPersonDetailUseCase getPersonDetail) {
        this.addPerson = addPerson;
        this.searchDirectory = searchDirectory;
        this.searchWalkInCandidates = searchWalkInCandidates;
        this.getPersonDetail = getPersonDetail;
    }

    @PostMapping("/api/directory/persons")
    public ResponseEntity<AddPersonResponse> add(
            @RequestBody AddPersonRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        UUID subject = UUID.fromString(jwt.getSubject());
        AddResult result = addPerson.add(subject, new AddPersonCommand(
                req.fullName(), req.gender(), req.dateOfBirth(), req.mobile(),
                req.guardianPersonId(), req.homeSabhaId(), req.overrideDuplicateWarning()));

        if (result.created()) {
            return ResponseEntity.status(201).body(AddPersonResponse.created(result.personId()));
        }
        return ResponseEntity.ok(AddPersonResponse.softWarn(result.candidates()));
    }

    @GetMapping("/api/directory/persons")
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

    @GetMapping("/api/directory/walk-in-search")
    public ResponseEntity<List<WalkInCandidate>> walkInSearch(
            @RequestParam UUID sabhaId,
            @RequestParam String q) {
        return ResponseEntity.ok(searchWalkInCandidates.search(sabhaId, q));
    }

    @GetMapping("/api/directory/persons/{id}")
    public ResponseEntity<PersonResponse> detail(@PathVariable UUID id) {
        return getPersonDetail.byId(id)
                .map(p -> ResponseEntity.ok(PersonResponse.of(p)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record AddPersonRequest(
            String fullName,
            Gender gender,
            LocalDate dateOfBirth,
            String mobile,
            UUID guardianPersonId,
            UUID homeSabhaId,
            boolean overrideDuplicateWarning) {
    }

    public record AddPersonResponse(UUID personId, List<NameCandidate> candidates, boolean requiresOverride) {
        static AddPersonResponse created(UUID personId) {
            return new AddPersonResponse(personId, List.of(), false);
        }

        static AddPersonResponse softWarn(List<NameCandidate> candidates) {
            return new AddPersonResponse(null, candidates, true);
        }
    }

    public record PersonResponse(
            UUID id,
            String fullName,
            Gender gender,
            LocalDate dateOfBirth,
            String mobile,
            UUID guardianPersonId) {

        static PersonResponse of(Person person) {
            return new PersonResponse(person.id(), person.fullName(), person.gender(),
                    person.dateOfBirth(), person.mobile(), person.guardianPersonId());
        }
    }
}
