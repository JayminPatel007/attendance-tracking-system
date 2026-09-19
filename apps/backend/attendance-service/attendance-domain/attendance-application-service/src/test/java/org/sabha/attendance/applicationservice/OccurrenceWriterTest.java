package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.attendance.domain.InvalidOccurrenceTransitionException;
import org.sabha.attendance.domain.Occurrence;
import org.sabha.attendance.domain.OccurrenceOpened;
import org.sabha.attendance.domain.OccurrenceState;
import org.sabha.attendance.domain.Reason;
import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.AuthorizedAction;
import org.sabha.common.CallerUnknownException;
import org.sabha.common.ConcurrentModificationException;
import org.sabha.common.DomainEvent;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.NirikshakAssignmentLookup;
import org.sabha.common.OptimisticLockException;
import org.sabha.common.Role;
import org.sabha.common.CallerAuthority;
import org.sabha.common.RoleAssignment;
import org.sabha.common.SabhaScope;
import org.sabha.common.SabhaFact;
import org.sabha.common.SanchalakLookup;
import org.sabha.common.SabhaFacts;

import org.sabha.common.UserId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The one place the Occurrence write contract is pinned down: retry on
 * optimistic-lock conflict, the audit row, and event publication. Every caller
 * (shaping, reopen, the cron scanners, attendance marking) rides this path, so
 * the concurrency contract is asserted here and nowhere else.
 */
class OccurrenceWriterTest {

    private static final UUID OCCURRENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID SABHA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID KSHETRA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SANCHALAK_USER = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final CallerAuthority SANCHALAK_CALLER = new CallerAuthority(
            UserId.of(SANCHALAK_USER), List.of(new RoleAssignment("SANCHALAK", SABHA_ID, null, null, null, null)));
    private static final UUID NIRIKSHAK_USER = UUID.fromString("00000000-0000-0000-0000-000000000031");
    /** The proxy holds no row on the Sabha — its authority is the assignment. */
    private static final CallerAuthority NIRIKSHAK_CALLER =
            CallerAuthority.withNoRoles(UserId.of(NIRIKSHAK_USER));
    private static final UUID STRANGER_USER = UUID.fromString("00000000-0000-0000-0000-000000000052");
    private static final CallerAuthority STRANGER_CALLER =
            CallerAuthority.withNoRoles(UserId.of(STRANGER_USER));
    private static final LocalDate OCCURRENCE_DATE = LocalDate.of(2026, 5, 26);
    private static final Instant FIXED_NOW = Instant.parse("2026-05-26T09:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    // --- the system (cron) actor ------------------------------------------

    @Test
    void aSystemTransitionSavesAppendsAnAuditRowAndPublishes() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.SCHEDULED);

        f.writer().transition(OCCURRENCE_ID, TransitionActor.system(),
                OccurrenceAction.OPEN, Occurrence::open);

        assertThat(f.occurrences.saved).singleElement()
                .extracting(Occurrence::state).isEqualTo(OccurrenceState.OPEN_FOR_MARKING);
        assertThat(f.transitions.appended).hasSize(1);
        OccurrenceStateTransition row = f.transitions.appended.get(0);
        assertThat(row.occurrenceId()).isEqualTo(OCCURRENCE_ID);
        assertThat(row.fromState()).isEqualTo(OccurrenceState.SCHEDULED);
        assertThat(row.toState()).isEqualTo(OccurrenceState.OPEN_FOR_MARKING);
        assertThat(row.action()).isEqualTo(OccurrenceAction.OPEN);
        assertThat(row.actorKind()).isEqualTo(ActorKind.SYSTEM);
        assertThat(row.actorUserId()).isNull();
        assertThat(row.onBehalfOfUserId()).isNull();
        assertThat(row.reason()).isNull();
        assertThat(row.at()).isEqualTo(FIXED_NOW);
        assertThat(f.publisher.published).singleElement().isInstanceOf(OccurrenceOpened.class);
    }

    // --- the user actor ----------------------------------------------------

    @Test
    void aUserTransitionResolvesTheCallerAuthorizesAndAuditsUnderTheirUserId() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.SCHEDULED);

        f.writer().transition(OCCURRENCE_ID,
                TransitionActor.user(SANCHALAK_CALLER, AuthorizedAction.CANCEL),
                OccurrenceAction.CANCEL, new Reason("Hall flooded"), Occurrence::cancel);

        assertThat(f.occurrences.saved).singleElement()
                .extracting(Occurrence::state).isEqualTo(OccurrenceState.CANCELLED);
        OccurrenceStateTransition row = f.transitions.appended.get(0);
        assertThat(row.action()).isEqualTo(OccurrenceAction.CANCEL);
        assertThat(row.actorKind()).isEqualTo(ActorKind.USER);
        assertThat(row.actorUserId()).isEqualTo(SANCHALAK_USER);
        assertThat(row.onBehalfOfUserId()).isNull();
        assertThat(row.reason()).isEqualTo("Hall flooded");
    }

    @Test
    void aProxyingNirikshakIsAuditedAsActingForTheAbsentSanchalak() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.SCHEDULED);

        f.writer().transition(OCCURRENCE_ID,
                TransitionActor.user(NIRIKSHAK_CALLER, AuthorizedAction.CANCEL),
                OccurrenceAction.CANCEL, new Reason("Sanchalak unreachable"), Occurrence::cancel);

        OccurrenceStateTransition row = f.transitions.appended.get(0);
        assertThat(row.actorUserId()).isEqualTo(NIRIKSHAK_USER);
        assertThat(row.onBehalfOfUserId()).isEqualTo(SANCHALAK_USER);
    }

    @Test
    void anUnauthorizedUserTransitionIsRejectedWithNoSideEffects() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.SCHEDULED);

        assertThatThrownBy(() -> f.writer().transition(OCCURRENCE_ID,
                TransitionActor.user(STRANGER_CALLER, AuthorizedAction.CANCEL),
                OccurrenceAction.CANCEL, new Reason("let me in"), Occurrence::cancel))
                .isInstanceOf(AuthorizationDeniedException.class);

        assertThat(f.occurrences.saved).isEmpty();
        assertThat(f.transitions.appended).isEmpty();
        assertThat(f.publisher.published).isEmpty();
    }

    // --- failure modes shared by every caller ------------------------------

    @Test
    void anUnknownOccurrenceThrowsOccurrenceNotFound() {
        Fixture f = new Fixture();

        assertThatThrownBy(() -> f.writer().transition(OCCURRENCE_ID, TransitionActor.system(),
                OccurrenceAction.OPEN, Occurrence::open))
                .isInstanceOf(OccurrenceNotFoundException.class);

        assertThat(f.transitions.appended).isEmpty();
        assertThat(f.publisher.published).isEmpty();
    }

    @Test
    void aMutationInvalidForTheCurrentStatePropagatesAndLeavesNoSideEffects() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.FINALIZED);

        assertThatThrownBy(() -> f.writer().transition(OCCURRENCE_ID, TransitionActor.system(),
                OccurrenceAction.OPEN, Occurrence::open))
                .isInstanceOf(InvalidOccurrenceTransitionException.class);

        assertThat(f.occurrences.saved).isEmpty();
        assertThat(f.transitions.appended).isEmpty();
        assertThat(f.publisher.published).isEmpty();
    }

    // --- the concurrency contract, asserted once for every caller ----------

    @Test
    void retriesOnOptimisticLockConflictAndSucceedsBeforeMaxAttempts() {
        Fixture f = Fixture.withFlakyOccurrence(OccurrenceState.SCHEDULED, 1);

        f.writer().transition(OCCURRENCE_ID, TransitionActor.system(),
                OccurrenceAction.OPEN, Occurrence::open);

        assertThat(f.occurrences.saveAttempts).isEqualTo(2);
        assertThat(f.transitions.appended).hasSize(1);
        assertThat(f.publisher.published).singleElement().isInstanceOf(OccurrenceOpened.class);
    }

    @Test
    void givesUpAfterThreeOptimisticLockConflictsAndSurfacesConcurrentModification() {
        Fixture f = Fixture.withFlakyOccurrence(OccurrenceState.SCHEDULED, 99);

        assertThatThrownBy(() -> f.writer().transition(OCCURRENCE_ID, TransitionActor.system(),
                OccurrenceAction.OPEN, Occurrence::open))
                .isInstanceOf(ConcurrentModificationException.class);

        assertThat(f.occurrences.saveAttempts).isEqualTo(3);
        assertThat(f.transitions.appended).isEmpty();
        assertThat(f.publisher.published).isEmpty();
    }

    @Test
    void anUnauditedMutationSharesTheSameRetryContract() {
        Fixture f = Fixture.withFlakyOccurrence(OccurrenceState.OPEN_FOR_MARKING, 99);

        assertThatThrownBy(() -> f.writer().mutateUnaudited(OCCURRENCE_ID, SANCHALAK_CALLER.userId(),
                (occurrence, actorUserId) -> occurrence.markWalkIn(
                        UUID.randomUUID(), actorUserId, FIXED_NOW)))
                .isInstanceOf(ConcurrentModificationException.class);

        assertThat(f.occurrences.saveAttempts).isEqualTo(3);
        assertThat(f.publisher.published).isEmpty();
    }

    @Test
    void theAuditRowIsStampedWhenTheSaveSticksNotBeforeTheRetriedAttempts() {
        Fixture f = Fixture.withFlakyOccurrence(OccurrenceState.SCHEDULED, 1);
        AdvancingClock clock = new AdvancingClock(FIXED_NOW);
        // Every save attempt — the conflicting one and the one that sticks —
        // costs a minute, so a row stamped before the save reads a minute early.
        f.occurrences.beforeSave = () -> clock.advance(Duration.ofMinutes(1));

        f.writer(clock).transition(OCCURRENCE_ID, TransitionActor.system(),
                OccurrenceAction.OPEN, Occurrence::open);

        assertThat(f.occurrences.saveAttempts).isEqualTo(2);
        assertThat(f.transitions.appended.get(0).at())
                .isEqualTo(FIXED_NOW.plus(Duration.ofMinutes(2)));
    }

    // --- the unaudited (marking) path --------------------------------------

    @Test
    void anUnauditedMutationSavesAndPublishesButAppendsNoAuditRow() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.OPEN_FOR_MARKING);
        UUID personId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        List<UUID> markedBy = new ArrayList<>();

        f.writer().mutateUnaudited(OCCURRENCE_ID, SANCHALAK_CALLER.userId(), (occurrence, actorUserId) -> {
            markedBy.add(actorUserId);
            occurrence.mark(personId, true, actorUserId, FIXED_NOW);
        });

        assertThat(markedBy).containsExactly(SANCHALAK_USER);
        assertThat(f.occurrences.saved).hasSize(1);
        assertThat(f.transitions.appended).isEmpty();
        assertThat(f.publisher.published).hasSize(1);
    }

    // --- fixture -----------------------------------------------------------

    /**
     * Wires a real {@link AuthorizationEngine} over lookup fakes in which
     * SANCHALAK_USER is the Sabha's Sanchalak, NIRIKSHAK_USER is a Nirikshak
     * assigned to it as a proxy, and STRANGER_USER holds nothing.
     */
    static final class Fixture {
        final RecordingOccurrenceRepository occurrences;
        final InMemoryTransitionLog transitions = new InMemoryTransitionLog();
        final CapturingPublisher publisher = new CapturingPublisher();

        private Fixture() {
            this(new RecordingOccurrenceRepository(0));
        }

        private Fixture(RecordingOccurrenceRepository occurrences) {
            this.occurrences = occurrences;
        }

        static Fixture withOccurrence(OccurrenceState state) {
            Fixture f = new Fixture();
            f.occurrences.put(new Occurrence(OCCURRENCE_ID, SABHA_ID, OCCURRENCE_DATE, state, 0L, List.of()));
            return f;
        }

        /** Seeds one Occurrence whose first {@code failures} saves conflict. */
        static Fixture withFlakyOccurrence(OccurrenceState state, int failures) {
            Fixture f = new Fixture(new RecordingOccurrenceRepository(failures));
            f.occurrences.put(new Occurrence(OCCURRENCE_ID, SABHA_ID, OCCURRENCE_DATE, state, 0L, List.of()));
            return f;
        }

        OccurrenceWriter writer() {
            return writer(FIXED_CLOCK);
        }

        OccurrenceWriter writer(Clock clock) {
            // Three methods became one: the two caller-keyed reads are on the
            // caller now, and what is left is keyed by the target Sabha (ADR-0032).
            SanchalakLookup roles = sabhaId ->
                    sabhaId.equals(SABHA_ID) ? Optional.of(SANCHALAK_USER) : Optional.empty();
            SabhaFacts sabhaFacts = new SabhaFacts() {
                @Override
                public Optional<SabhaFact> of(UUID sabhaId) {
                return sabhaId.equals(SABHA_ID)
                        ? Optional.of(SabhaFact.monthlyAdHoc(
                                sabhaId, new SabhaScope(KSHETRA_ID, "YUVAK", "REGULAR"), false))
                        : Optional.empty();
                }

                @Override
                public List<SabhaFact> allWeekly() {
                    return List.of();
                }

                @Override
                public Optional<SabhaFact> selectiveIn(UUID kshetraId, String demographic, String track) {
                    return Optional.empty();
                }
            };
            NirikshakAssignmentLookup nirikshakAssignments = (userId, sabhaId) ->
                    userId.equals(NIRIKSHAK_USER) && sabhaId.equals(SABHA_ID);
            return new OccurrenceWriter(
                    new AuthorizationEngine(roles, sabhaFacts, nirikshakAssignments),
                    occurrences, transitions, publisher, clock);
        }
    }

    /**
     * A writer wired for the cron path alone: the authorization engine's lookups
     * reject everyone, so a write that lands through this writer proves the
     * SYSTEM actor consults it not at all. Shared with
     * the scanner tests, which drive the cron end of the same write path.
     */
    static OccurrenceWriter cronWriter(OccurrenceRepository occurrences,
                                       OccurrenceStateTransitionRepository transitions,
                                       DomainEventPublisher events,
                                       Clock clock) {
        return unauthorizedWriter(occurrences, transitions, events, clock);
    }

    /**
     * A writer whose {@link AuthorizationEngine} grants nothing, for the two write
     * paths that never consult it: the SYSTEM cron actor and the unaudited marking
     * path. Shared with {@link MarkAttendanceApplicationServiceTest}.
     */
    static OccurrenceWriter unauthorizedWriter(OccurrenceRepository occurrences,
                                               OccurrenceStateTransitionRepository transitions,
                                               DomainEventPublisher events,
                                               Clock clock) {
        // Nobody runs this Sabha, so no proxy attribution is possible either.
        SanchalakLookup noSanchalak = sabhaId -> Optional.empty();
        SabhaFacts noSabhaFacts = new SabhaFacts() {
            @Override
            public Optional<SabhaFact> of(UUID sabhaId) {
                return Optional.empty();
            }

            @Override
            public List<SabhaFact> allWeekly() {
                return List.of();
            }

            @Override
            public Optional<SabhaFact> selectiveIn(UUID kshetraId, String demographic, String track) {
                return Optional.empty();
            }
        };
        NirikshakAssignmentLookup noProxy = (userId, sabhaId) -> false;
        return new OccurrenceWriter(
                new AuthorizationEngine(noSanchalak, noSabhaFacts, noProxy),
                occurrences, transitions, events, clock);
    }

    /**
     * Counts loads and save attempts, and conflicts on the first {@code failures}
     * saves. Each {@code findById} returns a freshly-rehydrated aggregate, mirroring
     * real JDBC load semantics so a retry sees a clean copy.
     */
    static final class RecordingOccurrenceRepository implements OccurrenceRepository {
        final Map<UUID, Occurrence> store = new HashMap<>();
        final List<Occurrence> saved = new ArrayList<>();
        private final int failures;
        /** Run on entry to every save attempt, so a test can move the clock. */
        Runnable beforeSave = () -> { };
        int loads;
        int saveAttempts;

        RecordingOccurrenceRepository() {
            this(0);
        }

        RecordingOccurrenceRepository(int failures) {
            this.failures = failures;
        }

        void put(Occurrence occurrence) {
            store.put(occurrence.id(), occurrence);
        }

        @Override
        public Optional<Occurrence> findById(UUID occurrenceId) {
            loads++;
            Occurrence held = store.get(occurrenceId);
            if (held == null) {
                return Optional.empty();
            }
            Occurrence copy = new Occurrence(held.id(), held.sabhaId(), held.date(), held.state(),
                    held.version(), held.markings());
            copy.restoreShaping(held.venueOverride(), held.rescheduledDate(),
                    held.rescheduledStartTime(), held.rescheduledEndTime());
            return Optional.of(copy);
        }

        @Override
        public void save(Occurrence occurrence) {
            beforeSave.run();
            saveAttempts++;
            if (saveAttempts <= failures) {
                throw new OptimisticLockException(occurrence.id());
            }
            store.put(occurrence.id(), occurrence);
            saved.add(occurrence);
        }
    }

    /** A clock the test moves by hand, to date events relative to each other. */
    static final class AdvancingClock extends Clock {
        private Instant now;

        AdvancingClock(Instant start) {
            this.now = start;
        }

        void advance(Duration by) {
            now = now.plus(by);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
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
