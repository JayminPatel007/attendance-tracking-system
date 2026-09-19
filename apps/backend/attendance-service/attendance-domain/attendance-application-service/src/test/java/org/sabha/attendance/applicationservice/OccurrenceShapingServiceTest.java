package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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
import org.sabha.attendance.domain.OccurrenceState;
import org.sabha.attendance.domain.Reason;
import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.DomainEvent;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.Role;
import org.sabha.common.CallerAuthority;
import org.sabha.common.RoleAssignment;
import org.sabha.common.SabhaSchedule;
import org.sabha.common.SabhaScope;
import org.sabha.common.NirikshakAssignmentLookup;
import org.sabha.common.SabhaFact;
import org.sabha.common.SanchalakLookup;
import org.sabha.common.SabhaFacts;

import org.sabha.common.UserId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OccurrenceShapingServiceTest {

    private static final UUID OCCURRENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID SABHA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final LocalDate OCCURRENCE_DATE = LocalDate.of(2026, 5, 24);
    private static final UUID SANCHALAK_USER = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final CallerAuthority SANCHALAK_CALLER = new CallerAuthority(
            UserId.of(SANCHALAK_USER), List.of(new RoleAssignment("SANCHALAK", SABHA_ID, null, null, null, null)));
    private static final UUID SAH_SANCHALAK_USER = UUID.fromString("00000000-0000-0000-0000-000000000007");
    private static final CallerAuthority SAH_SANCHALAK_CALLER = new CallerAuthority(
            UserId.of(SAH_SANCHALAK_USER), List.of(new RoleAssignment("SAH_SANCHALAK", SABHA_ID, null, null, null, null)));
    private static final UUID NIRIKSHAK_USER = UUID.fromString("00000000-0000-0000-0000-000000000051");
    /** The proxy holds no row on the Sabha — its authority is the assignment. */
    private static final CallerAuthority NIRIKSHAK_CALLER =
            CallerAuthority.withNoRoles(UserId.of(NIRIKSHAK_USER));
    // The Sabha ends 20:00 on the Occurrence date; scheduled-end Instant is 2026-05-24T20:00:00Z.
    private static final Instant SCHEDULED_END = Instant.parse("2026-05-24T20:00:00Z");

    @Test
    void sanchalakCancelsAScheduledOccurrenceWithReasonAndAnAuditRowIsAppended() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.SCHEDULED);

        f.service().cancel(SANCHALAK_CALLER, OCCURRENCE_ID, new Reason("Festival clash"));

        assertThat(f.occurrences.saved).singleElement()
                .extracting(Occurrence::state).isEqualTo(OccurrenceState.CANCELLED);
        assertThat(f.transitions.appended).hasSize(1);
        OccurrenceStateTransition row = f.transitions.appended.get(0);
        assertThat(row.fromState()).isEqualTo(OccurrenceState.SCHEDULED);
        assertThat(row.toState()).isEqualTo(OccurrenceState.CANCELLED);
        assertThat(row.action()).isEqualTo(OccurrenceAction.CANCEL);
        assertThat(row.actorKind()).isEqualTo(ActorKind.USER);
        assertThat(row.actorUserId()).isEqualTo(SANCHALAK_USER);
        assertThat(row.reason()).isEqualTo("Festival clash");
        // The Sanchalak acting on their own Sabha is not a proxy action.
        assertThat(row.onBehalfOfUserId()).isNull();
    }

    @Test
    void anAssignedNirikshakProxyingACancelIsAuditedActingForTheAbsentSanchalak() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.SCHEDULED);

        f.service().cancel(NIRIKSHAK_CALLER, OCCURRENCE_ID, new Reason("Sanchalak unreachable"));

        assertThat(f.occurrences.saved).singleElement()
                .extracting(Occurrence::state).isEqualTo(OccurrenceState.CANCELLED);
        OccurrenceStateTransition row = f.transitions.appended.get(0);
        assertThat(row.action()).isEqualTo(OccurrenceAction.CANCEL);
        assertThat(row.actorUserId()).isEqualTo(NIRIKSHAK_USER);
        assertThat(row.onBehalfOfUserId()).isEqualTo(SANCHALAK_USER);
    }

    @Test
    void sahSanchalakCancelIsRejectedWithNoSideEffects() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.SCHEDULED);

        assertThatThrownBy(() -> f.service().cancel(SAH_SANCHALAK_CALLER, OCCURRENCE_ID, new Reason("trying")))
                .isInstanceOf(AuthorizationDeniedException.class);

        assertThat(f.occurrences.saved).isEmpty();
        assertThat(f.transitions.appended).isEmpty();
        assertThat(f.publisher.published).isEmpty();
    }

    @Test
    void sanchalakRevertsACancelledOccurrenceWithinTheGraceWindow() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.CANCELLED);
        f.now = SCHEDULED_END.plus(Duration.ofHours(23));

        f.service().revert(SANCHALAK_CALLER, OCCURRENCE_ID);

        assertThat(f.occurrences.saved).singleElement()
                .extracting(Occurrence::state).isEqualTo(OccurrenceState.SCHEDULED);
        OccurrenceStateTransition row = f.transitions.appended.get(0);
        assertThat(row.fromState()).isEqualTo(OccurrenceState.CANCELLED);
        assertThat(row.toState()).isEqualTo(OccurrenceState.SCHEDULED);
        assertThat(row.action()).isEqualTo(OccurrenceAction.REVERT);
    }

    @Test
    void revertAfterTheGraceWindowIsRejected() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.CANCELLED);
        f.now = SCHEDULED_END.plus(Duration.ofHours(25));

        assertThatThrownBy(() -> f.service().revert(SANCHALAK_CALLER, OCCURRENCE_ID))
                .isInstanceOf(RevertWindowExpiredException.class);

        assertThat(f.occurrences.saved).isEmpty();
        assertThat(f.transitions.appended).isEmpty();
    }

    @Test
    void revertOfAMonthlyAdHocOccurrenceIsBoundedByItsOwnEndTime() {
        // A monthly-ad-hoc Sabha has no standing schedule to fall back to; the
        // Occurrence's Effective Slot comes from its own picked end time, so the
        // revert window still closes a grace period after that.
        Fixture f = Fixture.withMonthlyAdHocOccurrence();
        f.now = SCHEDULED_END.plus(Duration.ofHours(25));

        assertThatThrownBy(() -> f.service().revert(SANCHALAK_CALLER, OCCURRENCE_ID))
                .isInstanceOf(RevertWindowExpiredException.class);

        assertThat(f.occurrences.saved).isEmpty();
    }

    @Test
    void revertOfAMonthlyAdHocOccurrenceIsAllowedWithinItsOwnGraceWindow() {
        Fixture f = Fixture.withMonthlyAdHocOccurrence();
        f.now = SCHEDULED_END.plus(Duration.ofHours(23));

        f.service().revert(SANCHALAK_CALLER, OCCURRENCE_ID);

        assertThat(f.occurrences.saved).singleElement()
                .extracting(Occurrence::state).isEqualTo(OccurrenceState.SCHEDULED);
    }

    @Test
    void sanchalakReschedulesAScheduledOccurrenceToANewDateTime() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.SCHEDULED);
        LocalDate newDate = LocalDate.of(2026, 5, 31);

        f.service().reschedule(SANCHALAK_CALLER, OCCURRENCE_ID,
                newDate, LocalTime.of(18, 0), LocalTime.of(19, 30));

        Occurrence saved = f.occurrences.saved.get(0);
        assertThat(saved.state()).isEqualTo(OccurrenceState.RESCHEDULED);
        assertThat(saved.rescheduledDate()).isEqualTo(newDate);
        assertThat(saved.rescheduledStartTime()).isEqualTo(LocalTime.of(18, 0));
        OccurrenceStateTransition row = f.transitions.appended.get(0);
        assertThat(row.action()).isEqualTo(OccurrenceAction.RESCHEDULE);
        assertThat(row.toState()).isEqualTo(OccurrenceState.RESCHEDULED);
    }

    @Test
    void sanchalakSetsAVenueOverrideOnAScheduledOccurrence() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.SCHEDULED);

        f.service().overrideVenue(SANCHALAK_CALLER, OCCURRENCE_ID, "Community Hall Annexe");

        Occurrence saved = f.occurrences.saved.get(0);
        assertThat(saved.venueOverride()).isEqualTo("Community Hall Annexe");
        assertThat(saved.state()).isEqualTo(OccurrenceState.SCHEDULED);
        OccurrenceStateTransition row = f.transitions.appended.get(0);
        assertThat(row.action()).isEqualTo(OccurrenceAction.OVERRIDE_VENUE);
        assertThat(row.fromState()).isEqualTo(OccurrenceState.SCHEDULED);
        assertThat(row.toState()).isEqualTo(OccurrenceState.SCHEDULED);
    }

    // --- fixture -----------------------------------------------------------

    static final class Fixture {
        final InMemoryOccurrenceRepository occurrences = new InMemoryOccurrenceRepository();
        final InMemoryTransitionLog transitions = new InMemoryTransitionLog();
        final CapturingPublisher publisher = new CapturingPublisher();
        Instant now = SCHEDULED_END;
        SabhaSchedule standingSchedule = new SabhaSchedule(
                java.time.DayOfWeek.SUNDAY, LocalTime.of(19, 0), LocalTime.of(20, 0));

        static Fixture withOccurrence(OccurrenceState state) {
            Fixture f = new Fixture();
            f.occurrences.put(new Occurrence(OCCURRENCE_ID, SABHA_ID, OCCURRENCE_DATE, state, 0L, List.of()));
            return f;
        }

        /** A Cancelled Occurrence on a monthly-ad-hoc Sabha: own 19:00–20:00 slot, no standing schedule. */
        static Fixture withMonthlyAdHocOccurrence() {
            Fixture f = new Fixture();
            f.standingSchedule = null;
            Occurrence occurrence = new Occurrence(OCCURRENCE_ID, SABHA_ID, OCCURRENCE_DATE,
                    OccurrenceState.CANCELLED, 0L, List.of());
            occurrence.restoreShaping(null, null, LocalTime.of(19, 0), LocalTime.of(20, 0));
            f.occurrences.put(occurrence);
            return f;
        }

        OccurrenceShapingService service() {
            // Three methods became one: the two caller-keyed reads are on the
            // caller now, and what is left is keyed by the target Sabha (ADR-0032).
            SanchalakLookup roles = sabhaId ->
                    sabhaId.equals(SABHA_ID) ? Optional.of(SANCHALAK_USER) : Optional.empty();
            // Shaping is Sanchalak-scoped, so the engine never asks for a Sabha's scope;
            // the standing slot is the only fact these paths read.
            SabhaFacts sabhaFacts = new SabhaFacts() {
                @Override
                public Optional<SabhaFact> of(UUID sabhaId) {
                return Optional.ofNullable(standingSchedule)
                        .map(slot -> SabhaFact.weekly(
                                sabhaId, new SabhaScope(null, null, null), slot, false));
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
            Clock clock = Clock.fixed(now, ZoneOffset.UTC);
            org.sabha.common.NirikshakAssignmentLookup nirikshakAssignments =
                    (userId, sabhaId) ->
                            userId.equals(NIRIKSHAK_USER) && sabhaId.equals(SABHA_ID);
            OccurrenceWriter writer = new OccurrenceWriter(
                    new AuthorizationEngine(roles, sabhaFacts, nirikshakAssignments),
                    occurrences, transitions, publisher, clock);
            return new OccurrenceShapingService(writer, new EffectiveSlotResolver(sabhaFacts, clock),
                    clock, Duration.ofHours(24));
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
