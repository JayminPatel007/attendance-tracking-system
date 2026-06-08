package org.sabha.identity.applicationservice;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.sabha.common.DomainEventPublisher;
import org.sabha.identity.domain.OtpAttemptsExhaustedException;
import org.sabha.identity.domain.OtpExpiredException;
import org.sabha.identity.domain.PasswordReset;
import org.sabha.identity.domain.User;
import org.sabha.identity.domain.WrongOtpException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Self-service password-reset orchestrator (ADR-0004) — the deep module behind
 * the two-step public interface:
 *
 * <pre>
 *   request(username)                       -> resetId   (OTP sent to the mobile)
 *   verify(resetId, otpCode)                -> resetToken (later cycle)
 *   complete(resetToken, newPassword)       -> (password changed) (later cycle)
 * </pre>
 *
 * <p>It re-uses Slice 8's OTP infrastructure ({@link OtpGateway},
 * {@link OtpCodeGenerator}) and sends the code to the User's registered mobile.
 * <em>Login</em> stays username + password (ADR-0004); OTP appears only at reset
 * time.</p>
 */
@Service
public class PasswordResetService {

    /** Rolling window and cap for reset OTP sends per mobile (PRD-0001). */
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);
    private static final int MAX_OTPS_PER_WINDOW = 3;

    /** Resend cooldown between OTPs to the same mobile (PRD-0001). */
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(30);

    private final UserRepository users;
    private final PersonContactLookup contacts;
    private final PasswordResetRepository resets;
    private final OtpGateway otpGateway;
    private final OtpCodeGenerator otpCodeGenerator;
    private final ResetTokenGenerator resetTokenGenerator;
    private final IdentityProviderGateway identityProvider;
    private final DomainEventPublisher events;
    private final Clock clock;

    public PasswordResetService(
            UserRepository users,
            PersonContactLookup contacts,
            PasswordResetRepository resets,
            OtpGateway otpGateway,
            OtpCodeGenerator otpCodeGenerator,
            ResetTokenGenerator resetTokenGenerator,
            IdentityProviderGateway identityProvider,
            DomainEventPublisher events,
            Clock clock) {
        this.users = users;
        this.contacts = contacts;
        this.resets = resets;
        this.otpGateway = otpGateway;
        this.otpCodeGenerator = otpCodeGenerator;
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

        Instant now = clock.instant();
        resets.lastInitiatedAt(mobile).ifPresent(last -> {
            if (Duration.between(last, now).compareTo(RESEND_COOLDOWN) < 0) {
                throw new OtpResendCooldownException(mobile);
            }
        });
        if (resets.sendCountSince(mobile, now.minus(RATE_LIMIT_WINDOW)) >= MAX_OTPS_PER_WINDOW) {
            throw new OtpRateLimitExceededException(mobile);
        }

        String code = otpCodeGenerator.generate();
        PasswordReset reset = PasswordReset.request(
                UUID.randomUUID(), user.id(), user.keycloakUserId(), mobile, code, now);

        otpGateway.send(mobile, code);
        reset.markOtpSent(now);
        resets.save(reset);
        events.publishAll(reset.pullDomainEvents());
        return reset.id();
    }

    /**
     * A failed OTP must still persist its consequence (incremented attempt count,
     * EXPIRED status) so the budget accumulates across calls — hence the
     * OTP-consume exceptions are exempted from rollback, mirroring
     * {@link HomeSabhaTransferService#confirm}.
     */
    @Transactional(noRollbackFor = {
            WrongOtpException.class,
            OtpExpiredException.class,
            OtpAttemptsExhaustedException.class })
    public String verify(UUID resetId, String otpCode) {
        PasswordReset reset = resets.findById(resetId).orElseThrow();

        String resetToken = resetTokenGenerator.generate();
        try {
            reset.verify(otpCode, resetToken, clock.instant());
        } catch (WrongOtpException | OtpExpiredException | OtpAttemptsExhaustedException rejected) {
            resets.save(reset);
            throw rejected;
        }

        resets.save(reset);
        events.publishAll(reset.pullDomainEvents());
        return resetToken;
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
