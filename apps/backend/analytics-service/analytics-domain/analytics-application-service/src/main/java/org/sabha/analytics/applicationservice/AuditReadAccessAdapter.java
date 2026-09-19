package org.sabha.analytics.applicationservice;

import org.sabha.common.AuditReadAccess;
import org.sabha.common.CallerAuthority;
import org.springframework.stereotype.Component;

/**
 * Surfaces the audit Authorization Engine ({@link AuditLogAccess}) as the
 * cross-context {@link AuditReadAccess} port the identity web shell consults to
 * gate the Audit-log section (issue #80). A caller may read the log exactly when
 * the engine admits them to a scope; this keeps the web nav gate and the BFF in
 * lockstep without a second tier-set definition.
 *
 * <p>It lives in {@code analytics-application-service} rather than {@code
 * analytics-data-access} under ADR-0032's placement rule — <em>a port
 * implementation lives in the ring that owns what it delegates to</em>. This one
 * delegates to an engine and, since the fold, issues no queries at all; putting
 * it in the data-access ring would force that module to depend on
 * application-service and invert ADR-0019's ring order. What #133 recorded as a
 * layering oddity is dissolved by the rule rather than excused.</p>
 *
 * <p>It stays a separate class from the engine it fronts because it is the only
 * place carrying the <em>direction</em> of the dependency — identity asks,
 * analytics answers. A second cross-context audit question is the moment merging
 * the two wins.</p>
 */
@Component
public class AuditReadAccessAdapter implements AuditReadAccess {

    private final AuditLogAccess access;

    public AuditReadAccessAdapter(AuditLogAccess access) {
        this.access = access;
    }

    @Override
    public boolean canRead(CallerAuthority caller) {
        return access.scopeFor(caller).admitted();
    }
}
