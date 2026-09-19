package org.sabha.identity.applicationservice.transfer;

import java.util.UUID;

import org.sabha.common.CallerAuthority;
import org.sabha.common.SabhaKindRetiredException;
import org.sabha.common.SabhaFact;
import org.sabha.common.SabhaFacts;
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
 *
 * <p>"Who may pull a Person in" is now one call — {@link
 * CallerAuthority#runsSabha(UUID)} — where it used to be a two-line role fold
 * copied byte for byte into {@code SelectionService.nominate}. Reading the
 * caller's roles inline was flagged as an engine bypass while they came from a
 * port; under a caller parameter there is no port to bypass, and two new engine
 * classes would only have preserved the duplication behind two names
 * (ADR-0032).</p>
 */
@Service
public class HomeSabhaTransferService {

    private final HomeSabhaDirectory directory;
    private final HomeSabhaTransferRepository transfers;
    private final OtpGuardedFlow otpFlow;
    private final SabhaFacts sabhas;

    public HomeSabhaTransferService(
            HomeSabhaDirectory directory,
            HomeSabhaTransferRepository transfers,
            OtpGuardedFlow otpFlow,
            SabhaFacts sabhas) {
        this.directory = directory;
        this.transfers = transfers;
        this.otpFlow = otpFlow;
        this.sabhas = sabhas;
    }

    @Transactional
    public UUID initiate(CallerAuthority caller, UUID personId, UUID destinationSabhaId) {
        UUID initiatingUserId = caller.userId().value();

        if (!caller.runsSabha(destinationSabhaId)) {
            throw new TransferNotAuthorizedException(initiatingUserId, destinationSabhaId);
        }

        if (isKindRetired(destinationSabhaId)) {
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

    /**
     * Whether the given Sabha's {@code (demographic, track)} kind has been
     * soft-retired (ADR-0026). {@code false} when no such Sabha exists — the
     * retired check is not an existence check, and the caller's own not-found
     * handling reports a missing Sabha.
     */
    private boolean isKindRetired(UUID sabhaId) {
        return sabhas.of(sabhaId).map(SabhaFact::kindRetired).orElse(false);
    }
}
