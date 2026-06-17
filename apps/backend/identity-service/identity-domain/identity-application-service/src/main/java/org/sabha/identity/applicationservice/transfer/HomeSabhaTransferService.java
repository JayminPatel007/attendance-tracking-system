package org.sabha.identity.applicationservice.transfer;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.sabha.common.CallerResolver;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.Role;
import org.sabha.common.RoleAssignmentLookup;
import org.sabha.identity.applicationservice.otp.OtpCodeGenerator;
import org.sabha.identity.applicationservice.otp.OtpGateway;
import org.sabha.identity.applicationservice.otp.OtpSendPolicy;
import org.sabha.identity.domain.HomeSabhaSwap;
import org.sabha.identity.domain.HomeSabhaTransfer;
import org.sabha.identity.domain.OtpAttemptsExhaustedException;
import org.sabha.identity.domain.OtpExpiredException;
import org.sabha.identity.domain.OtpHasher;
import org.sabha.identity.domain.Person;
import org.sabha.identity.domain.PersonHasNoMobileException;
import org.sabha.identity.domain.WrongOtpException;
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

    private final CallerResolver callerResolver;
    private final RoleAssignmentLookup roleAssignments;
    private final HomeSabhaDirectory directory;
    private final HomeSabhaTransferRepository transfers;
    private final OtpGateway otpGateway;
    private final OtpCodeGenerator otpCodeGenerator;
    private final OtpHasher otpHasher;
    private final OtpSendPolicy otpSendPolicy;
    private final DomainEventPublisher events;
    private final Clock clock;

    public HomeSabhaTransferService(
            CallerResolver callerResolver,
            RoleAssignmentLookup roleAssignments,
            HomeSabhaDirectory directory,
            HomeSabhaTransferRepository transfers,
            OtpGateway otpGateway,
            OtpCodeGenerator otpCodeGenerator,
            OtpHasher otpHasher,
            OtpSendPolicy otpSendPolicy,
            DomainEventPublisher events,
            Clock clock) {
        this.callerResolver = callerResolver;
        this.roleAssignments = roleAssignments;
        this.directory = directory;
        this.transfers = transfers;
        this.otpGateway = otpGateway;
        this.otpCodeGenerator = otpCodeGenerator;
        this.otpHasher = otpHasher;
        this.otpSendPolicy = otpSendPolicy;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public UUID initiate(UUID keycloakSubject, UUID personId, UUID destinationSabhaId) {
        UUID initiatingUserId = callerResolver.requireUserId(keycloakSubject);

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
        otpSendPolicy.enforce(mobile, transfers, now);

        String code = otpCodeGenerator.generate();
        HomeSabhaTransfer transfer = HomeSabhaTransfer.initiate(
                UUID.randomUUID(), personId, mobile, destinationSabhaId,
                initiatingUserId, code, now, otpHasher);

        otpGateway.send(mobile, code);
        transfer.markOtpSent(now);
        transfers.save(transfer);
        events.publishAll(transfer.pullDomainEvents());
        return transfer.id();
    }

    /**
     * A failed OTP must still persist its consequence (incremented attempt count,
     * EXPIRED/LOCKED status) so the budget accumulates across calls — hence the
     * OTP-consume exceptions are exempted from rollback. The list is kept narrow
     * on purpose: a swap-phase failure (e.g. {@link org.sabha.identity.domain.NoMatchingHomeSabhaException},
     * also a {@code DomainException}) must roll back normally so it can never
     * leave a half-applied swap behind.
     */
    @Transactional(noRollbackFor = {
            WrongOtpException.class,
            OtpExpiredException.class,
            OtpAttemptsExhaustedException.class })
    public void confirm(UUID transferId, String otpCode) {
        HomeSabhaTransfer transfer = transfers.findById(transferId).orElseThrow();

        try {
            transfer.confirm(otpCode, clock.instant(), otpHasher);
        } catch (WrongOtpException | OtpExpiredException | OtpAttemptsExhaustedException rejected) {
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
