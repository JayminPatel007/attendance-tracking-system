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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Person Directory endpoints (ADR-0013): add a Person, search the Directory
 * (exact mobile on {@code /persons}, fuzzy name on {@code /name-search}), and
 * read a Person's detail. Mobile hard block and the
 * neither-mobile-nor-guardian rule surface through the domain
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

    /**
     * The exact-mobile lookup that opens the add-person and transfer flows: the
     * mobile is the Directory's system-wide-unique key (ADR-0013), so a hit is a
     * single Person and a miss means "this number is new". The miss is a
     * {@code 404} the caller treats as an outcome, not an error — declared here
     * so the generated clients see both (issue #104).
     */
    @Operation(summary = "Look a Person up by their exact mobile number")
    @ApiResponse(responseCode = "200", description = "The Person registered to that mobile")
    @ApiResponse(responseCode = "404", description = "No Person has that mobile", content = @Content)
    @ApiResponse(responseCode = "400", description = "The mobile to look up was blank", content = @Content)
    @GetMapping("/api/directory/persons")
    public ResponseEntity<PersonResponse> byMobile(@RequestParam String mobile) {
        if (mobile.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return searchDirectory.byMobile(mobile)
                .map(p -> ResponseEntity.ok(PersonResponse.of(p)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * The Kshetra-scoped fuzzy name lookup, on its own path beside
     * {@code walk-in-search} rather than sharing the mobile lookup's. One path
     * cannot carry two response shapes without falling back to the untyped
     * {@code Object} that kept the mobile flow hand-rolled (issue #104).
     */
    @GetMapping("/api/directory/name-search")
    public ResponseEntity<List<NameCandidate>> nameSearch(
            @RequestParam UUID kshetraId,
            @RequestParam String name) {
        return ResponseEntity.ok(searchDirectory.byName(kshetraId, name));
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

    /**
     * The two outcomes of an add in one shape: a clean create carries the new
     * {@code personId} with {@code requiresOverride} false, and a name soft-warn
     * carries the candidate list with {@code requiresOverride} true and no id.
     * The discriminator and the list are therefore always present and
     * {@code personId} is not — stated in the document (issue #104) so callers
     * branch on a non-null flag rather than on a nullable one.
     */
    public record AddPersonResponse(
            UUID personId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<NameCandidate> candidates,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean requiresOverride) {

        static AddPersonResponse created(UUID personId) {
            return new AddPersonResponse(personId, List.of(), false);
        }

        static AddPersonResponse softWarn(List<NameCandidate> candidates) {
            return new AddPersonResponse(null, candidates, true);
        }
    }
}
