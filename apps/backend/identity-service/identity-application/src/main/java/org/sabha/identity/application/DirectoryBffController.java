package org.sabha.identity.application;

import java.util.List;
import java.util.UUID;

import org.sabha.identity.applicationservice.directory.NameCandidate;
import org.sabha.identity.applicationservice.directory.SearchDirectoryUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Directory search for the web shell (ADR-0013, ADR-0022): an exact mobile lookup
 * and a Kshetra-scoped name lookup, backing the directory-first candidate list of
 * flows like role appointment (Slice 11). The mobile-bearer twin of this search
 * lives in {@code PersonDirectoryRestController} on the {@code /api} chain; this
 * is the cookie-session {@code /bff} surface. Both delegate to the one
 * {@link SearchDirectoryUseCase}, so the search logic itself is not duplicated —
 * and both split the two lookups across two paths for the same reason (issue
 * #104 there, #131 here): one path answering with either a Person or a candidate
 * list can only be typed {@code Object}, which the generated clients render as an
 * untyped payload every caller then has to parse by hand.
 */
@RestController
public class DirectoryBffController {

    private final SearchDirectoryUseCase searchDirectory;

    public DirectoryBffController(SearchDirectoryUseCase searchDirectory) {
        this.searchDirectory = searchDirectory;
    }

    /**
     * The mobile is the Directory's system-wide-unique key (ADR-0013), so a hit is
     * a single Person and a miss means "this number is new" — an outcome the
     * person picker offers to act on, not an error.
     */
    @Operation(summary = "Look a Person up by their exact mobile number")
    @ApiResponse(responseCode = "200", description = "The Person registered to that mobile")
    @ApiResponse(responseCode = "404", description = "No Person has that mobile", content = @Content)
    @ApiResponse(responseCode = "400", description = "The mobile to look up was blank", content = @Content)
    @GetMapping("/bff/directory/search")
    public ResponseEntity<PersonResponse> search(@RequestParam String mobile) {
        if (mobile.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return searchDirectory.byMobile(mobile)
                .map(p -> ResponseEntity.ok(PersonResponse.of(p)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/bff/directory/name-search")
    public ResponseEntity<List<NameCandidate>> nameSearch(
            @RequestParam UUID kshetraId,
            @RequestParam String name) {
        return ResponseEntity.ok(searchDirectory.byName(kshetraId, name));
    }
}
