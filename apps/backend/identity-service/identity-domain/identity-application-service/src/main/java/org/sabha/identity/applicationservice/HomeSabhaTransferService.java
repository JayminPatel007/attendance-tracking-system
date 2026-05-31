package org.sabha.identity.applicationservice;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import java.util.Set;

import org.sabha.common.CallerResolver;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.DomainException;
import org.sabha.common.Role;
import org.sabha.common.RoleAssignmentLookup;
import org.sabha.identity.domain.HomeSabhaSwap;
import org.sabha.identity.domain.HomeSabhaTransfer;
import org.sabha.identity.domain.Person;
import org.sabha.identity.domain.PersonHasNoMobileException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verified Home Sabha Transfer orchestrator (ADR-0002) — the deep module behind
 * the two-call public interface:
 *
 * <pre>
 *   initiate(personId, destinationSabhaId, initiatingUserId) -> transferId
 *   confirm(transferId, otpCode)                             -> (swap committed)
 * </pre>
 *
 * <p>It hides OTP send, consent receipt, the Roster swap, and audit. A
 * Sanchalak/Sah-Sanchalak of the destination Sabha pulls a Directory Person in;
 * the Person's own OTP confirms consent before any Home Sabha changes.
 */
@Service
public class HomeSabhaTransferService {

    /** Rolling window and cap for OTP sends per mobile (PRD-0001). */
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);
    private static final int MAX_OTPS_PER_WINDOW = 3;
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(30);

    private final CallerResolver callerResolver;
    private final RoleAssignmentLookup roleAssignments;
    private final HomeSabhaDirectory directory;
    private final HomeSabhaTransferRepository transfers;
    private final OtpGateway otpGateway;
    private final OtpCodeGenerator otpCodeGenerator;
    private final DomainEventPublisher events;
    private final Clock clock;

    public HomeSabhaTransferService(
            CallerResolver callerResolver,
            RoleAssignmentLookup roleAssignments,
            HomeSabhaDirectory directory,
            HomeSabhaTransferRepository transfers,
            OtpGateway otpGateway,
            OtpCodeGenerator otpCodeGenerator,
            DomainEventPublisher events,
            Clock clock) {
        this.callerResolver = callerResolver;
        this.roleAssignments = roleAssignments;
        this.directory = directory;
        this.transfers = transfers;
        this.otpGateway = otpGateway;
        this.otpCodeGenerator = otpCodeGenerator;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public UUID initiate(UUID keycloakSubject, UUID personId, UUID destinationSabhaId) {
        UUID initiatingUserId = callerResolver.resolveUserId(keycloakSubject)
                .orElseThrow(() -> new CallerUnknownException(keycloakSubject));

        Set<Role> roles = roleAssignments.rolesForUserOnSabha(initiatingUserId, destinationSabhaId);
        if (!roles.contains(Role.SANCHALAK) && !roles.contains(Role.SAH_SANCHALAK)) {
            throw new TransferNotAuthorizedException(initiatingUserId, destinationSabhaId);
        }

        Person person = directory.findById(personId).orElseThrow();
        String mobile = person.mobile();
        if (mobile == null || mobile.isBlank()) {
            throw new PersonHasNoMobileException(personId);
        }

        Instant now = clock.instant();
        transfers.lastInitiatedAt(mobile).ifPresent(last -> {
            if (Duration.between(last, now).compareTo(RESEND_COOLDOWN) < 0) {
                throw new OtpResendCooldownException(mobile);
            }
        });
        if (transfers.sendCountSince(mobile, now.minus(RATE_LIMIT_WINDOW)) >= MAX_OTPS_PER_WINDOW) {
            throw new OtpRateLimitExceededException(mobile);
        }

        String code = otpCodeGenerator.generate();
        HomeSabhaTransfer transfer = HomeSabhaTransfer.initiate(
                UUID.randomUUID(), personId, mobile, destinationSabhaId,
                initiatingUserId, code, now);

        otpGateway.send(mobile, code);
        transfer.markOtpSent(now);
        transfers.save(transfer);
        events.publishAll(transfer.pullDomainEvents());
        return transfer.id();
    }

    /**
     * A failed OTP must still persist its consequence (incremented attempt count,
     * EXPIRED/LOCKED status) so the budget accumulates across calls — hence
     * {@code noRollbackFor} on the OTP-consume {@link DomainException}s.
     */
    @Transactional(noRollbackFor = DomainException.class)
    public void confirm(UUID transferId, String otpCode) {
        HomeSabhaTransfer transfer = transfers.findById(transferId).orElseThrow();

        try {
            transfer.confirm(otpCode, clock.instant());
        } catch (DomainException rejected) {
            transfers.save(transfer);
            throw rejected;
        }

        String destinationKind = directory.kindOf(transfer.destinationSabhaId()).orElseThrow();
        UUID previousSabhaId = HomeSabhaSwap.selectPrevious(
                directory.homeSabhasOf(transfer.personId()), destinationKind);
        directory.replaceHomeSabha(transfer.personId(), previousSabhaId, transfer.destinationSabhaId());
        transfer.recordSwap(previousSabhaId, clock.instant());

        transfers.save(transfer);
        events.publishAll(transfer.pullDomainEvents());
    }
}
