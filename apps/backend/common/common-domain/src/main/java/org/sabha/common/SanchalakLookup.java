package org.sabha.common;

import java.util.Optional;
import java.util.UUID;

/**
 * Cross-context read port (ADR-0019): who currently runs a Sabha. Keyed by the
 * <b>target</b> Sabha, not by a caller — which is why it survived the ADR-0032
 * fold when its two sibling methods did not. The attendance context's
 * Authorization Engine needs it to attribute a Nirikshak proxy action to the
 * absent Sanchalak it acts on behalf of (Slice 14); every other reading of
 * {@code role_assignments} it used to serve was about the caller and now comes
 * from {@link CallerAuthority}.
 *
 * <p>It was {@code RoleAssignmentLookup} until then. Once the caller-keyed
 * methods left, that name described a type that looks up exactly one thing, and
 * would have collided conceptually with the {@link RoleAssignment} row type the
 * fold introduced (issues #217 / #218). The {@code role_assignments} table is
 * identity's (ADR-0029), so the implementation lives in identity-data-access.</p>
 */
public interface SanchalakLookup {

    /**
     * The User currently holding the {@link Role#SANCHALAK} role on {@code
     * sabhaId}, if any.
     *
     * <p>It carried a {@code default} returning empty, so that fakes of the old
     * three-method port need not implement the proxy path. With the other two
     * methods folded away that default left the interface with no abstract method
     * at all — every class would have implemented it, and no lambda could. It is
     * abstract now, which is what lets a test write {@code sabhaId -> ...}.</p>
     */
    Optional<UUID> sanchalakOf(UUID sabhaId);
}
