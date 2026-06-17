package org.sabha.identity.applicationservice.passwordreset;

import java.time.Clock;
import java.util.UUID;

import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.AuthorizedAction;
import org.sabha.common.CallerResolver;
import org.sabha.common.MadhyasthaKaryalayaLookup;
import org.sabha.common.SantLookup;
import org.sabha.identity.applicationservice.IdentityProviderGateway;
import org.sabha.identity.applicationservice.UserRepository;
import org.sabha.identity.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assigner-reissue orchestrator (ADR-0004): the fallback for a User who has lost
 * their mobile entirely and so cannot self-serve an OTP reset. Their original
 * appointing Karyakar generates a fresh password (force-change on next login);
 * for Sants — who have no appointer per ADR-0011 — a Madhyastha Karyalaya member
 * does it instead.
 *
 * <p>Distinct from {@link PasswordResetService}: this path is authenticated, has
 * no OTP, and the new password is caller-supplied (mirroring the appointment
 * flow). Every reissue is audited with its actor and timestamp.</p>
 */
@Service
public class PasswordReissueService {

    private final CallerResolver callerResolver;
    private final ReissueAuthorityLookup authority;
    private final SantLookup sants;
    private final MadhyasthaKaryalayaLookup mkMembership;
    private final UserRepository users;
    private final IdentityProviderGateway identityProvider;
    private final PasswordReissueAuditLog audit;
    private final Clock clock;

    public PasswordReissueService(
            CallerResolver callerResolver,
            ReissueAuthorityLookup authority,
            SantLookup sants,
            MadhyasthaKaryalayaLookup mkMembership,
            UserRepository users,
            IdentityProviderGateway identityProvider,
            PasswordReissueAuditLog audit,
            Clock clock) {
        this.callerResolver = callerResolver;
        this.authority = authority;
        this.sants = sants;
        this.mkMembership = mkMembership;
        this.users = users;
        this.identityProvider = identityProvider;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public void reissue(UUID keycloakSubject, UUID targetUserId, String newPassword) {
        UUID actor = callerResolver.requireUserId(keycloakSubject);

        if (!canReissue(actor, targetUserId)) {
            throw new AuthorizationDeniedException(actor, AuthorizedAction.REISSUE_PASSWORD);
        }

        User target = users.findById(targetUserId).orElseThrow();
        identityProvider.resetPassword(target.keycloakUserId(), newPassword, true);
        audit.recordReissue(UUID.randomUUID(), targetUserId, actor, clock.instant());
    }

    private boolean canReissue(UUID actor, UUID targetUserId) {
        if (authority.wasAppointedBy(targetUserId, actor)) {
            return true;
        }
        return sants.isSant(targetUserId) && mkMembership.isMember(actor);
    }
}
