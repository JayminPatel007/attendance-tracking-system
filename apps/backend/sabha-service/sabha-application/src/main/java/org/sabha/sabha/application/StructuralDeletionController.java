package org.sabha.sabha.application;

import java.util.UUID;

import org.sabha.common.CallerResolver;
import org.sabha.sabha.applicationservice.StructuralDeletionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Structural-admin deletion BFF endpoints (ADR-0026, ADR-0022): the Angular web
 * shell deletes Cities (MK), Zones (Regional Team), Kshetras (Sanyojak), and
 * Sabhas (Nirdeshak) by their owning scope. Each delete is block-if-non-empty —
 * allowed only when the entity has no live children and no recorded Occurrences
 * beneath it, never cascading — and authorized by the tier-above scope holder.
 *
 * <p>Like the creation endpoints, these are web requests authenticated by the
 * server-side OIDC session; the caller is the Keycloak subject resolved to the
 * local User via {@link CallerResolver}. Authority and emptiness are arbitrated
 * by {@link StructuralDeletionService} — 403 on denial, 409 (with the
 * human-readable blocking reason) on a non-empty entity, 404 on an unknown id,
 * all via the global handler.</p>
 */
@RestController
public class StructuralDeletionController {

    private final StructuralDeletionService deletion;
    private final CallerResolver callers;

    public StructuralDeletionController(StructuralDeletionService deletion, CallerResolver callers) {
        this.deletion = deletion;
        this.callers = callers;
    }

    @DeleteMapping("/bff/structure/cities/{id}")
    public ResponseEntity<Void> deleteCity(@PathVariable UUID id, Authentication authentication) {
        deletion.deleteCity(requireCaller(authentication), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bff/structure/zones/{id}")
    public ResponseEntity<Void> deleteZone(@PathVariable UUID id, Authentication authentication) {
        deletion.deleteZone(requireCaller(authentication), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bff/structure/kshetras/{id}")
    public ResponseEntity<Void> deleteKshetra(@PathVariable UUID id, Authentication authentication) {
        deletion.deleteKshetra(requireCaller(authentication), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bff/sabhas/{id}")
    public ResponseEntity<Void> deleteSabha(@PathVariable UUID id, Authentication authentication) {
        deletion.deleteSabha(requireCaller(authentication), id);
        return ResponseEntity.noContent().build();
    }

    private UUID requireCaller(Authentication authentication) {
        return callers.requireUserId(UUID.fromString(authentication.getName()));
    }
}
