package org.sabha.sabha.applicationservice;

import java.time.Clock;
import java.util.UUID;

import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.AuthorizedAction;
import org.sabha.sabha.domain.SabhaKind;
import org.sabha.sabha.domain.SabhaKindNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the soft-retire lifecycle of a Sabha Kind (ADR-0026). A Sabha Kind
 * is reference data the whole type system hangs off, so it is never hard-deleted —
 * the Madhyastha Karyalaya marks it inactive so no new Sabhas, roles, or Home
 * Sabhas of that kind can be created while existing ones drain, and can later
 * reactivate it.
 *
 * <p>Retiring and reactivating are State-level MK authority, the same authority
 * that registers kinds ({@link StructuralCreationAuthorization#canCreateStateStructure}).
 * A denial becomes an {@link AuthorizationDeniedException} (HTTP 403); the
 * aggregate enforces the active/retired transition invariants. The retire is
 * attributed to the acting MK member on the {@link SabhaKind} itself.</p>
 */
@Service
public class SabhaKindLifecycleService {

    private final StructuralCreationAuthorization authz;
    private final SabhaKindRepository sabhaKinds;
    private final Clock clock;

    public SabhaKindLifecycleService(
            StructuralCreationAuthorization authz, SabhaKindRepository sabhaKinds, Clock clock) {
        this.authz = authz;
        this.sabhaKinds = sabhaKinds;
        this.clock = clock;
    }

    @Transactional
    public void retire(UUID caller, UUID kindId) {
        SabhaKind kind = requireStateAuthorityOver(caller, kindId, AuthorizedAction.RETIRE_SABHA_KIND);
        sabhaKinds.update(kind.retire(caller, clock.instant()));
    }

    @Transactional
    public void reactivate(UUID caller, UUID kindId) {
        SabhaKind kind = requireStateAuthorityOver(caller, kindId, AuthorizedAction.REACTIVATE_SABHA_KIND);
        sabhaKinds.update(kind.reactivate());
    }

    private SabhaKind requireStateAuthorityOver(UUID caller, UUID kindId, AuthorizedAction action) {
        if (!authz.canCreateStateStructure(caller)) {
            throw new AuthorizationDeniedException(caller, action);
        }
        return sabhaKinds.findById(kindId).orElseThrow(() -> new SabhaKindNotFoundException(kindId));
    }
}
