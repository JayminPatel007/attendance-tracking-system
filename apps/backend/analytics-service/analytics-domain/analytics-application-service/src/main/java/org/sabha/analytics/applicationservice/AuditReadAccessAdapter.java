package org.sabha.analytics.applicationservice;

import java.util.UUID;

import org.sabha.common.AuditReadAccess;
import org.springframework.stereotype.Component;

/**
 * Surfaces the audit Authorization Engine ({@link AuditLogAccess}) as the
 * cross-context {@link AuditReadAccess} port the identity web shell consults to
 * gate the Audit-log section (issue #80). A caller may read the log exactly when
 * the engine resolves them to a scope other than {@link AuditScope.Denied}; this
 * keeps the web nav gate and the BFF in lockstep without a second tier-set
 * definition.
 */
@Component
public class AuditReadAccessAdapter implements AuditReadAccess {

    private final AuditLogAccess access;

    public AuditReadAccessAdapter(AuditLogAccess access) {
        this.access = access;
    }

    @Override
    public boolean canRead(UUID userId) {
        return !(access.scopeFor(userId) instanceof AuditScope.Denied);
    }
}
