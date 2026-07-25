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
import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.CallerResolver;
import org.sabha.common.DomainEvent;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.Role;
import org.sabha.common.RoleAssignmentLookup;
import org.sabha.common.SabhaSchedule;
import org.sabha.common.SabhaScheduleLookup;
import org.sabha.common.SabhaScope;
import org.sabha.common.StructuralHierarchyLookup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OccurrenceShapingServiceTest {

    private static final UUID OCCURRENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID SABHA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final LocalDate OCCURRENCE_DATE = LocalDate.of(2026, 5, 24);
    private static final UUID SANCHALAK_SUBJECT = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID SANCHALAK_USER = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID SAH_SANCHALAK_SUBJECT = UUID.fromString("00000000-0000-0000-0000-000000000008");
    private static final UUID SAH_SANCHALAK_USER = UUID.fromString("00000000-0000-0000-0000-000000000007");
    private static final UUID NIRIKSHAK_SUBJECT = UUID.fromString("00000000-0000-0000-0000-000000000052");
    private static final UUID NIRIKSHAK_USER = UUID.fromString("00000000-0000-0000-0000-000000000051");
    // The Sabha ends 20:00 on the Occurrence date; scheduled-end Instant is 2026-05-24T20:00:00Z.
    private static final Instant SCHEDULED_END = Instant.parse("2026-05-24T20:00:00Z");

    @Test
    void sanchalakCancelsAScheduledOccurrenceWithReasonAndAnAuditRowIsAppended() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.SCHEDULED);

        f.service().cancel(SANCHALAK_SUBJECT, OCCURRENCE_ID, "Festival clash");

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

        f.service().cancel(NIRIKSHAK_SUBJECT, OCCURRENCE_ID, "Sanchalak unreachable");

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

        assertThatThrownBy(() -> f.service().cancel(SAH_SANCHALAK_SUBJECT, OCCURRENCE_ID, "trying"))
                .isInstanceOf(AuthorizationDeniedException.class);

        assertThat(f.occurrences.saved).isEmpty();
        assertThat(f.transitions.appended).isEmpty();
        assertThat(f.publisher.published).isEmpty();
    }

    @Test
    void cancelWithoutAReasonIsRejected() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.SCHEDULED);

        assertThatThrownBy(() -> f.service().cancel(SANCHALAK_SUBJECT, OCCURRENCE_ID, "  "))
                .isInstanceOf(CancellationReasonRequiredException.class);

        assertThat(f.occurrences.saved).isEmpty();
        assertThat(f.transitions.appended).isEmpty();
    }

    @Test
    void sanchalakRevertsACancelledOccurrenceWithinTheGraceWindow() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.CANCELLED);
        f.now = SCHEDULED_END.plus(Duration.ofHours(23));

        f.service().revert(SANCHALAK_SUBJECT, OCCURRENCE_ID);

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

        assertThatThrownBy(() -> f.service().revert(SANCHALAK_SUBJECT, OCCURRENCE_ID))
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

        assertThatThrownBy(() -> f.service().revert(SANCHALAK_SUBJECT, OCCURRENCE_ID))
                .isInstanceOf(RevertWindowExpiredException.class);

        assertThat(f.occurrences.saved).isEmpty();
    }

    @Test
    void revertOfAMonthlyAdHocOccurrenceIsAllowedWithinItsOwnGraceWindow() {
        Fixture f = Fixture.withMonthlyAdHocOccurrence();
        f.now = SCHEDULED_END.plus(Duration.ofHours(23));

        f.service().revert(SANCHALAK_SUBJECT, OCCURRENCE_ID);

        assertThat(f.occurrences.saved).singleElement()
                .extracting(Occurrence::state).isEqualTo(OccurrenceState.SCHEDULED);
    }

    @Test
    void sanchalakReschedulesAScheduledOccurrenceToANewDateTime() {
        Fixture f = Fixture.withOccurrence(OccurrenceState.SCHEDULED);
        LocalDate newDate = LocalDate.of(2026, 5, 31);

        f.service().reschedule(SANCHALAK_SUBJECT, OCCURRENCE_ID,
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

        f.service().overrideVenue(SANCHALAK_SUBJECT, OCCURRENCE_ID, "Community Hall Annexe");

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
            CallerResolver callerResolver = subject -> {
                if (subject.equals(SANCHALAK_SUBJECT)) {
                    return Optional.of(SANCHALAK_USER);
                }
                if (subject.equals(SAH_SANCHALAK_SUBJECT)) {
                    return Optional.of(SAH_SANCHALAK_USER);
                }
                if (subject.equals(NIRIKSHAK_SUBJECT)) {
                    return Optional.of(NIRIKSHAK_USER);
                }
                return Optional.empty();
            };
            RoleAssignmentLookup roles = new RoleAssignmentLookup() {
                @Override
                public Set<Role> rolesForUserOnSabha(UUID userId, UUID sabhaId) {
                    if (userId.equals(SANCHALAK_USER) && sabhaId.equals(SABHA_ID)) {
                        return Set.of(Role.SANCHALAK);
                    }
                    if (userId.equals(SAH_SANCHALAK_USER) && sabhaId.equals(SABHA_ID)) {
                        return Set.of(Role.SAH_SANCHALAK);
                    }
                    return Set.of();
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
            // Shaping is Sanchalak-scoped; the engine never consults the hierarchy for these actions.
            StructuralHierarchyLookup hierarchy = new StructuralHierarchyLookup() {
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
            SabhaScheduleLookup schedule = sabhaId -> Optional.ofNullable(standingSchedule);
            Clock clock = Clock.fixed(now, ZoneOffset.UTC);
            org.sabha.common.NirikshakAssignmentLookup nirikshakAssignments =
                    new org.sabha.common.NirikshakAssignmentLookup() {
                        @Override
                        public boolean isAssignedTo(UUID userId, UUID sabhaId) {
                            return userId.equals(NIRIKSHAK_USER) && sabhaId.equals(SABHA_ID);
                        }

                        @Override
                        public Set<UUID> sabhasAssignedTo(UUID userId) {
                            return userId.equals(NIRIKSHAK_USER) ? Set.of(SABHA_ID) : Set.of();
                        }
                    };
            OccurrenceWriter writer = new OccurrenceWriter(
                    callerResolver, new AuthorizationEngine(roles, hierarchy, nirikshakAssignments),
                    occurrences, transitions, publisher, clock);
            return new OccurrenceShapingService(writer, new EffectiveSlotResolver(schedule, clock),
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
