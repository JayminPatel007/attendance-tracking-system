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
import org.sabha.attendance.domain.Occurrence;
import org.sabha.attendance.domain.OccurrenceReopened;
import org.sabha.attendance.domain.OccurrenceState;
import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.CallerResolver;
import org.sabha.common.DomainEvent;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.Role;
import org.sabha.common.RoleAssignmentLookup;
import org.sabha.common.SabhaScope;
import org.sabha.common.StructuralHierarchyLookup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OccurrenceReopenServiceTest {

    private static final UUID OCCURRENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID SABHA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID KSHETRA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String DEMOGRAPHIC = "YUVAK";
    private static final LocalDate OCCURRENCE_DATE = LocalDate.of(2026, 5, 24);
    private static final UUID NIRIKSHAK_SUBJECT = UUID.fromString("00000000-0000-0000-0000-000000000041");
    private static final UUID NIRIKSHAK_USER = UUID.fromString("00000000-0000-0000-0000-000000000031");
    private static final UUID SANCHALAK_SUBJECT = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID SANCHALAK_USER = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final Instant NOW = Instant.parse("2026-05-26T08:00:00Z");

    @Test
    void aNirikshakReopensAFinalizedOccurrenceWithReasonAndAnAuditRowIsAppended() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.FINALIZED);

        f.service().reopen(NIRIKSHAK_SUBJECT, OCCURRENCE_ID, "Forgot to mark Ravi");

        assertThat(f.occurrences.saved).singleElement()
                .extracting(Occurrence::state).isEqualTo(OccurrenceState.OPEN_FOR_MARKING);
        assertThat(f.transitions.appended).hasSize(1);
        OccurrenceStateTransition row = f.transitions.appended.get(0);
        assertThat(row.fromState()).isEqualTo(OccurrenceState.FINALIZED);
        assertThat(row.toState()).isEqualTo(OccurrenceState.OPEN_FOR_MARKING);
        assertThat(row.action()).isEqualTo(OccurrenceAction.REOPEN);
        assertThat(row.actorKind()).isEqualTo(ActorKind.USER);
        assertThat(row.actorUserId()).isEqualTo(NIRIKSHAK_USER);
        assertThat(row.reason()).isEqualTo("Forgot to mark Ravi");
        assertThat(f.publisher.published).singleElement().isInstanceOf(OccurrenceReopened.class);
    }

    @Test
    void reopenWithoutAReasonIsRejectedWithNoSideEffects() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.FINALIZED);

        assertThatThrownBy(() -> f.service().reopen(NIRIKSHAK_SUBJECT, OCCURRENCE_ID, "  "))
                .isInstanceOf(ReopenReasonRequiredException.class);

        assertThat(f.occurrences.saved).isEmpty();
        assertThat(f.transitions.appended).isEmpty();
        assertThat(f.publisher.published).isEmpty();
    }

    @Test
    void aSanchalakReopenIsRejectedWithNoSideEffects() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.FINALIZED);

        assertThatThrownBy(() -> f.service().reopen(SANCHALAK_SUBJECT, OCCURRENCE_ID, "let me in"))
                .isInstanceOf(AuthorizationDeniedException.class);

        assertThat(f.occurrences.saved).isEmpty();
        assertThat(f.transitions.appended).isEmpty();
        assertThat(f.publisher.published).isEmpty();
    }

    // --- fixture -----------------------------------------------------------

    static final class Fixture {
        final InMemoryOccurrenceRepository occurrences = new InMemoryOccurrenceRepository();
        final InMemoryTransitionLog transitions = new InMemoryTransitionLog();
        final CapturingPublisher publisher = new CapturingPublisher();

        static Fixture withOccurrence(OccurrenceState state) {
            Fixture f = new Fixture();
            f.occurrences.put(new Occurrence(OCCURRENCE_ID, SABHA_ID, OCCURRENCE_DATE, state, 0L, List.of()));
            return f;
        }

        OccurrenceReopenService service() {
            CallerResolver callerResolver = subject -> {
                if (subject.equals(NIRIKSHAK_SUBJECT)) {
                    return Optional.of(NIRIKSHAK_USER);
                }
                if (subject.equals(SANCHALAK_SUBJECT)) {
                    return Optional.of(SANCHALAK_USER);
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
                    return userId.equals(NIRIKSHAK_USER) && kshetraId.equals(KSHETRA_ID)
                            && demographic.equals(DEMOGRAPHIC)
                            ? Set.of(Role.NIRIKSHAK) : Set.of();
                }
            };
            StructuralHierarchyLookup hierarchy = new StructuralHierarchyLookup() {
                @Override
                public Optional<SabhaScope> sabhaScope(UUID sabhaId) {
                    return sabhaId.equals(SABHA_ID)
                            ? Optional.of(new SabhaScope(KSHETRA_ID, DEMOGRAPHIC, "REGULAR"))
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
            Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
            OccurrenceTransitionExecutor executor = new OccurrenceTransitionExecutor(
                    callerResolver, new AuthorizationEngine(roles, hierarchy),
                    occurrences, transitions, publisher, clock);
            return new OccurrenceReopenService(executor);
        }
    }

    static final class InMemoryOccurrenceRepository implements OccurrenceRepository {
        final Map<UUID, Occurrence> store = new HashMap<>();
        final List<Occurrence> saved = new ArrayList<>();

        void put(Occurrence occurrence) {
            store.put(occurrence.id(), occurrence);
        }

        @Override
        public Optional<Occurrence> findById(UUID occurrenceId) {
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
