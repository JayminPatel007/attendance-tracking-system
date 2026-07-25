package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.Duration;
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
import org.sabha.attendance.domain.Occurrence;
import org.sabha.attendance.domain.OccurrenceState;
import org.sabha.common.CallerResolver;
import org.sabha.common.CallerUnknownException;
import org.sabha.common.DomainEvent;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.UserActivityRecorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SyncAttendanceApplicationServiceTest {

    private static final UUID SUBJECT = UUID.fromString("00000000-0000-0000-0000-000000000300");
    private static final UUID MARKED_BY = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID OCCURRENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID SABHA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PERSON_A = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID PERSON_B = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final Instant SERVER_NOW = Instant.parse("2026-05-27T12:00:00Z");

    @Test
    void afreshRosterVersionAppliesEveryQueuedMarking() {
        Fixture f = openOccurrence();
        Instant fresh = SERVER_NOW.minus(Duration.ofHours(1));

        SyncResult result = f.service.execute(SUBJECT, fresh, List.of(
                new SyncRequestItem(OCCURRENCE_ID, PERSON_A, true, SERVER_NOW.minus(Duration.ofMinutes(10))),
                new SyncRequestItem(OCCURRENCE_ID, PERSON_B, false, SERVER_NOW.minus(Duration.ofMinutes(5)))));

        assertThat(result.appliedCount()).isEqualTo(2);
        Occurrence saved = f.occurrences.savedOccurrences().get(f.occurrences.savedOccurrences().size() - 1);
        assertThat(saved.markings()).hasSize(2);
    }

    @Test
    void aSuccessfulSyncRecordsTheSanchalaksLastSyncActivity() {
        Fixture f = openOccurrence();
        Instant fresh = SERVER_NOW.minus(Duration.ofHours(1));

        f.service.execute(SUBJECT, fresh, List.of(
                new SyncRequestItem(OCCURRENCE_ID, PERSON_A, true, SERVER_NOW.minus(Duration.ofMinutes(10)))));

        assertThat(f.recorder.syncs).containsExactly(Map.entry(MARKED_BY, SERVER_NOW));
    }

    @Test
    void aRejectedStaleSyncRecordsNoActivity() {
        Fixture f = openOccurrence();
        Instant stale = SERVER_NOW.minus(Duration.ofDays(7)).minus(Duration.ofSeconds(1));

        assertThatThrownBy(() -> f.service.execute(SUBJECT, stale, List.of(
                new SyncRequestItem(OCCURRENCE_ID, PERSON_A, true, SERVER_NOW))))
                .isInstanceOf(StaleRosterException.class);

        assertThat(f.recorder.syncs).isEmpty();
    }

    @Test
    void aRosterVersionOlderThanSevenDaysIsRejectedAndNothingIsApplied() {
        Fixture f = openOccurrence();
        Instant stale = SERVER_NOW.minus(Duration.ofDays(7)).minus(Duration.ofSeconds(1));

        assertThatThrownBy(() -> f.service.execute(SUBJECT, stale, List.of(
                new SyncRequestItem(OCCURRENCE_ID, PERSON_A, true, SERVER_NOW))))
                .isInstanceOf(StaleRosterException.class);

        assertThat(f.occurrences.savedOccurrences()).isEmpty();
        assertThat(f.publisher.published).isEmpty();
    }

    @Test
    void aRosterVersionExactlySevenDaysOldStillSyncs() {
        Fixture f = openOccurrence();
        Instant edge = SERVER_NOW.minus(Duration.ofDays(7));

        SyncResult result = f.service.execute(SUBJECT, edge, List.of(
                new SyncRequestItem(OCCURRENCE_ID, PERSON_A, true, SERVER_NOW)));

        assertThat(result.appliedCount()).isEqualTo(1);
    }

    @Test
    void replayingTheSameBatchConvergesToTheSameMarkingsViaLWW() {
        Fixture f = openOccurrence();
        Instant fresh = SERVER_NOW.minus(Duration.ofHours(1));
        Instant clientMarkedAt = SERVER_NOW.minus(Duration.ofMinutes(5));
        List<SyncRequestItem> batch = List.of(
                new SyncRequestItem(OCCURRENCE_ID, PERSON_A, true, clientMarkedAt));

        f.service.execute(SUBJECT, fresh, batch);
        f.service.execute(SUBJECT, fresh, batch);

        Occurrence saved = f.occurrences.savedOccurrences().get(f.occurrences.savedOccurrences().size() - 1);
        assertThat(saved.markings()).hasSize(1);
        assertThat(saved.markings().iterator().next().present()).isTrue();
    }

    @Test
    void aSyncBatchLoadsEachOccurrenceOnceRegardlessOfHowManyItemsItContains() {
        UUID otherOccurrenceId = UUID.fromString("00000000-0000-0000-0000-000000000021");
        InMemoryOccurrenceRepository occurrences = new InMemoryOccurrenceRepository();
        occurrences.put(new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.OPEN_FOR_MARKING));
        occurrences.put(new Occurrence(otherOccurrenceId, SABHA_ID,
                LocalDate.of(2026, 5, 24), OccurrenceState.OPEN_FOR_MARKING));
        CapturingPublisher publisher = new CapturingPublisher();
        CallerResolver resolver = subject ->
                subject.equals(SUBJECT) ? Optional.of(MARKED_BY) : Optional.empty();
        Clock clock = Clock.fixed(SERVER_NOW, ZoneOffset.UTC);
        MarkAttendanceApplicationService markUseCase =
                markAttendance(resolver, occurrences, publisher, clock);
        SyncAttendanceApplicationService service = new SyncAttendanceApplicationService(
                resolver, markUseCase, new RecordingActivity(), clock);

        Instant fresh = SERVER_NOW.minus(Duration.ofHours(1));
        SyncResult result = service.execute(SUBJECT, fresh, List.of(
                new SyncRequestItem(OCCURRENCE_ID, PERSON_A, true, SERVER_NOW.minus(Duration.ofMinutes(10))),
                new SyncRequestItem(OCCURRENCE_ID, PERSON_B, false, SERVER_NOW.minus(Duration.ofMinutes(9))),
                new SyncRequestItem(otherOccurrenceId, PERSON_A, true, SERVER_NOW.minus(Duration.ofMinutes(8))),
                new SyncRequestItem(otherOccurrenceId, PERSON_B, false, SERVER_NOW.minus(Duration.ofMinutes(7)))));

        assertThat(result.appliedCount()).isEqualTo(4);
        assertThat(occurrences.loadCount(OCCURRENCE_ID)).isEqualTo(1);
        assertThat(occurrences.loadCount(otherOccurrenceId)).isEqualTo(1);
        assertThat(occurrences.saveCount(OCCURRENCE_ID)).isEqualTo(1);
        assertThat(occurrences.saveCount(otherOccurrenceId)).isEqualTo(1);
    }

    @Test
    void anUnknownKeycloakSubjectIsRejectedBeforeAnyMarkingsAreApplied() {
        Fixture f = openOccurrence();
        SyncAttendanceApplicationService service = new SyncAttendanceApplicationService(
                subject -> Optional.empty(),
                f.markUseCase,
                new RecordingActivity(),
                Clock.fixed(SERVER_NOW, ZoneOffset.UTC));

        UUID unknown = UUID.fromString("00000000-0000-0000-0000-000000000999");
        assertThatThrownBy(() -> service.execute(unknown, SERVER_NOW, List.of(
                new SyncRequestItem(OCCURRENCE_ID, PERSON_A, true, SERVER_NOW))))
                .isInstanceOf(CallerUnknownException.class);
        assertThat(f.occurrences.savedOccurrences()).isEmpty();
    }

    private Fixture openOccurrence() {
        InMemoryOccurrenceRepository occurrences = new InMemoryOccurrenceRepository();
        occurrences.put(new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.OPEN_FOR_MARKING));
        CapturingPublisher publisher = new CapturingPublisher();
        CallerResolver resolver = subject ->
                subject.equals(SUBJECT) ? Optional.of(MARKED_BY) : Optional.empty();
        Clock clock = Clock.fixed(SERVER_NOW, ZoneOffset.UTC);
        MarkAttendanceApplicationService markUseCase =
                markAttendance(resolver, occurrences, publisher, clock);
        RecordingActivity recorder = new RecordingActivity();
        SyncAttendanceApplicationService service = new SyncAttendanceApplicationService(
                resolver, markUseCase, recorder, clock);
        return new Fixture(occurrences, publisher, markUseCase, recorder, service);
    }

    private static MarkAttendanceApplicationService markAttendance(
            CallerResolver resolver, OccurrenceRepository occurrences,
            DomainEventPublisher publisher, Clock clock) {
        return new MarkAttendanceApplicationService(OccurrenceWriterTest.unauthorizedWriter(
                resolver, occurrences, new OccurrenceWriterTest.InMemoryTransitionLog(),
                publisher, clock));
    }

    private record Fixture(
            InMemoryOccurrenceRepository occurrences,
            CapturingPublisher publisher,
            MarkAttendanceApplicationService markUseCase,
            RecordingActivity recorder,
            SyncAttendanceApplicationService service) {
    }

    private static final class RecordingActivity implements UserActivityRecorder {
        final List<Map.Entry<UUID, Instant>> syncs = new ArrayList<>();
        final List<Map.Entry<UUID, Instant>> logins = new ArrayList<>();

        @Override
        public void recordSync(UUID userId, Instant at) {
            syncs.add(Map.entry(userId, at));
        }

        @Override
        public void recordLogin(UUID userId, Instant at) {
            logins.add(Map.entry(userId, at));
        }
    }

    private static final class InMemoryOccurrenceRepository implements OccurrenceRepository {
        private final Map<UUID, Occurrence> store = new HashMap<>();
        private final List<Occurrence> saved = new ArrayList<>();
        private final Map<UUID, Integer> loadCounts = new HashMap<>();
        private final Map<UUID, Integer> saveCounts = new HashMap<>();

        void put(Occurrence occurrence) {
            store.put(occurrence.id(), occurrence);
        }

        List<Occurrence> savedOccurrences() {
            return saved;
        }

        int loadCount(UUID occurrenceId) {
            return loadCounts.getOrDefault(occurrenceId, 0);
        }

        int saveCount(UUID occurrenceId) {
            return saveCounts.getOrDefault(occurrenceId, 0);
        }

        @Override
        public Optional<Occurrence> findById(UUID occurrenceId) {
            loadCounts.merge(occurrenceId, 1, Integer::sum);
            return Optional.ofNullable(store.get(occurrenceId));
        }

        @Override
        public void save(Occurrence occurrence) {
            saveCounts.merge(occurrence.id(), 1, Integer::sum);
            store.put(occurrence.id(), occurrence);
            saved.add(occurrence);
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
