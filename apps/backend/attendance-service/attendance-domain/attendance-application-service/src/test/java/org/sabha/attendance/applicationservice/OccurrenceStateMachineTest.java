package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.attendance.domain.InvalidOccurrenceTransitionException;
import org.sabha.attendance.domain.Occurrence;
import org.sabha.attendance.domain.OccurrenceFinalized;
import org.sabha.attendance.domain.OccurrenceOpened;
import org.sabha.attendance.domain.OccurrenceState;
import org.sabha.common.ConcurrentModificationException;
import org.sabha.common.DomainEvent;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.OptimisticLockException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OccurrenceStateMachineTest {

    private static final UUID OCCURRENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID SABHA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant FIXED_NOW = Instant.parse("2026-05-26T09:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Test
    void openActionTransitionsAScheduledOccurrenceAndAppendsAnAuditRow() {
        InMemoryOccurrenceRepository occurrences = new InMemoryOccurrenceRepository();
        Occurrence scheduled = Occurrence.scheduled(OCCURRENCE_ID, SABHA_ID, LocalDate.of(2026, 5, 26));
        occurrences.put(scheduled);
        InMemoryTransitionLog transitions = new InMemoryTransitionLog();
        CapturingPublisher publisher = new CapturingPublisher();
        OccurrenceStateMachine stateMachine = new OccurrenceStateMachine(
                occurrences, transitions, publisher, FIXED_CLOCK);

        stateMachine.transition(OCCURRENCE_ID, OccurrenceAction.OPEN, TransitionActor.system());

        assertThat(occurrences.savedOccurrences()).hasSize(1);
        assertThat(occurrences.savedOccurrences().get(0).state()).isEqualTo(OccurrenceState.OPEN_FOR_MARKING);

        assertThat(transitions.appended).hasSize(1);
        OccurrenceStateTransition row = transitions.appended.get(0);
        assertThat(row.occurrenceId()).isEqualTo(OCCURRENCE_ID);
        assertThat(row.fromState()).isEqualTo(OccurrenceState.SCHEDULED);
        assertThat(row.toState()).isEqualTo(OccurrenceState.OPEN_FOR_MARKING);
        assertThat(row.action()).isEqualTo(OccurrenceAction.OPEN);
        assertThat(row.actorKind()).isEqualTo(ActorKind.SYSTEM);
        assertThat(row.actorUserId()).isNull();
        assertThat(row.at()).isEqualTo(FIXED_NOW);

        assertThat(publisher.published).singleElement().isInstanceOf(OccurrenceOpened.class);
    }

    @Test
    void finalizeActionTransitionsAnOpenOccurrenceAndAppendsAuditRowWithUserActor() {
        InMemoryOccurrenceRepository occurrences = new InMemoryOccurrenceRepository();
        Occurrence open = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 26), OccurrenceState.OPEN_FOR_MARKING);
        occurrences.put(open);
        InMemoryTransitionLog transitions = new InMemoryTransitionLog();
        CapturingPublisher publisher = new CapturingPublisher();
        OccurrenceStateMachine stateMachine = new OccurrenceStateMachine(
                occurrences, transitions, publisher, FIXED_CLOCK);
        UUID nirikshakUserId = UUID.fromString("00000000-0000-0000-0000-000000000200");

        stateMachine.transition(OCCURRENCE_ID, OccurrenceAction.FINALIZE,
                TransitionActor.user(nirikshakUserId));

        assertThat(occurrences.savedOccurrences()).hasSize(1);
        assertThat(occurrences.savedOccurrences().get(0).state()).isEqualTo(OccurrenceState.FINALIZED);

        assertThat(transitions.appended).hasSize(1);
        OccurrenceStateTransition row = transitions.appended.get(0);
        assertThat(row.fromState()).isEqualTo(OccurrenceState.OPEN_FOR_MARKING);
        assertThat(row.toState()).isEqualTo(OccurrenceState.FINALIZED);
        assertThat(row.action()).isEqualTo(OccurrenceAction.FINALIZE);
        assertThat(row.actorKind()).isEqualTo(ActorKind.USER);
        assertThat(row.actorUserId()).isEqualTo(nirikshakUserId);

        assertThat(publisher.published).singleElement().isInstanceOf(OccurrenceFinalized.class);
    }

    @Test
    void invalidActionForCurrentStatePropagatesAndLeavesNoSideEffects() {
        InMemoryOccurrenceRepository occurrences = new InMemoryOccurrenceRepository();
        Occurrence finalized = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 26), OccurrenceState.FINALIZED);
        occurrences.put(finalized);
        InMemoryTransitionLog transitions = new InMemoryTransitionLog();
        CapturingPublisher publisher = new CapturingPublisher();
        OccurrenceStateMachine stateMachine = new OccurrenceStateMachine(
                occurrences, transitions, publisher, FIXED_CLOCK);

        assertThatThrownBy(() -> stateMachine.transition(
                OCCURRENCE_ID, OccurrenceAction.OPEN, TransitionActor.system()))
                .isInstanceOf(InvalidOccurrenceTransitionException.class);

        assertThat(occurrences.savedOccurrences()).isEmpty();
        assertThat(transitions.appended).isEmpty();
        assertThat(publisher.published).isEmpty();
    }

    @Test
    void unknownOccurrenceIdThrowsOccurrenceNotFound() {
        InMemoryOccurrenceRepository occurrences = new InMemoryOccurrenceRepository();
        InMemoryTransitionLog transitions = new InMemoryTransitionLog();
        CapturingPublisher publisher = new CapturingPublisher();
        OccurrenceStateMachine stateMachine = new OccurrenceStateMachine(
                occurrences, transitions, publisher, FIXED_CLOCK);

        assertThatThrownBy(() -> stateMachine.transition(
                OCCURRENCE_ID, OccurrenceAction.OPEN, TransitionActor.system()))
                .isInstanceOf(OccurrenceNotFoundException.class);

        assertThat(transitions.appended).isEmpty();
        assertThat(publisher.published).isEmpty();
    }

    @Test
    void retriesOnOptimisticLockConflictAndSucceedsBeforeMaxAttempts() {
        FlakyOccurrenceRepository occurrences = new FlakyOccurrenceRepository(1,
                OccurrenceState.SCHEDULED);
        InMemoryTransitionLog transitions = new InMemoryTransitionLog();
        CapturingPublisher publisher = new CapturingPublisher();
        OccurrenceStateMachine stateMachine = new OccurrenceStateMachine(
                occurrences, transitions, publisher, FIXED_CLOCK);

        stateMachine.transition(OCCURRENCE_ID, OccurrenceAction.OPEN, TransitionActor.system());

        assertThat(occurrences.saveAttempts).isEqualTo(2);
        assertThat(transitions.appended).hasSize(1);
        assertThat(publisher.published).singleElement().isInstanceOf(OccurrenceOpened.class);
    }

    @Test
    void givesUpAfterThreeOptimisticLockConflictsAndSurfacesConcurrentModification() {
        FlakyOccurrenceRepository occurrences = new FlakyOccurrenceRepository(99,
                OccurrenceState.SCHEDULED);
        InMemoryTransitionLog transitions = new InMemoryTransitionLog();
        CapturingPublisher publisher = new CapturingPublisher();
        OccurrenceStateMachine stateMachine = new OccurrenceStateMachine(
                occurrences, transitions, publisher, FIXED_CLOCK);

        assertThatThrownBy(() -> stateMachine.transition(
                OCCURRENCE_ID, OccurrenceAction.OPEN, TransitionActor.system()))
                .isInstanceOf(ConcurrentModificationException.class);

        assertThat(occurrences.saveAttempts).isEqualTo(3);
        assertThat(transitions.appended).isEmpty();
        assertThat(publisher.published).isEmpty();
    }

    static final class FlakyOccurrenceRepository implements OccurrenceRepository {
        private final int failures;
        private final OccurrenceState seedState;
        int saveAttempts;

        FlakyOccurrenceRepository(int failures, OccurrenceState seedState) {
            this.failures = failures;
            this.seedState = seedState;
        }

        @Override
        public Optional<Occurrence> findById(UUID occurrenceId) {
            return Optional.of(new Occurrence(occurrenceId, SABHA_ID,
                    LocalDate.of(2026, 5, 26), seedState));
        }

        @Override
        public void save(Occurrence occurrence) {
            saveAttempts++;
            if (saveAttempts <= failures) {
                throw new OptimisticLockException(occurrence.id());
            }
        }
    }

    static final class InMemoryOccurrenceRepository implements OccurrenceRepository {
        final Map<UUID, Occurrence> store = new HashMap<>();
        final List<Occurrence> saved = new ArrayList<>();

        void put(Occurrence occurrence) {
            store.put(occurrence.id(), occurrence);
        }

        List<Occurrence> savedOccurrences() {
            return saved;
        }

        @Override
        public Optional<Occurrence> findById(UUID occurrenceId) {
            Occurrence held = store.get(occurrenceId);
            if (held == null) {
                return Optional.empty();
            }
            return Optional.of(new Occurrence(held.id(), held.sabhaId(), held.date(), held.state()));
        }

        @Override
        public void save(Occurrence occurrence) {
            store.put(occurrence.id(), occurrence);
            saved.add(occurrence);
        }
    }

    static final class InMemoryTransitionLog implements OccurrenceStateTransitionRepository {
        final List<OccurrenceStateTransition> appended = new ArrayList<>();

        @Override
        public void append(OccurrenceStateTransition transition) {
            appended.add(transition);
        }
    }

    static final class CapturingPublisher implements DomainEventPublisher {
        final List<DomainEvent> published = new ArrayList<>();

        @Override
        public void publishAll(List<? extends DomainEvent> events) {
            published.addAll(events);
        }
    }
}
