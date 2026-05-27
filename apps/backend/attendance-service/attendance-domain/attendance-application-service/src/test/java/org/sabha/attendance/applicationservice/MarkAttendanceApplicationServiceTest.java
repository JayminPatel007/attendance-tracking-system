package org.sabha.attendance.applicationservice;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.attendance.domain.AttendanceMarked;
import org.sabha.attendance.domain.Occurrence;
import org.sabha.attendance.domain.OccurrenceState;
import org.sabha.common.CallerResolver;
import org.sabha.common.DomainEvent;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.OptimisticLockException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkAttendanceApplicationServiceTest {

    private static final UUID SUBJECT = UUID.fromString("00000000-0000-0000-0000-000000000300");
    private static final UUID MARKED_BY = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID OCCURRENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID SABHA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PERSON_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final Instant CLIENT_MARKED_AT = Instant.parse("2026-05-23T19:00:00Z");

    @Test
    void executingTheUseCaseLoadsMarksSavesAndPublishesEvents() {
        InMemoryOccurrenceRepository occurrences = new InMemoryOccurrenceRepository();
        Occurrence existing = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.OPEN_FOR_MARKING);
        occurrences.put(existing);
        CapturingPublisher publisher = new CapturingPublisher();
        MarkAttendanceApplicationService service = new MarkAttendanceApplicationService(
                staticCallerResolver(),
                occurrences,
                publisher);

        service.execute(SUBJECT, OCCURRENCE_ID, PERSON_ID, true, CLIENT_MARKED_AT);

        assertThat(occurrences.savedOccurrences()).hasSize(1);
        Occurrence saved = occurrences.savedOccurrences().get(0);
        assertThat(saved.id()).isEqualTo(OCCURRENCE_ID);
        assertThat(saved.markings()).hasSize(1);
        assertThat(saved.markings().iterator().next().personId()).isEqualTo(PERSON_ID);
        assertThat(saved.markings().iterator().next().present()).isTrue();
        assertThat(publisher.published).singleElement().isInstanceOf(AttendanceMarked.class);
    }

    @Test
    void executingGivesUpAfterThreeOptimisticLockConflictsAndSurfacesConcurrentModification() {
        FlakyOccurrenceRepository occurrences = new FlakyOccurrenceRepository(99);
        Occurrence existing = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.OPEN_FOR_MARKING);
        occurrences.put(existing);
        CapturingPublisher publisher = new CapturingPublisher();
        MarkAttendanceApplicationService service = new MarkAttendanceApplicationService(
                staticCallerResolver(),
                occurrences,
                publisher);

        assertThatThrownBy(() -> service.execute(SUBJECT, OCCURRENCE_ID, PERSON_ID, true, CLIENT_MARKED_AT))
                .isInstanceOf(org.sabha.common.ConcurrentModificationException.class);
        assertThat(occurrences.saveAttempts).isEqualTo(3);
        assertThat(publisher.published).isEmpty();
    }

    @Test
    void executingRetriesAndSucceedsAfterAnOptimisticLockConflict() {
        FlakyOccurrenceRepository occurrences = new FlakyOccurrenceRepository(1);
        Occurrence existing = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.OPEN_FOR_MARKING);
        occurrences.put(existing);
        CapturingPublisher publisher = new CapturingPublisher();
        MarkAttendanceApplicationService service = new MarkAttendanceApplicationService(
                staticCallerResolver(),
                occurrences,
                publisher);

        service.execute(SUBJECT, OCCURRENCE_ID, PERSON_ID, true, CLIENT_MARKED_AT);

        assertThat(occurrences.saveAttempts).isEqualTo(2);
        assertThat(publisher.published).singleElement().isInstanceOf(AttendanceMarked.class);
    }

    @Test
    void executingWithAnUnknownKeycloakSubjectThrowsCallerUnknown() {
        InMemoryOccurrenceRepository occurrences = new InMemoryOccurrenceRepository();
        CapturingPublisher publisher = new CapturingPublisher();
        MarkAttendanceApplicationService service = new MarkAttendanceApplicationService(
                subject -> Optional.empty(),
                occurrences,
                publisher);

        UUID unknownSubject = UUID.fromString("00000000-0000-0000-0000-000000000999");
        assertThatThrownBy(() -> service.execute(unknownSubject, OCCURRENCE_ID, PERSON_ID, true, CLIENT_MARKED_AT))
                .isInstanceOf(CallerUnknownException.class);
        assertThat(occurrences.savedOccurrences()).isEmpty();
        assertThat(publisher.published).isEmpty();
    }

    @Test
    void executingAgainstAnUnknownOccurrenceThrowsOccurrenceNotFound() {
        InMemoryOccurrenceRepository occurrences = new InMemoryOccurrenceRepository();
        CapturingPublisher publisher = new CapturingPublisher();
        MarkAttendanceApplicationService service = new MarkAttendanceApplicationService(
                staticCallerResolver(),
                occurrences,
                publisher);

        assertThatThrownBy(() -> service.execute(SUBJECT, OCCURRENCE_ID, PERSON_ID, true, CLIENT_MARKED_AT))
                .isInstanceOf(OccurrenceNotFoundException.class);
        assertThat(occurrences.savedOccurrences()).isEmpty();
        assertThat(publisher.published).isEmpty();
    }

    private static CallerResolver staticCallerResolver() {
        return subject -> subject.equals(SUBJECT) ? Optional.of(MARKED_BY) : Optional.empty();
    }

    private static final class InMemoryOccurrenceRepository implements OccurrenceRepository {
        private final Map<UUID, Occurrence> store = new HashMap<>();
        private final List<Occurrence> saved = new ArrayList<>();

        void put(Occurrence occurrence) {
            store.put(occurrence.id(), occurrence);
        }

        List<Occurrence> savedOccurrences() {
            return saved;
        }

        @Override
        public Optional<Occurrence> findById(UUID occurrenceId) {
            return Optional.ofNullable(store.get(occurrenceId));
        }

        @Override
        public void save(Occurrence occurrence) {
            store.put(occurrence.id(), occurrence);
            saved.add(occurrence);
        }
    }

    /**
     * Throws OptimisticLockException on the first {@code failures} save() calls.
     * Returns a fresh Occurrence instance on each findById to mirror real JDBC
     * load semantics (each retry sees a newly-rehydrated aggregate).
     */
    private static final class FlakyOccurrenceRepository implements OccurrenceRepository {
        private final Map<UUID, OccurrenceState> stateById = new HashMap<>();
        private final int failures;
        int saveAttempts;

        FlakyOccurrenceRepository(int failures) {
            this.failures = failures;
        }

        void put(Occurrence occurrence) {
            stateById.put(occurrence.id(), occurrence.state());
        }

        @Override
        public Optional<Occurrence> findById(UUID occurrenceId) {
            OccurrenceState state = stateById.get(occurrenceId);
            if (state == null) {
                return Optional.empty();
            }
            return Optional.of(new Occurrence(occurrenceId, SABHA_ID,
                    LocalDate.of(2026, 5, 23), state));
        }

        @Override
        public void save(Occurrence occurrence) {
            saveAttempts++;
            if (saveAttempts <= failures) {
                throw new OptimisticLockException(occurrence.id());
            }
            stateById.put(occurrence.id(), occurrence.state());
        }
    }

    private static final class CapturingPublisher implements DomainEventPublisher {
        final List<DomainEvent> published = new ArrayList<>();

        @Override
        public void publishAll(List<? extends DomainEvent> events) {
            published.addAll(events);
        }
    }
}
