package org.sabha.common;

import java.util.UUID;

/**
 * Cross-context read port (ADR-0019, ADR-0029): whether a user holds the Sant
 * oversight role. Lives in common-domain so any context — the analytics
 * dashboard and audit-log engines, the identity web shell and password-reissue
 * flow — can consult it without duplicating {@code role = 'SANT'} SQL. The
 * {@code role_assignments} row that records Sant membership is owned by the
 * identity bounded context, so the implementation lives in identity-data-access
 * (one adapter, mirroring {@link MadhyasthaKaryalayaLookup}).
 *
 * <p>Sant is deliberately not a {@link Role}; it is an {@link OversightRole},
 * which is where the "outside the operational Role enum" caveat now lives. The
 * Sant's formal (City, demographic) on that row is irrelevant to every caller of
 * this port — they need only the boolean.</p>
 */
public interface SantLookup {

    boolean isSant(UUID userId);
}
