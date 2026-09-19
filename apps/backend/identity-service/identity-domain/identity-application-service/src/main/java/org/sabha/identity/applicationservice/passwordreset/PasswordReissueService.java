package org.sabha.identity.applicationservice.passwordreset;

import java.time.Clock;
import java.util.UUID;

import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.AuthorizedAction;
import org.sabha.common.CallerAuthority;
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
 *
 * <p>The authority rule itself is no longer a private method here — ADR-0032
 * promoted it to {@link ReissueAuthorization}. What is left is the command: the
 * transaction, the identity-provider call and the audit row.</p>
 */
@Service
public class PasswordReissueService {

    private final ReissueAuthorization authorization;
    private final UserRepository users;
    private final IdentityProviderGateway identityProvider;
    private final PasswordReissueAuditLog audit;
    private final Clock clock;

    public PasswordReissueService(
            ReissueAuthorization authorization,
            UserRepository users,
            IdentityProviderGateway identityProvider,
            PasswordReissueAuditLog audit,
            Clock clock) {
        this.authorization = authorization;
        this.users = users;
        this.identityProvider = identityProvider;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public void reissue(CallerAuthority caller, UUID targetUserId, String newPassword) {
        UUID actor = caller.userId().value();

        if (!authorization.canReissue(caller, targetUserId)) {
            throw new AuthorizationDeniedException(actor, AuthorizedAction.REISSUE_PASSWORD);
        }

        User target = users.findById(targetUserId).orElseThrow();
        identityProvider.resetPassword(target.keycloakUserId(), newPassword, true);
        audit.recordReissue(UUID.randomUUID(), targetUserId, actor, clock.instant());
    }
}
