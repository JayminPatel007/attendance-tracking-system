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
import org.sabha.attendance.domain.Reason;
import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.DomainEvent;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.Role;
import org.sabha.common.CallerAuthority;
import org.sabha.common.RoleAssignment;
import org.sabha.common.SabhaScope;
import org.sabha.common.NirikshakAssignmentLookup;
import org.sabha.common.SabhaFact;
import org.sabha.common.SanchalakLookup;
import org.sabha.common.SabhaFacts;

import org.sabha.common.UserId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OccurrenceReopenServiceTest {

    private static final UUID OCCURRENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID SABHA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID KSHETRA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String DEMOGRAPHIC = "YUVAK";
    private static final LocalDate OCCURRENCE_DATE = LocalDate.of(2026, 5, 24);
    private static final UUID NIRIKSHAK_USER = UUID.fromString("00000000-0000-0000-0000-000000000031");
    private static final CallerAuthority NIRIKSHAK_CALLER = new CallerAuthority(
            UserId.of(NIRIKSHAK_USER), List.of(new RoleAssignment("NIRIKSHAK", null, KSHETRA_ID, null, null, DEMOGRAPHIC)));
    private static final UUID SANCHALAK_USER = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final CallerAuthority SANCHALAK_CALLER = new CallerAuthority(
            UserId.of(SANCHALAK_USER), List.of(new RoleAssignment("SANCHALAK", SABHA_ID, null, null, null, null)));
    private static final Instant NOW = Instant.parse("2026-05-26T08:00:00Z");

    @Test
    void aNirikshakReopensAFinalizedOccurrenceWithReasonAndAnAuditRowIsAppended() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.FINALIZED);

        f.service().reopen(NIRIKSHAK_CALLER, OCCURRENCE_ID, new Reason("Forgot to mark Ravi"));

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
    void aSanchalakReopenIsRejectedWithNoSideEffects() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.FINALIZED);

        assertThatThrownBy(() -> f.service().reopen(SANCHALAK_CALLER, OCCURRENCE_ID, new Reason("let me in")))
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
            // Three methods became one: the two caller-keyed reads are on the
            // caller now, and what is left is keyed by the target Sabha (ADR-0032).
            SanchalakLookup roles = sabhaId ->
                    sabhaId.equals(SABHA_ID) ? Optional.of(SANCHALAK_USER) : Optional.empty();
            SabhaFacts sabhaFacts = new SabhaFacts() {
                @Override
                public Optional<SabhaFact> of(UUID sabhaId) {
                return sabhaId.equals(SABHA_ID)
                        ? Optional.of(SabhaFact.monthlyAdHoc(
                                sabhaId, new SabhaScope(KSHETRA_ID, DEMOGRAPHIC, "REGULAR"), false))
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
            Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
            // No Nirikshak proxy assignments — these paths are the Kshetra-tier reopen.
            NirikshakAssignmentLookup noProxy = (userId, sabhaId) -> false;
            OccurrenceWriter writer = new OccurrenceWriter(
                    new AuthorizationEngine(roles, sabhaFacts, noProxy),
                    occurrences, transitions, publisher, clock);
            return new OccurrenceReopenService(writer);
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
