package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.util.UUID;

import org.sabha.attendance.domain.Occurrence;
import org.sabha.attendance.domain.OccurrenceState;
import org.sabha.common.ConcurrentModificationException;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.OptimisticLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deep-module dispatcher for Occurrence state transitions (PRD-0001).
 *
 * <p>Public interface: {@link #transition(UUID, OccurrenceAction, TransitionActor)}.
 * The application service loads the aggregate, applies the requested action,
 * persists, appends an audit row, and publishes any registered domain events.
 * On optimistic-lock conflict the load-mutate-save cycle is retried up to
 * three times before surfacing {@link ConcurrentModificationException}.
 */
@Service
public class OccurrenceStateMachine {

    private static final int MAX_OPTIMISTIC_LOCK_ATTEMPTS = 3;

    private final OccurrenceRepository occurrences;
    private final OccurrenceStateTransitionRepository transitions;
    private final DomainEventPublisher events;
    private final Clock clock;

    public OccurrenceStateMachine(
            OccurrenceRepository occurrences,
            OccurrenceStateTransitionRepository transitions,
            DomainEventPublisher events,
            Clock clock) {
        this.occurrences = occurrences;
        this.transitions = transitions;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public void transition(UUID occurrenceId, OccurrenceAction action, TransitionActor actor) {
        OptimisticLockException lastConflict = null;
        for (int attempt = 0; attempt < MAX_OPTIMISTIC_LOCK_ATTEMPTS; attempt++) {
            Occurrence occurrence = occurrences.findById(occurrenceId)
                    .orElseThrow(() -> new OccurrenceNotFoundException(occurrenceId));

            OccurrenceState from = occurrence.state();
            applyAction(occurrence, action);
            OccurrenceState to = occurrence.state();

            try {
                occurrences.save(occurrence);
            } catch (OptimisticLockException retry) {
                lastConflict = retry;
                continue;
            }

            transitions.append(new OccurrenceStateTransition(
                    UUID.randomUUID(),
                    occurrenceId,
                    from,
                    to,
                    action,
                    actor.kind(),
                    actor.userId(),
                    null, // never a proxy action — the auto-Finalize/Open cron acts as SYSTEM
                    null, // no reason on an automatic transition
                    clock.instant()));
            events.publishAll(occurrence.pullDomainEvents());
            return;
        }
        throw new ConcurrentModificationException(occurrenceId, lastConflict);
    }

    private void applyAction(Occurrence occurrence, OccurrenceAction action) {
        switch (action) {
            case OPEN -> occurrence.open();
            case FINALIZE -> occurrence.markFinalized();
        }
    }
}
