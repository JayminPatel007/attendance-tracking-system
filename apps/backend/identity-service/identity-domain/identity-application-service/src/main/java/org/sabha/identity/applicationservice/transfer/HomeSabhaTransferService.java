package org.sabha.identity.applicationservice.transfer;

import java.util.Set;
import java.util.UUID;

import org.sabha.common.CallerResolver;
import org.sabha.common.Role;
import org.sabha.common.RoleAssignmentLookup;
import org.sabha.common.SabhaKindRetiredException;
import org.sabha.common.StructuralHierarchyLookup;
import org.sabha.identity.applicationservice.otp.OtpGuardedFlow;
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
 * <p>A Sanchalak/Sah-Sanchalak of the destination Sabha pulls a Directory Person
 * in; the Person's own OTP confirms consent before any Home Sabha changes. The
 * OTP send and code consumption belong to {@link OtpGuardedFlow}; what stays here
 * is who may pull a Person in, and the Roster swap that consent unlocks.
 */
@Service
public class HomeSabhaTransferService {

    private final CallerResolver callerResolver;
    private final RoleAssignmentLookup roleAssignments;
    private final HomeSabhaDirectory directory;
    private final HomeSabhaTransferRepository transfers;
    private final OtpGuardedFlow otpFlow;
    private final StructuralHierarchyLookup hierarchy;

    public HomeSabhaTransferService(
            CallerResolver callerResolver,
            RoleAssignmentLookup roleAssignments,
            HomeSabhaDirectory directory,
            HomeSabhaTransferRepository transfers,
            OtpGuardedFlow otpFlow,
            StructuralHierarchyLookup hierarchy) {
        this.callerResolver = callerResolver;
        this.roleAssignments = roleAssignments;
        this.directory = directory;
        this.transfers = transfers;
        this.otpFlow = otpFlow;
        this.hierarchy = hierarchy;
    }

    @Transactional
    public UUID initiate(UUID keycloakSubject, UUID personId, UUID destinationSabhaId) {
        UUID initiatingUserId = callerResolver.requireUserId(keycloakSubject);

        Set<Role> roles = roleAssignments.rolesForUserOnSabha(initiatingUserId, destinationSabhaId);
        if (!roles.contains(Role.SANCHALAK) && !roles.contains(Role.SAH_SANCHALAK)) {
            throw new TransferNotAuthorizedException(initiatingUserId, destinationSabhaId);
        }

        if (hierarchy.isSabhaKindRetired(destinationSabhaId)) {
            throw new SabhaKindRetiredException(destinationSabhaId);
        }

        Person person = directory.findById(personId).orElseThrow();
        String mobile = person.mobile();
        if (mobile == null || mobile.isBlank()) {
            throw new PersonHasNoMobileException(personId);
        }

        return otpFlow.begin(mobile, transfers, (code, now, hasher) -> HomeSabhaTransfer.initiate(
                UUID.randomUUID(), personId, mobile, destinationSabhaId,
                initiatingUserId, code, now, hasher)).id();
    }

    /**
     * Deliberately not {@code @Transactional}: {@link OtpGuardedFlow#consume} owns
     * the boundary, because it owns the rollback rules that let a rejected OTP keep
     * its consequence. The swap below runs inside that same transaction, so a
     * swap-phase failure still rolls the whole confirmation back.
     */
    public void confirm(UUID transferId, String otpCode) {
        otpFlow.consume(transferId, transfers, (transfer, now, hasher) -> {
            transfer.confirm(otpCode, now, hasher);

            String destinationKind = directory.kindOf(transfer.destinationSabhaId()).orElseThrow();
            UUID previousSabhaId = HomeSabhaSwap.selectPrevious(
                    directory.homeSabhasOf(transfer.personId()), destinationKind);
            directory.replaceHomeSabha(transfer.personId(), previousSabhaId, transfer.destinationSabhaId());
            transfer.recordSwap(previousSabhaId, now);
            return null;
        });
    }
}
