package org.sabha.sabha.application;

import java.util.List;

import org.sabha.common.CallerAuthority;
import org.sabha.common.web.CurrentUser;
import org.sabha.sabha.applicationservice.StructuralQueries;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read endpoint backing the Nirdeshak's Sabha-deletion list (ADR-0026, ADR-0022):
 * the Sabhas the signed-in member is Nirdeshak of, each with its recorded
 * Occurrence count so the web can disable delete on a non-empty Sabha. The caller
 * arrives already resolved to the local User by the edge (ADR-0030); the owning
 * {@code (Kshetra, demographic)} scopes come off the caller itself ({@link
 * CallerAuthority#nirdeshakScopes()}) rather than a cross-context port, mirroring
 *  the {@code my-zones}/{@code my-cities} reads for the tiers above. Delete
 * authority itself is re-checked server-side on
 * {@code DELETE /bff/sabhas/{id}}.
 */
@RestController
public class SabhaListController {

    private final StructuralQueries queries;

    public SabhaListController(StructuralQueries queries) {
        this.queries = queries;
    }

    @GetMapping("/bff/sabhas/mine")
    public ResponseEntity<List<StructuralQueries.SabhaView>> mySabhas(@CurrentUser CallerAuthority caller) {
        return ResponseEntity.ok(queries.sabhasOwnedBy(caller.nirdeshakScopes()));
    }
}
