package org.sabha.identity.applicationservice;

import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * The Authorization Engine for Sabha definition (ADR-0012): a Sabha may be
 * created only by the Nirdeshak over its (Kshetra, demographic) scope — the same
 * authority that appoints the Sabha's Sanchalak (ADR-0011). Pure decision
 * component: returns a boolean, never throws or mutates; {@link SabhaDefinitionService}
 * turns a {@code false} into an
 * {@link org.sabha.common.AuthorizationDeniedException}.
 */
@Service
public class SabhaDefinitionAuthorization {

    private final AppointerAuthorityLookup appointer;

    public SabhaDefinitionAuthorization(AppointerAuthorityLookup appointer) {
        this.appointer = appointer;
    }

    public boolean canDefineSabha(UUID userId, UUID kshetraId, String demographic) {
        return appointer.holdsNirdeshak(userId, kshetraId, demographic);
    }
}
