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
import org.sabha.common.SabhaShapeLookup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateMonthlyOccurrenceServiceTest {

    private static final UUID SUBJECT = UUID.fromString("00000000-0000-0000-0000-0000000000f0");
    private static final UUID SANCHALAK = UUID.fromString("00000000-0000-0000-0000-0000000000d0");
    private static final UUID MONTHLY_SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID WEEKLY_SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000a2");

    private final RecordingInsert occurrences = new RecordingInsert();
    private final CreateMonthlyOccurrenceApplicationService service = new CreateMonthlyOccurrenceApplicationService(
            subject -> subject.equals(SUBJECT) ? Optional.of(SANCHALAK) : Optional.empty(),
            new AuthorizationEngine(new FakeRoles()),
            new FakeShapes(),
            occurrences);

    @Test
    void sanchalakCreatesThisMonthsOccurrenceInScheduledState() {
        UUID id = service.create(SUBJECT, MONTHLY_SABHA,
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
        UUID otherSubject = UUID.fromString("00000000-0000-0000-0000-0000000000f9");
        CreateMonthlyOccurrenceApplicationService denying = new CreateMonthlyOccurrenceApplicationService(
                subject -> Optional.of(UUID.fromString("00000000-0000-0000-0000-0000000000d9")),
                new AuthorizationEngine(new FakeRoles()),
                new FakeShapes(),
                occurrences);

        assertThatThrownBy(() -> denying.create(otherSubject, MONTHLY_SABHA,
                LocalDate.of(2026, 6, 21), LocalTime.of(9, 0), LocalTime.of(10, 30), "Andheri Hall"))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(occurrences.added).isEmpty();
    }

    @Test
    void creatingAnOccurrenceOnAWeeklySabhaIsRejected() {
        assertThatThrownBy(() -> service.create(SUBJECT, WEEKLY_SABHA,
                LocalDate.of(2026, 6, 21), LocalTime.of(9, 0), LocalTime.of(10, 30), "Andheri Hall"))
                .isInstanceOf(NotMonthlyAdHocException.class);
        assertThat(occurrences.added).isEmpty();
    }

    private static final class FakeRoles implements RoleAssignmentLookup {
        @Override
        public Set<Role> rolesForUserOnSabha(UUID userId, UUID sabhaId) {
            return userId.equals(SANCHALAK) && sabhaId.equals(MONTHLY_SABHA) ? Set.of(Role.SANCHALAK) : Set.of();
        }
    }

    private static final class FakeShapes implements SabhaShapeLookup {
        @Override
        public Optional<String> scheduleShapeOf(UUID sabhaId) {
            if (sabhaId.equals(MONTHLY_SABHA)) {
                return Optional.of("MONTHLY_AD_HOC");
            }
            if (sabhaId.equals(WEEKLY_SABHA)) {
                return Optional.of("WEEKLY_RECURRING");
            }
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
