package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.sabha.attendance.domain.Occurrence;
import org.sabha.attendance.domain.OccurrenceState;
import org.sabha.attendance.domain.Reason;
import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.ConcurrentModificationException;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.OptimisticLockException;
import org.sabha.common.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The one write path to an Occurrence (issue #128). Every mutation of the
 * aggregate — Sanchalak shaping (Slice 5), the higher-tier reopen (Slice 13),
 * the auto-Open / auto-Finalize cron (Slice 3), and attendance marking
 * (Slice 2/4) — goes through here, so the optimistic-lock contract is written
 * down exactly once: load, mutate, save, retry the whole cycle on {@link
 * OptimisticLockException} up to {@value #MAX_OPTIMISTIC_LOCK_ATTEMPTS} times,
 * then append the audit row and publish the aggregate's registered events.
 * Exhausting the retries surfaces {@link ConcurrentModificationException}.
 *
 * <p>Callers supply only what makes their write different: the {@link
 * TransitionActor} driving it, the mutation to apply, and — for the two transitions
 * that must say why (cancel, reopen) — a {@link Reason}. Their own vocabulary and
 * preconditions (grace windows, roster freshness) stay in the calling application
 * service; reason-required is no longer among them, it is the {@link Reason} type's
 * and fires wherever a Reason is constructed. The retry/authorize/audit/publish
 * orchestration never diverges between callers because there is only one copy.</p>
 *
 * <p>Nothing is written until the save succeeds: a mutation that throws — an
 * invalid transition, a denied authority — leaves no audit row and publishes no
 * event, and the audit row is stamped with the moment the save stuck.</p>
 *
 * <p>The write methods are {@code @Transactional} (ADR-0018) so the cron scanners,
 * which call in from a Spring-scheduled thread with no ambient transaction, still
 * get one per Occurrence — the boundary the deleted {@code OccurrenceStateMachine}
 * owned. Request-driven callers are themselves {@code @Transactional}, so their
 * write simply joins the transaction they opened.</p>
 */
@Service
public class OccurrenceWriter {

    private static final int MAX_OPTIMISTIC_LOCK_ATTEMPTS = 3;

    private final AuthorizationEngine authorization;
    private final OccurrenceRepository occurrences;
    private final OccurrenceStateTransitionRepository transitions;
    private final DomainEventPublisher events;
    private final Clock clock;

    public OccurrenceWriter(
            AuthorizationEngine authorization,
            OccurrenceRepository occurrences,
            OccurrenceStateTransitionRepository transitions,
            DomainEventPublisher events,
            Clock clock) {
        this.authorization = authorization;
        this.occurrences = occurrences;
        this.transitions = transitions;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Applies an audited lifecycle transition that carries no reason — revert,
     * reschedule, venue-override, and the cron's auto-Open / auto-Finalize. The
     * audit row's reason is left empty.
     *
     * @param actor     who is driving the write; a signed-in actor is authorized
     *                  against the Occurrence's Sabha first
     * @param auditedAs the action recorded on the audit row — may differ from the
     *                  actor's authority (e.g. REVERT audited under CANCEL authority)
     * @param mutation  the aggregate transition to apply
     */
    @Transactional
    public void transition(UUID occurrenceId, TransitionActor actor, OccurrenceAction auditedAs,
                           Consumer<Occurrence> mutation) {
        audited(occurrenceId, actor, auditedAs, null, mutation);
    }

    /**
     * Applies an audited lifecycle transition that must record why it happened —
     * cancel and reopen (ADR-0001). The {@link Reason} is already valid by the time
     * it arrives, so no check belongs here.
     *
     * <p>The audit row records the state either side of {@code mutation}, so it
     * captures what actually happened rather than what was asked for.</p>
     *
     * @param actor     who is driving the write; a signed-in actor is authorized
     *                  against the Occurrence's Sabha first
     * @param auditedAs the action recorded on the audit row — may differ from the
     *                  actor's authority
     * @param reason    why the transition was made; stamped on the audit row
     * @param mutation  the aggregate transition to apply
     */
    @Transactional
    public void transition(UUID occurrenceId, TransitionActor actor, OccurrenceAction auditedAs,
                           Reason reason, Consumer<Occurrence> mutation) {
        Objects.requireNonNull(reason, "this transition must record a reason; "
                + "use the overload without one if it genuinely has none");
        audited(occurrenceId, actor, auditedAs, reason.text(), mutation);
    }

    /**
     * The one audited-transition body. Only the two public overloads decide whether
     * a reason is recorded, so {@code null} appears in exactly one place instead of
     * at every reason-less call site.
     */
    private void audited(UUID occurrenceId, TransitionActor actor, OccurrenceAction auditedAs,
                         String reasonText, Consumer<Occurrence> mutation) {
        UUID actorUserId = actorUserId(actor);
        write(occurrenceId, occurrence -> {
            UUID onBehalfOf = authorize(actor, actorUserId, occurrence);
            OccurrenceState from = occurrence.state();
            mutation.accept(occurrence);
            return new AuditIntent(from, occurrence.state(), auditedAs,
                    actor.kind(), actorUserId, onBehalfOf, reasonText);
        });
    }

    /**
     * Applies a write that is not a lifecycle transition — attendance marking,
     * which changes the Occurrence's markings but not its state and so appends no
     * audit row (markings carry their own {@code markedBy} attribution). The
     * mutation is handed the caller's resolved User id.
     *
     * <p>No authority is checked here: marking is gated by the aggregate's
     * Open-for-marking guard and by the Roster the caller was served, not by the
     * {@link AuthorizationEngine}.</p>
     */
    @Transactional
    public void mutateUnaudited(UUID occurrenceId, UserId caller,
                                 BiConsumer<Occurrence, UUID> mutation) {
        write(occurrenceId, occurrence -> {
            mutation.accept(occurrence, caller.value());
            return null;
        });
    }

    /**
     * The load-mutate-save-retry cycle. {@code apply} mutates the loaded aggregate
     * and returns what the audit row should record, or {@code null} when the write
     * is not an audited transition.
     */
    private void write(UUID occurrenceId, Function<Occurrence, AuditIntent> apply) {
        OptimisticLockException lastConflict = null;
        for (int attempt = 0; attempt < MAX_OPTIMISTIC_LOCK_ATTEMPTS; attempt++) {
            Occurrence occurrence = occurrences.findById(occurrenceId)
                    .orElseThrow(() -> new OccurrenceNotFoundException(occurrenceId));

            AuditIntent audited = apply.apply(occurrence);

            try {
                occurrences.save(occurrence);
            } catch (OptimisticLockException retry) {
                lastConflict = retry;
                continue;
            }

            if (audited != null) {
                transitions.append(audited.stampedAt(occurrenceId, clock.instant()));
            }
            events.publishAll(occurrence.pullDomainEvents());
            return;
        }
        throw new ConcurrentModificationException(occurrenceId, lastConflict);
    }

    /** The acting user, or {@code null} for the cron actor, which holds no identity. */
    private static UUID actorUserId(TransitionActor actor) {
        return actor instanceof TransitionActor.SignedIn user ? user.caller().userId().value() : null;
    }

    /**
     * What an audited transition will record, minus the moment it landed: the row
     * is only stamped and appended once the save has stuck, so a write that ends
     * up retried or abandoned never dates an audit row it did not write.
     */
    private record AuditIntent(
            OccurrenceState fromState,
            OccurrenceState toState,
            OccurrenceAction action,
            ActorKind actorKind,
            UUID actorUserId,
            UUID onBehalfOfUserId,
            String reason) {

        OccurrenceStateTransition stampedAt(UUID occurrenceId, Instant at) {
            return new OccurrenceStateTransition(UUID.randomUUID(), occurrenceId,
                    fromState, toState, action, actorKind, actorUserId, onBehalfOfUserId, reason, at);
        }
    }

    /** @return the absent Sanchalak this is a proxy action for, or {@code null} */
    private UUID authorize(TransitionActor actor, UUID actorUserId, Occurrence occurrence) {
        if (!(actor instanceof TransitionActor.SignedIn user)) {
            return null;
        }
        if (!authorization.canUserDo(user.caller(), user.authority(), occurrence.sabhaId())) {
            throw new AuthorizationDeniedException(actorUserId, user.authority());
        }
        return authorization.onBehalfOf(user.caller(), user.authority(), occurrence.sabhaId())
                .orElse(null);
    }
}
