package org.sabha.attendance.applicationservice;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.attendance.domain.Occurrence;
import org.sabha.attendance.domain.OccurrenceState;
import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.Role;
import org.sabha.common.RoleAssignmentLookup;
import org.sabha.common.SabhaFact;
import org.sabha.common.SabhaFacts;
import org.sabha.common.SabhaSchedule;
import org.sabha.common.SabhaScope;

import org.sabha.common.UserId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateMonthlyOccurrenceServiceTest {

    private static final UUID SANCHALAK = UUID.fromString("00000000-0000-0000-0000-0000000000d0");
    private static final UserId CALLER = UserId.of(SANCHALAK);
    private static final UUID MONTHLY_SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID WEEKLY_SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000a2");

    private final RecordingInsert occurrences = new RecordingInsert();
    private final FakeSabhaFacts sabhaFacts = new FakeSabhaFacts();
    private final CreateMonthlyOccurrenceApplicationService service = new CreateMonthlyOccurrenceApplicationService(
            new AuthorizationEngine(new FakeRoles(), sabhaFacts, new NoNirikshakAssignments()),
            sabhaFacts,
            occurrences);

    @Test
    void sanchalakCreatesThisMonthsOccurrenceInScheduledState() {
        UUID id = service.create(CALLER, MONTHLY_SABHA,
                LocalDate.of(2026, 6, 21), LocalTime.of(9, 0), LocalTime.of(10, 30), "Andheri Hall");

        Occurrence created = occurrences.added.get(0);
        assertThat(created.id()).isEqualTo(id);
        assertThat(created.sabhaId()).isEqualTo(MONTHLY_SABHA);
        assertThat(created.date()).isEqualTo(LocalDate.of(2026, 6, 21));
        assertThat(created.state()).isEqualTo(OccurrenceState.SCHEDULED);
        assertThat(created.rescheduledStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(created.rescheduledEndTime()).isEqualTo(LocalTime.of(10, 30));
        assertThat(created.venueOverride()).isEqualTo("Andheri Hall");
    }

    @Test
    void aNonSanchalakIsDeniedAndNoOccurrenceIsCreated() {
        UserId outsider = UserId.of(UUID.fromString("00000000-0000-0000-0000-0000000000d9"));

        assertThatThrownBy(() -> service.create(outsider, MONTHLY_SABHA,
                LocalDate.of(2026, 6, 21), LocalTime.of(9, 0), LocalTime.of(10, 30), "Andheri Hall"))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(occurrences.added).isEmpty();
    }

    @Test
    void creatingAnOccurrenceOnAWeeklySabhaIsRejected() {
        assertThatThrownBy(() -> service.create(CALLER, WEEKLY_SABHA,
                LocalDate.of(2026, 6, 21), LocalTime.of(9, 0), LocalTime.of(10, 30), "Andheri Hall"))
                .isInstanceOf(NotMonthlyAdHocException.class);
        assertThat(occurrences.added).isEmpty();
    }

    private static final class FakeRoles implements RoleAssignmentLookup {
        @Override
        public Set<Role> rolesForUserOnSabha(UUID userId, UUID sabhaId) {
            return userId.equals(SANCHALAK) && sabhaId.equals(MONTHLY_SABHA) ? Set.of(Role.SANCHALAK) : Set.of();
        }

        @Override
        public Set<Role> rolesForUserOnKshetra(UUID userId, UUID kshetraId, String demographic) {
            return Set.of();
        }
    }

    /** No Nirikshak proxy assignments — these tests exercise the Sanchalak path only. */
    private static final class NoNirikshakAssignments implements org.sabha.common.NirikshakAssignmentLookup {
        @Override
        public boolean isAssignedTo(UUID userId, UUID sabhaId) {
            return false;
        }

        @Override
        public Set<UUID> sabhasAssignedTo(UUID userId) {
            return Set.of();
        }
    }

    /**
     * One fake now serves both the shape guard and the authorization engine — the
     * two used separate ports over the same row before ADR-0033. Monthly-occurrence
     * creation is a shaping action (Sanchalak), so the scope is never read.
     */
    private static final class FakeSabhaFacts implements SabhaFacts {
        private static final SabhaScope ANY_SCOPE = new SabhaScope(null, null, null);
        private static final SabhaSchedule ANY_SLOT = new SabhaSchedule(
                java.time.DayOfWeek.SUNDAY, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 30));

        @Override
        public Optional<SabhaFact> of(UUID sabhaId) {
            if (sabhaId.equals(MONTHLY_SABHA)) {
                return Optional.of(SabhaFact.monthlyAdHoc(sabhaId, ANY_SCOPE, false));
            }
            if (sabhaId.equals(WEEKLY_SABHA)) {
                return Optional.of(SabhaFact.weekly(sabhaId, ANY_SCOPE, ANY_SLOT, false));
            }
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
    }

    private static final class RecordingInsert implements OccurrenceInsert {
        final List<Occurrence> added = new ArrayList<>();

        @Override
        public void add(Occurrence occurrence) {
            added.add(occurrence);
        }
    }
}
