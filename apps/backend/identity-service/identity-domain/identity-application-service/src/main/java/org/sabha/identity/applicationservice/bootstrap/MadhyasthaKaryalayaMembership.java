package org.sabha.identity.applicationservice.bootstrap;

import java.util.UUID;

/**
 * Driven port for Madhyastha Karyalaya (State-level oversight body) membership.
 *
 * <p>MK membership is deliberately <em>not</em> a {@link org.sabha.common.Role}
 * — that enum holds only the operational Karyakar tiers. MK is recorded as a
 * {@code role_assignments} row with {@code role = 'MADHYASTHA_KARYALAYA'} and
 * null scope (State-level; the system is single-organization per ADR-0005, so
 * there is no per-State column). This port keeps that representation behind a
 * named membership concept rather than leaking the string into callers.</p>
 *
 * <p>It carried an {@code isMember(userId)} until ADR-0032. Every one of its six
 * call sites asked about the <em>caller</em>, so all six became {@code
 * CallerAuthority.isMadhyasthaKaryalaya()} and the method was left with no
 * consumers. What remains is a question about the organization and a write —
 * neither caller-keyed, which is why the port survives at all.</p>
 */
public interface MadhyasthaKaryalayaMembership {

    /** Whether any MK member exists at all — drives the one-off install bootstrap. */
    boolean anyMemberExists();

    /** Records that the given local user is an MK member. */
    void grantTo(UUID userId);
}
