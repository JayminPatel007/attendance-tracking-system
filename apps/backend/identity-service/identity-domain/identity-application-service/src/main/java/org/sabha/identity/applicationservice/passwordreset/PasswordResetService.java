package org.sabha.identity.applicationservice.passwordreset;

import java.time.Clock;
import java.util.UUID;

import org.sabha.common.DomainEventPublisher;
import org.sabha.identity.applicationservice.IdentityProviderGateway;
import org.sabha.identity.applicationservice.UnknownUsernameException;
import org.sabha.identity.applicationservice.UserRepository;
import org.sabha.identity.applicationservice.directory.PersonContactLookup;
import org.sabha.identity.applicationservice.otp.OtpGuardedFlow;
import org.sabha.identity.domain.PasswordReset;
import org.sabha.identity.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Self-service password-reset orchestrator (ADR-0004) — the deep module behind
 * the three-step public interface:
 *
 * <pre>
 *   request(username)                       -> resetId   (OTP sent to the mobile)
 *   verify(resetId, otpCode)                -> resetToken
 *   complete(resetToken, newPassword)       -> (password changed)
 * </pre>
 *
 * <p>The OTP half of the first two steps belongs to {@link OtpGuardedFlow}; what
 * stays here is what is this flow's own — which User is being reset and where
 * their code goes, and what verifying and completing mean for a credential.
 * <em>Login</em> stays username + password (ADR-0004); OTP appears only at reset
 * time.</p>
 */
@Service
public class PasswordResetService {

    private final UserRepository users;
    private final PersonContactLookup contacts;
    private final PasswordResetRepository resets;
    private final OtpGuardedFlow otpFlow;
    private final ResetTokenGenerator resetTokenGenerator;
    private final IdentityProviderGateway identityProvider;
    private final DomainEventPublisher events;
    private final Clock clock;

    public PasswordResetService(
            UserRepository users,
            PersonContactLookup contacts,
            PasswordResetRepository resets,
            OtpGuardedFlow otpFlow,
            ResetTokenGenerator resetTokenGenerator,
            IdentityProviderGateway identityProvider,
            DomainEventPublisher events,
            Clock clock) {
        this.users = users;
        this.contacts = contacts;
        this.resets = resets;
        this.otpFlow = otpFlow;
        this.resetTokenGenerator = resetTokenGenerator;
        this.identityProvider = identityProvider;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public UUID request(String username) {
        User user = users.findByUsername(username)
                .orElseThrow(() -> new UnknownUsernameException(username));
        String mobile = contacts.mobileOf(user.personId())
                .filter(m -> !m.isBlank())
                .orElseThrow(() -> new NoRegisteredMobileException(username));

        return otpFlow.begin(mobile, resets, (code, now, hasher) -> PasswordReset.request(
                UUID.randomUUID(), user.id(), user.keycloakUserId(), mobile, code, now, hasher)).id();
    }

    /**
     * Deliberately not {@code @Transactional}: {@link OtpGuardedFlow#consume} owns
     * the boundary, because it owns the rollback rules that let a rejected OTP keep
     * its consequence. An annotation here would decide the rollback instead.
     */
    public String verify(UUID resetId, String otpCode) {
        return otpFlow.consume(resetId, resets, (reset, now, hasher) -> {
            String resetToken = resetTokenGenerator.generate();
            reset.verify(otpCode, resetToken, now, hasher);
            return resetToken;
        });
    }

    @Transactional
    public void complete(String resetToken, String newPassword) {
        PasswordReset reset = resets.findByResetToken(resetToken)
                .orElseThrow(InvalidResetTokenException::new);

        reset.complete(clock.instant());
        identityProvider.resetPassword(reset.keycloakUserId(), newPassword, false);

        resets.save(reset);
        events.publishAll(reset.pullDomainEvents());
    }
}
