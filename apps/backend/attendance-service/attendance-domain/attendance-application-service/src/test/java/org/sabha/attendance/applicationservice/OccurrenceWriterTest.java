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
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.attendance.domain.InvalidOccurrenceTransitionException;
import org.sabha.attendance.domain.Occurrence;
import org.sabha.attendance.domain.OccurrenceOpened;
import org.sabha.attendance.domain.OccurrenceState;
import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.AuthorizedAction;
import org.sabha.common.CallerResolver;
import org.sabha.common.CallerUnknownException;
import org.sabha.common.ConcurrentModificationException;
import org.sabha.common.DomainEvent;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.NirikshakAssignmentLookup;
import org.sabha.common.OptimisticLockException;
import org.sabha.common.Role;
import org.sabha.common.RoleAssignmentLookup;
import org.sabha.common.SabhaScope;
import org.sabha.common.StructuralHierarchyLookup;

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
    private static final UUID SANCHALAK_SUBJECT = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID SANCHALAK_USER = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID NIRIKSHAK_SUBJECT = UUID.fromString("00000000-0000-0000-0000-000000000041");
    private static final UUID NIRIKSHAK_USER = UUID.fromString("00000000-0000-0000-0000-000000000031");
    private static final UUID STRANGER_SUBJECT = UUID.fromString("00000000-0000-0000-0000-000000000051");
    private static final UUID STRANGER_USER = UUID.fromString("00000000-0000-0000-0000-000000000052");
    private static final LocalDate OCCURRENCE_DATE = LocalDate.of(2026, 5, 26);
    private static final Instant FIXED_NOW = Instant.parse("2026-05-26T09:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    // --- the system (cron) actor ------------------------------------------

    @Test
    void aSystemTransitionSavesAppendsAnAuditRowAndPublishes() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.SCHEDULED);

        f.writer().transition(OCCURRENCE_ID, TransitionActor.system(),
                OccurrenceAction.OPEN, null, Occurrence::open);

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
                TransitionActor.user(SANCHALAK_SUBJECT, AuthorizedAction.CANCEL),
                OccurrenceAction.CANCEL, "Hall flooded", Occurrence::cancel);

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
                TransitionActor.user(NIRIKSHAK_SUBJECT, AuthorizedAction.CANCEL),
                OccurrenceAction.CANCEL, "Sanchalak unreachable", Occurrence::cancel);

        OccurrenceStateTransition row = f.transitions.appended.get(0);
        assertThat(row.actorUserId()).isEqualTo(NIRIKSHAK_USER);
        assertThat(row.onBehalfOfUserId()).isEqualTo(SANCHALAK_USER);
    }

    @Test
    void anUnauthorizedUserTransitionIsRejectedWithNoSideEffects() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.SCHEDULED);

        assertThatThrownBy(() -> f.writer().transition(OCCURRENCE_ID,
                TransitionActor.user(STRANGER_SUBJECT, AuthorizedAction.CANCEL),
                OccurrenceAction.CANCEL, "let me in", Occurrence::cancel))
                .isInstanceOf(AuthorizationDeniedException.class);

        assertThat(f.occurrences.saved).isEmpty();
        assertThat(f.transitions.appended).isEmpty();
        assertThat(f.publisher.published).isEmpty();
    }

    @Test
    void anUnknownKeycloakSubjectIsRejectedBeforeTheOccurrenceIsLoaded() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.SCHEDULED);
        UUID unknownSubject = UUID.fromString("00000000-0000-0000-0000-000000000999");

        assertThatThrownBy(() -> f.writer().transition(OCCURRENCE_ID,
                TransitionActor.user(unknownSubject, AuthorizedAction.CANCEL),
                OccurrenceAction.CANCEL, "who am I", Occurrence::cancel))
                .isInstanceOf(CallerUnknownException.class);

        assertThat(f.occurrences.loads).isZero();
        assertThat(f.occurrences.saved).isEmpty();
    }

    // --- failure modes shared by every caller ------------------------------

    @Test
    void anUnknownOccurrenceThrowsOccurrenceNotFound() {
        Fixture f = new Fixture();

        assertThatThrownBy(() -> f.writer().transition(OCCURRENCE_ID, TransitionActor.system(),
                OccurrenceAction.OPEN, null, Occurrence::open))
                .isInstanceOf(OccurrenceNotFoundException.class);

        assertThat(f.transitions.appended).isEmpty();
        assertThat(f.publisher.published).isEmpty();
    }

    @Test
    void aMutationInvalidForTheCurrentStatePropagatesAndLeavesNoSideEffects() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.FINALIZED);

        assertThatThrownBy(() -> f.writer().transition(OCCURRENCE_ID, TransitionActor.system(),
                OccurrenceAction.OPEN, null, Occurrence::open))
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
                OccurrenceAction.OPEN, null, Occurrence::open);

        assertThat(f.occurrences.saveAttempts).isEqualTo(2);
        assertThat(f.transitions.appended).hasSize(1);
        assertThat(f.publisher.published).singleElement().isInstanceOf(OccurrenceOpened.class);
    }

    @Test
    void givesUpAfterThreeOptimisticLockConflictsAndSurfacesConcurrentModification() {
        Fixture f = Fixture.withFlakyOccurrence(OccurrenceState.SCHEDULED, 99);

        assertThatThrownBy(() -> f.writer().transition(OCCURRENCE_ID, TransitionActor.system(),
                OccurrenceAction.OPEN, null, Occurrence::open))
                .isInstanceOf(ConcurrentModificationException.class);

        assertThat(f.occurrences.saveAttempts).isEqualTo(3);
        assertThat(f.transitions.appended).isEmpty();
        assertThat(f.publisher.published).isEmpty();
    }

    @Test
    void anUnauditedMutationSharesTheSameRetryContract() {
        Fixture f = Fixture.withFlakyOccurrence(OccurrenceState.OPEN_FOR_MARKING, 99);

        assertThatThrownBy(() -> f.writer().mutate(OCCURRENCE_ID, SANCHALAK_SUBJECT,
                (occurrence, actorUserId) -> occurrence.markWalkIn(
                        UUID.randomUUID(), actorUserId, FIXED_NOW)))
                .isInstanceOf(ConcurrentModificationException.class);

        assertThat(f.occurrences.saveAttempts).isEqualTo(3);
        assertThat(f.publisher.published).isEmpty();
    }

    // --- the unaudited (marking) path --------------------------------------

    @Test
    void anUnauditedMutationSavesAndPublishesButAppendsNoAuditRow() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.OPEN_FOR_MARKING);
        UUID personId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        List<UUID> markedBy = new ArrayList<>();

        f.writer().mutate(OCCURRENCE_ID, SANCHALAK_SUBJECT, (occurrence, actorUserId) -> {
            markedBy.add(actorUserId);
            occurrence.mark(personId, true, actorUserId, FIXED_NOW);
        });

        assertThat(markedBy).containsExactly(SANCHALAK_USER);
        assertThat(f.occurrences.saved).hasSize(1);
        assertThat(f.transitions.appended).isEmpty();
        assertThat(f.publisher.published).hasSize(1);
    }

    @Test
    void anUnauditedMutationWithAnUnknownSubjectIsRejectedBeforeTheOccurrenceIsLoaded() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.OPEN_FOR_MARKING);
        UUID unknownSubject = UUID.fromString("00000000-0000-0000-0000-000000000999");

        assertThatThrownBy(() -> f.writer().mutate(OCCURRENCE_ID, unknownSubject,
                (occurrence, actorUserId) -> occurrence.mark(
                        UUID.randomUUID(), true, actorUserId, FIXED_NOW)))
                .isInstanceOf(CallerUnknownException.class);

        assertThat(f.occurrences.loads).isZero();
        assertThat(f.occurrences.saved).isEmpty();
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
            CallerResolver callerResolver = subject -> {
                if (subject.equals(SANCHALAK_SUBJECT)) {
                    return Optional.of(SANCHALAK_USER);
                }
                if (subject.equals(NIRIKSHAK_SUBJECT)) {
                    return Optional.of(NIRIKSHAK_USER);
                }
                if (subject.equals(STRANGER_SUBJECT)) {
                    return Optional.of(STRANGER_USER);
                }
                return Optional.empty();
            };
            RoleAssignmentLookup roles = new RoleAssignmentLookup() {
                @Override
                public Set<Role> rolesForUserOnSabha(UUID userId, UUID sabhaId) {
                    return userId.equals(SANCHALAK_USER) && sabhaId.equals(SABHA_ID)
                            ? Set.of(Role.SANCHALAK) : Set.of();
                }

                @Override
                public Set<Role> rolesForUserOnKshetra(UUID userId, UUID kshetraId, String demographic) {
                    return Set.of();
                }

                @Override
                public Optional<UUID> sanchalakOf(UUID sabhaId) {
                    return sabhaId.equals(SABHA_ID) ? Optional.of(SANCHALAK_USER) : Optional.empty();
                }
            };
            StructuralHierarchyLookup hierarchy = new StructuralHierarchyLookup() {
                @Override
                public Optional<SabhaScope> sabhaScope(UUID sabhaId) {
                    return sabhaId.equals(SABHA_ID)
                            ? Optional.of(new SabhaScope(KSHETRA_ID, "YUVAK", "REGULAR"))
                            : Optional.empty();
                }

                @Override
                public Optional<UUID> zoneOfKshetra(UUID kshetraId) {
                    return Optional.empty();
                }

                @Override
                public Optional<UUID> cityOfZone(UUID zoneId) {
                    return Optional.empty();
                }
            };
            NirikshakAssignmentLookup nirikshakAssignments = new NirikshakAssignmentLookup() {
                @Override
                public boolean isAssignedTo(UUID userId, UUID sabhaId) {
                    return userId.equals(NIRIKSHAK_USER) && sabhaId.equals(SABHA_ID);
                }

                @Override
                public Set<UUID> sabhasAssignedTo(UUID userId) {
                    return userId.equals(NIRIKSHAK_USER) ? Set.of(SABHA_ID) : Set.of();
                }
            };
            return new OccurrenceWriter(callerResolver,
                    new AuthorizationEngine(roles, hierarchy, nirikshakAssignments),
                    occurrences, transitions, publisher, FIXED_CLOCK);
        }
    }

    /**
     * A writer wired for the cron path alone: the caller resolver and the
     * authorization engine's lookups reject everyone, so a write that lands
     * through this writer proves the SYSTEM actor consults neither. Shared with
     * the scanner tests, which drive the cron end of the same write path.
     */
    static OccurrenceWriter cronWriter(OccurrenceRepository occurrences,
                                       OccurrenceStateTransitionRepository transitions,
                                       DomainEventPublisher events,
                                       Clock clock) {
        return unauthorizedWriter(subject -> Optional.empty(),
                occurrences, transitions, events, clock);
    }

    /**
     * A writer whose {@link AuthorizationEngine} grants nothing, for the two write
     * paths that never consult it: the SYSTEM cron actor and the unaudited marking
     * path. Shared with {@link MarkAttendanceApplicationServiceTest}.
     */
    static OccurrenceWriter unauthorizedWriter(CallerResolver callerResolver,
                                               OccurrenceRepository occurrences,
                                               OccurrenceStateTransitionRepository transitions,
                                               DomainEventPublisher events,
                                               Clock clock) {
        RoleAssignmentLookup noRoles = new RoleAssignmentLookup() {
            @Override
            public Set<Role> rolesForUserOnSabha(UUID userId, UUID sabhaId) {
                return Set.of();
            }

            @Override
            public Set<Role> rolesForUserOnKshetra(UUID userId, UUID kshetraId, String demographic) {
                return Set.of();
            }
        };
        StructuralHierarchyLookup noHierarchy = new StructuralHierarchyLookup() {
            @Override
            public Optional<SabhaScope> sabhaScope(UUID sabhaId) {
                return Optional.empty();
            }

            @Override
            public Optional<UUID> zoneOfKshetra(UUID kshetraId) {
                return Optional.empty();
            }

            @Override
            public Optional<UUID> cityOfZone(UUID zoneId) {
                return Optional.empty();
            }
        };
        NirikshakAssignmentLookup noProxy = new NirikshakAssignmentLookup() {
            @Override
            public boolean isAssignedTo(UUID userId, UUID sabhaId) {
                return false;
            }

            @Override
            public Set<UUID> sabhasAssignedTo(UUID userId) {
                return Set.of();
            }
        };
        return new OccurrenceWriter(callerResolver,
                new AuthorizationEngine(noRoles, noHierarchy, noProxy),
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

        List<Occurrence> savedOccurrences() {
            return saved;
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
            saveAttempts++;
            if (saveAttempts <= failures) {
                throw new OptimisticLockException(occurrence.id());
            }
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
