package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Consumer;

import org.sabha.attendance.domain.Occurrence;
import org.sabha.attendance.domain.OccurrenceState;
import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.AuthorizedAction;
import org.sabha.common.CallerResolver;
import org.sabha.common.ConcurrentModificationException;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.OptimisticLockException;
import org.springframework.stereotype.Component;

/**
 * Applies one authorized, audited state transition to an Occurrence — the single
 * engine room shared by every actor-driven transition (Slice 5 shaping, Slice 13
 * reopen). It resolves the calling user, asks the {@link AuthorizationEngine}
 * whether they may perform the action on the Occurrence's Sabha, runs the supplied
 * mutation, persists, appends the state-transition audit row (Slice 3), and
 * publishes the registered domain events. The load-mutate-save cycle is retried on
 * optimistic-lock conflict; exhausting the retries surfaces {@link
 * ConcurrentModificationException}.
 *
 * <p>Callers stay thin: each application service supplies its own vocabulary and
 * preconditions (e.g. reason-required, grace windows) and delegates the orchestration
 * here, so the retry/authorize/audit/publish contract lives in exactly one place.
 * The transaction boundary is owned by the calling {@code @Transactional} service
 * method.</p>
 */
@Component
public class OccurrenceTransitionExecutor {

    private static final int MAX_OPTIMISTIC_LOCK_ATTEMPTS = 3;

    private final CallerResolver callerResolver;
    private final AuthorizationEngine authorization;
    private final OccurrenceRepository occurrences;
    private final OccurrenceStateTransitionRepository transitions;
    private final DomainEventPublisher events;
    private final Clock clock;

    public OccurrenceTransitionExecutor(
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
     * @param authAction  the authority arbitrated by the {@link AuthorizationEngine}
     * @param auditAction the action recorded on the audit row (may differ from
     *                    {@code authAction}, e.g. REVERT audited under CANCEL authority)
     * @param reason      free-text reason for the audit row, or {@code null}
     * @param mutation    the aggregate transition to apply once the caller is authorized
     */
    public void execute(UUID keycloakSubject, UUID occurrenceId, AuthorizedAction authAction,
                        OccurrenceAction auditAction, String reason, Consumer<Occurrence> mutation) {
        UUID userId = callerResolver.resolveUserId(keycloakSubject)
                .orElseThrow(() -> new CallerUnknownException(keycloakSubject));

        OptimisticLockException lastConflict = null;
        for (int attempt = 0; attempt < MAX_OPTIMISTIC_LOCK_ATTEMPTS; attempt++) {
            Occurrence occurrence = occurrences.findById(occurrenceId)
                    .orElseThrow(() -> new OccurrenceNotFoundException(occurrenceId));

            if (!authorization.canUserDo(userId, authAction, occurrence.sabhaId())) {
                throw new AuthorizationDeniedException(userId, authAction);
            }
            UUID onBehalfOf = authorization.onBehalfOf(userId, authAction, occurrence.sabhaId())
                    .orElse(null);

            OccurrenceState from = occurrence.state();
            mutation.accept(occurrence);
            OccurrenceState to = occurrence.state();

            try {
                occurrences.save(occurrence);
            } catch (OptimisticLockException retry) {
                lastConflict = retry;
                continue;
            }

            transitions.append(new OccurrenceStateTransition(
                    UUID.randomUUID(), occurrenceId, from, to, auditAction,
                    ActorKind.USER, userId, onBehalfOf, reason, clock.instant()));
            events.publishAll(occurrence.pullDomainEvents());
            return;
        }
        throw new ConcurrentModificationException(occurrenceId, lastConflict);
    }
}
