package org.sabha.common;

import java.util.UUID;

/**
 * Cross-context read port (ADR-0019): whether a user holds Madhyastha Karyalaya
 * (State-level oversight) membership. Lives in common-domain so the sabha
 * context's structural-creation authorization can consult it without depending
 * on identity's domain types — the {@code role_assignments} row that records MK
 * membership is owned by the identity bounded context, so the implementation
 * lives in identity-data-access.
 *
 * <p>MK is deliberately not a {@link Role} (that enum holds only operational
 * Karyakar tiers); this port keeps the membership read behind a named concept.</p>
 */
public interface MadhyasthaKaryalayaLookup {

    boolean isMember(UUID userId);
}
