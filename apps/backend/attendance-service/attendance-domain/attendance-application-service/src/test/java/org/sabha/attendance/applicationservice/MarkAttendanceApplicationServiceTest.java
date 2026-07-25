package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.attendance.applicationservice.MarkAttendanceApplicationService.MarkItem;
import org.sabha.attendance.domain.AttendanceMarked;
import org.sabha.attendance.domain.AttendanceMarking;
import org.sabha.attendance.domain.MarkingType;
import org.sabha.attendance.domain.Occurrence;
import org.sabha.attendance.domain.OccurrenceState;
import org.sabha.common.CallerResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers what this service still owns after issue #128: the marking vocabulary
 * (Roster presence vs Walk-in) and the fact that marking appends no
 * state-transition row. The load/retry/save/publish contract it rides is
 * asserted once in {@link OccurrenceWriterTest}.
 */
class MarkAttendanceApplicationServiceTest {

    private static final UUID SUBJECT = UUID.fromString("00000000-0000-0000-0000-000000000300");
    private static final UUID MARKED_BY = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID OCCURRENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID SABHA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PERSON_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final Instant CLIENT_MARKED_AT = Instant.parse("2026-05-23T19:00:00Z");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-23T20:00:00Z"), ZoneOffset.UTC);

    @Test
    void markingARosterMemberRecordsThePresenceAttributedToTheCaller() {
        Fixture f = new Fixture();

        f.service().execute(SUBJECT, OCCURRENCE_ID, PERSON_ID, true, CLIENT_MARKED_AT);

        assertThat(f.occurrences.savedOccurrences()).hasSize(1);
        Occurrence saved = f.occurrences.savedOccurrences().get(0);
        assertThat(saved.id()).isEqualTo(OCCURRENCE_ID);
        AttendanceMarking marking = saved.markings().iterator().next();
        assertThat(marking.personId()).isEqualTo(PERSON_ID);
        assertThat(marking.present()).isTrue();
        assertThat(marking.markingType()).isEqualTo(MarkingType.ROSTER);
        assertThat(marking.markedByUserId()).isEqualTo(MARKED_BY);
        assertThat(f.publisher.published).singleElement().isInstanceOf(AttendanceMarked.class);
    }

    @Test
    void markingAWalkInRecordsAPresentWalkInMarkingAndPublishesIt() {
        Fixture f = new Fixture();

        f.service().executeBatch(SUBJECT, OCCURRENCE_ID,
                List.of(MarkItem.walkIn(PERSON_ID, CLIENT_MARKED_AT)));

        AttendanceMarking marking = f.occurrences.savedOccurrences().get(0).markings().iterator().next();
        assertThat(marking.personId()).isEqualTo(PERSON_ID);
        assertThat(marking.present()).isTrue();
        assertThat(marking.markingType()).isEqualTo(MarkingType.WALK_IN);
        assertThat(f.publisher.published).singleElement().isInstanceOf(AttendanceMarked.class);
        assertThat(((AttendanceMarked) f.publisher.published.get(0)).markingType())
                .isEqualTo(MarkingType.WALK_IN);
    }

    @Test
    void markingIsNotALifecycleTransitionSoNoAuditRowIsAppended() {
        Fixture f = new Fixture();

        f.service().execute(SUBJECT, OCCURRENCE_ID, PERSON_ID, true, CLIENT_MARKED_AT);

        assertThat(f.transitions.appended).isEmpty();
    }

    private static final class Fixture {
        final OccurrenceWriterTest.RecordingOccurrenceRepository occurrences =
                new OccurrenceWriterTest.RecordingOccurrenceRepository();
        final OccurrenceWriterTest.InMemoryTransitionLog transitions =
                new OccurrenceWriterTest.InMemoryTransitionLog();
        final OccurrenceWriterTest.CapturingPublisher publisher =
                new OccurrenceWriterTest.CapturingPublisher();

        Fixture() {
            occurrences.put(new Occurrence(OCCURRENCE_ID, SABHA_ID,
                    LocalDate.of(2026, 5, 23), OccurrenceState.OPEN_FOR_MARKING));
        }

        MarkAttendanceApplicationService service() {
            CallerResolver callerResolver =
                    subject -> subject.equals(SUBJECT) ? Optional.of(MARKED_BY) : Optional.empty();
            return new MarkAttendanceApplicationService(OccurrenceWriterTest.unauthorizedWriter(
                    callerResolver, occurrences, transitions, publisher, FIXED_CLOCK));
        }
    }
}
