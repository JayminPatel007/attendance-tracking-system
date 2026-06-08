package org.sabha.identity.applicationservice;

import java.util.UUID;

/**
 * Identity-local read port backing the assigner-reissue authorization (ADR-0004 /
 * ADR-0011): a User who has lost their mobile contacts their original appointing
 * Karyakar, who reissues a fresh password. Sants have no appointer, so Madhyastha
 * Karyalaya members reissue for them — that branch consults
 * {@link org.sabha.common.MadhyasthaKaryalayaLookup} instead.
 *
 * <p>The JDBC implementation reads {@code role_assignments} (the {@code appointed_by}
 * audit and the {@code SANT} role string); unit tests drive an in-memory fake.</p>
 */
public interface ReissueAuthorityLookup {

    /** Whether {@code targetUserId} holds any role assignment recorded as appointed by {@code appointerUserId}. */
    boolean wasAppointedBy(UUID targetUserId, UUID appointerUserId);

    /** Whether the User holds a Sant role — the no-appointer case MK handles. */
    boolean isSant(UUID userId);
}
