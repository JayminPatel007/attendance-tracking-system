package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.sabha.attendance.domain.Occurrence;
import org.sabha.attendance.domain.OccurrenceState;
import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.CallerResolver;
import org.sabha.common.ConcurrentModificationException;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.OptimisticLockException;
import org.springframework.stereotype.Component;
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
 * TransitionActor} driving it and the mutation to apply. Their own vocabulary and
 * preconditions (reason-required, grace windows, roster freshness) stay in the
 * calling application service; the retry/authorize/audit/publish orchestration
 * never diverges between them because there is only one copy of it.</p>
 *
 * <p>Nothing is written until the save succeeds: a mutation that throws — an
 * invalid transition, a denied authority — leaves no audit row and publishes no
 * event.</p>
 */
@Component
public class OccurrenceWriter {

    private static final int MAX_OPTIMISTIC_LOCK_ATTEMPTS = 3;

    private final CallerResolver callerResolver;
    private final AuthorizationEngine authorization;
    private final OccurrenceRepository occurrences;
    private final OccurrenceStateTransitionRepository transitions;
    private final DomainEventPublisher events;
    private final Clock clock;

    public OccurrenceWriter(
            CallerResolver callerResolver,
            AuthorizationEngine authorization,
            OccurrenceRepository occurrences,
            OccurrenceStateTransitionRepository transitions,
            DomainEventPublisher events,
            Clock clock) {
        this.callerResolver = callerResolver;
        this.authorization = authorization;
        this.occurrences = occurrences;
        this.transitions = transitions;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Applies an audited lifecycle transition. The audit row records the state
     * either side of {@code mutation}, so it captures what actually happened
     * rather than what was asked for.
     *
     * @param actor     who is driving the write; a signed-in actor is resolved and
     *                  authorized against the Occurrence's Sabha first
     * @param auditedAs the action recorded on the audit row — may differ from the
     *                  actor's authority (e.g. REVERT audited under CANCEL authority)
     * @param reason    free-text reason for the audit row, or {@code null}
     * @param mutation  the aggregate transition to apply
     */
    @Transactional
    public void transition(UUID occurrenceId, TransitionActor actor, OccurrenceAction auditedAs,
                           String reason, Consumer<Occurrence> mutation) {
        UUID actorUserId = resolve(actor);
        write(occurrenceId, occurrence -> {
            UUID onBehalfOf = authorize(actor, actorUserId, occurrence);
            OccurrenceState from = occurrence.state();
            mutation.accept(occurrence);
            return new OccurrenceStateTransition(
                    UUID.randomUUID(), occurrenceId, from, occurrence.state(), auditedAs,
                    actor.kind(), actorUserId, onBehalfOf, reason, clock.instant());
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
    public void mutate(UUID occurrenceId, UUID keycloakSubject,
                       BiConsumer<Occurrence, UUID> mutation) {
        UUID actorUserId = callerResolver.requireUserId(keycloakSubject);
        write(occurrenceId, occurrence -> {
            mutation.accept(occurrence, actorUserId);
            return null;
        });
    }

    /**
     * The load-mutate-save-retry cycle. {@code apply} mutates the loaded aggregate
     * and returns the audit row to append once the save sticks, or {@code null}
     * when the write is not an audited transition.
     */
    private void write(UUID occurrenceId, Function<Occurrence, OccurrenceStateTransition> apply) {
        OptimisticLockException lastConflict = null;
        for (int attempt = 0; attempt < MAX_OPTIMISTIC_LOCK_ATTEMPTS; attempt++) {
            Occurrence occurrence = occurrences.findById(occurrenceId)
                    .orElseThrow(() -> new OccurrenceNotFoundException(occurrenceId));

            OccurrenceStateTransition auditRow = apply.apply(occurrence);

            try {
                occurrences.save(occurrence);
            } catch (OptimisticLockException retry) {
                lastConflict = retry;
                continue;
            }

            if (auditRow != null) {
                transitions.append(auditRow);
            }
            events.publishAll(occurrence.pullDomainEvents());
            return;
        }
        throw new ConcurrentModificationException(occurrenceId, lastConflict);
    }

    /**
     * Resolves the calling user before the aggregate is loaded, so an unknown
     * caller is rejected without touching the Occurrence.
     */
    private UUID resolve(TransitionActor actor) {
        return actor instanceof TransitionActor.SignedIn user
                ? callerResolver.requireUserId(user.keycloakSubject())
                : null;
    }

    /** @return the absent Sanchalak this is a proxy action for, or {@code null} */
    private UUID authorize(TransitionActor actor, UUID actorUserId, Occurrence occurrence) {
        if (!(actor instanceof TransitionActor.SignedIn user)) {
            return null;
        }
        if (!authorization.canUserDo(actorUserId, user.authority(), occurrence.sabhaId())) {
            throw new AuthorizationDeniedException(actorUserId, user.authority());
        }
        return authorization.onBehalfOf(actorUserId, user.authority(), occurrence.sabhaId())
                .orElse(null);
    }
}
