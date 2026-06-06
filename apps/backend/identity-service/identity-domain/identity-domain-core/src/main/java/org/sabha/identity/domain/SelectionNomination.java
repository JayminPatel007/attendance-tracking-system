package org.sabha.identity.domain;

import java.time.Instant;
import java.util.UUID;

import org.sabha.common.AggregateRoot;

/**
 * The BSS/YSS Selection Nomination aggregate (ADR-0006). A Regular Sanchalak
 * nominates a Person from their Roster for the selective track; the demographic
 * Nirdeshak then approves (the Person gains an <em>additional</em> selective Home
 * Sabha — the Regular one is untouched) or rejects. The nomination record itself
 * is the audit trail: it carries who nominated, who decided, when, and any
 * rejection reason. Selection criteria are not system-enforced — the workflow is
 * facilitated and audited, never gated on age or attendance.
 */
public class SelectionNomination extends AggregateRoot<UUID> {

    private final UUID id;
    private final UUID personId;
    private final UUID regularSabhaId;
    private final UUID selectiveSabhaId;
    private final UUID kshetraId;
    private final String demographic;
    private final String track;
    private final UUID nominatedBy;
    private final Instant nominatedAt;
    private NominationStatus status;
    private UUID decidedBy;
    private Instant decidedAt;
    private String rejectionReason;

    private SelectionNomination(UUID id, UUID personId, UUID regularSabhaId, UUID selectiveSabhaId,
                                UUID kshetraId, String demographic, String track, UUID nominatedBy,
                                Instant nominatedAt, NominationStatus status) {
        this.id = id;
        this.personId = personId;
        this.regularSabhaId = regularSabhaId;
        this.selectiveSabhaId = selectiveSabhaId;
        this.kshetraId = kshetraId;
        this.demographic = demographic;
        this.track = track;
        this.nominatedBy = nominatedBy;
        this.nominatedAt = nominatedAt;
        this.status = status;
    }

    /**
     * Opens a nomination in {@code PENDING}, targeting the selective Sabha derived
     * from the Person's Regular Sabha. Registers {@link SelectionNominated}.
     */
    public static SelectionNomination nominate(UUID id, UUID personId, UUID regularSabhaId,
                                               UUID selectiveSabhaId, UUID kshetraId, String demographic,
                                               String track, UUID nominatedBy, Instant now) {
        SelectionNomination nomination = new SelectionNomination(
                id, personId, regularSabhaId, selectiveSabhaId, kshetraId, demographic, track,
                nominatedBy, now, NominationStatus.PENDING);
        nomination.registerEvent(new SelectionNominated(id, personId, selectiveSabhaId, nominatedBy, now));
        return nomination;
    }

    /**
     * The demographic Nirdeshak approves: the nomination moves to {@code APPROVED}
     * and the decider/timestamp are recorded for audit. The application service
     * then adds the selective Home Sabha. Registers {@link SelectionApproved}.
     */
    public void approve(UUID deciderUserId, Instant now) {
        requirePending();
        this.status = NominationStatus.APPROVED;
        this.decidedBy = deciderUserId;
        this.decidedAt = now;
        registerEvent(new SelectionApproved(id, personId, selectiveSabhaId, deciderUserId, now));
    }

    /**
     * The demographic Nirdeshak rejects with an optional {@code reason}: the
     * nomination moves to {@code REJECTED} and the decider/timestamp/reason are
     * recorded for audit. No Home Sabha changes. Registers {@link SelectionRejected}.
     */
    public void reject(UUID deciderUserId, String reason, Instant now) {
        requirePending();
        this.status = NominationStatus.REJECTED;
        this.decidedBy = deciderUserId;
        this.decidedAt = now;
        this.rejectionReason = reason;
        registerEvent(new SelectionRejected(id, personId, deciderUserId, reason, now));
    }

    /**
     * The demographic Nirdeshak deselects an approved Person: the nomination moves
     * to {@code DESELECTED} and the decider/timestamp are recorded for audit. The
     * application service then removes the selective Home Sabha. Registers
     * {@link SelectionRevoked}.
     */
    public void deselect(UUID deciderUserId, Instant now) {
        if (status != NominationStatus.APPROVED) {
            throw new NotApprovedException(id, status);
        }
        this.status = NominationStatus.DESELECTED;
        this.decidedBy = deciderUserId;
        this.decidedAt = now;
        registerEvent(new SelectionRevoked(id, personId, selectiveSabhaId, deciderUserId, now));
    }

    private void requirePending() {
        if (status != NominationStatus.PENDING) {
            throw new NominationAlreadyDecidedException(id, status);
        }
    }

    /** Rehydrates a persisted nomination without registering any domain events. */
    public static SelectionNomination rehydrate(UUID id, UUID personId, UUID regularSabhaId,
                                                UUID selectiveSabhaId, UUID kshetraId, String demographic,
                                                String track, UUID nominatedBy, Instant nominatedAt,
                                                NominationStatus status, UUID decidedBy, Instant decidedAt,
                                                String rejectionReason) {
        SelectionNomination nomination = new SelectionNomination(
                id, personId, regularSabhaId, selectiveSabhaId, kshetraId, demographic, track,
                nominatedBy, nominatedAt, status);
        nomination.decidedBy = decidedBy;
        nomination.decidedAt = decidedAt;
        nomination.rejectionReason = rejectionReason;
        return nomination;
    }

    @Override
    public UUID id() {
        return id;
    }

    public UUID personId() {
        return personId;
    }

    public UUID regularSabhaId() {
        return regularSabhaId;
    }

    public UUID selectiveSabhaId() {
        return selectiveSabhaId;
    }

    public UUID kshetraId() {
        return kshetraId;
    }

    public String demographic() {
        return demographic;
    }

    public String track() {
        return track;
    }

    public UUID nominatedBy() {
        return nominatedBy;
    }

    public Instant nominatedAt() {
        return nominatedAt;
    }

    public NominationStatus status() {
        return status;
    }

    public UUID decidedBy() {
        return decidedBy;
    }

    public Instant decidedAt() {
        return decidedAt;
    }

    public String rejectionReason() {
        return rejectionReason;
    }
}
