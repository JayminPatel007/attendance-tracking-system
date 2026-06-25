package org.sabha.sabha.application;

import java.util.List;
import java.util.UUID;

import org.sabha.common.CallerResolver;
import org.sabha.common.NirdeshakScopeLookup;
import org.sabha.sabha.applicationservice.StructuralQueries;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read endpoint backing the Nirdeshak's Sabha-deletion list (ADR-0026, ADR-0022):
 * the Sabhas the signed-in member is Nirdeshak of, each with its recorded
 * Occurrence count so the web can disable delete on a non-empty Sabha. The caller
 * is the OIDC session subject resolved to the local User via {@link CallerResolver};
 * the owning {@code (Kshetra, demographic)} scopes come from the cross-context
 * {@link NirdeshakScopeLookup}, mirroring the {@code my-zones}/{@code my-cities}
 * reads for the tiers above. Delete authority itself is re-checked server-side on
 * {@code DELETE /bff/sabhas/{id}}.
 */
@RestController
public class SabhaListController {

    private final StructuralQueries queries;
    private final NirdeshakScopeLookup nirdeshakScopes;
    private final CallerResolver callers;

    public SabhaListController(
            StructuralQueries queries, NirdeshakScopeLookup nirdeshakScopes, CallerResolver callers) {
        this.queries = queries;
        this.nirdeshakScopes = nirdeshakScopes;
        this.callers = callers;
    }

    @GetMapping("/bff/sabhas/mine")
    public ResponseEntity<List<StructuralQueries.SabhaView>> mySabhas(Authentication authentication) {
        UUID userId = callers.requireUserId(UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(queries.sabhasOwnedBy(nirdeshakScopes.scopesOf(userId)));
    }
}
