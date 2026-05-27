package org.sabha.attendance.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.DomainEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OccurrenceTest {

    private static final UUID OCCURRENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID SABHA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void openingAScheduledOccurrenceTransitionsToOpenForMarkingAndRegistersAnEvent() {
        Occurrence occurrence = Occurrence.scheduled(OCCURRENCE_ID, SABHA_ID, LocalDate.of(2026, 5, 23));

        occurrence.open();

        assertThat(occurrence.state()).isEqualTo(OccurrenceState.OPEN_FOR_MARKING);
        List<DomainEvent> events = occurrence.pullDomainEvents();
        assertThat(events).singleElement().isInstanceOf(OccurrenceOpened.class);
        OccurrenceOpened event = (OccurrenceOpened) events.get(0);
        assertThat(event.aggregateId()).isEqualTo(OCCURRENCE_ID);
    }

    @Test
    void openingAFinalizedOccurrenceIsRejected() {
        Occurrence occurrence = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.FINALIZED);

        assertThatThrownBy(occurrence::open)
                .isInstanceOf(InvalidOccurrenceTransitionException.class);
        assertThat(occurrence.state()).isEqualTo(OccurrenceState.FINALIZED);
        assertThat(occurrence.pullDomainEvents()).isEmpty();
    }

    @Test
    void markingAPersonOnAnOpenOccurrenceRecordsTheMarkingAndRegistersAnEvent() {
        Occurrence occurrence = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.OPEN_FOR_MARKING);
        UUID personId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID markedBy = UUID.fromString("00000000-0000-0000-0000-000000000004");
        Instant clientMarkedAt = Instant.parse("2026-05-23T19:00:00Z");

        occurrence.mark(personId, true, markedBy, clientMarkedAt);

        assertThat(occurrence.markings()).hasSize(1);
        AttendanceMarking marking = occurrence.markings().iterator().next();
        assertThat(marking.personId()).isEqualTo(personId);
        assertThat(marking.present()).isTrue();
        assertThat(marking.markedByUserId()).isEqualTo(markedBy);

        List<DomainEvent> events = occurrence.pullDomainEvents();
        assertThat(events).singleElement().isInstanceOf(AttendanceMarked.class);
        AttendanceMarked event = (AttendanceMarked) events.get(0);
        assertThat(event.aggregateId()).isEqualTo(OCCURRENCE_ID);
        assertThat(event.personId()).isEqualTo(personId);
        assertThat(event.present()).isTrue();
        assertThat(event.markedBy()).isEqualTo(markedBy);
    }

    @Test
    void markingAPersonOnAFinalizedOccurrenceIsRejected() {
        Occurrence occurrence = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.FINALIZED);
        UUID personId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID markedBy = UUID.fromString("00000000-0000-0000-0000-000000000004");
        Instant clientMarkedAt = Instant.parse("2026-05-23T19:00:00Z");

        assertThatThrownBy(() -> occurrence.mark(personId, true, markedBy, clientMarkedAt))
                .isInstanceOf(OccurrenceNotOpenForMarkingException.class);
        assertThat(occurrence.markings()).isEmpty();
        assertThat(occurrence.pullDomainEvents()).isEmpty();
    }

    @Test
    void finalizingAnOpenOccurrenceTransitionsToFinalizedAndRegistersAnEvent() {
        Occurrence occurrence = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.OPEN_FOR_MARKING);

        occurrence.markFinalized();

        assertThat(occurrence.state()).isEqualTo(OccurrenceState.FINALIZED);
        List<DomainEvent> events = occurrence.pullDomainEvents();
        assertThat(events).singleElement().isInstanceOf(OccurrenceFinalized.class);
        OccurrenceFinalized event = (OccurrenceFinalized) events.get(0);
        assertThat(event.aggregateId()).isEqualTo(OCCURRENCE_ID);
    }

    @Test
    void finalizingAScheduledOccurrenceIsRejected() {
        Occurrence occurrence = Occurrence.scheduled(OCCURRENCE_ID, SABHA_ID, LocalDate.of(2026, 5, 23));

        assertThatThrownBy(occurrence::markFinalized)
                .isInstanceOf(InvalidOccurrenceTransitionException.class);
        assertThat(occurrence.state()).isEqualTo(OccurrenceState.SCHEDULED);
        assertThat(occurrence.pullDomainEvents()).isEmpty();
    }

    @Test
    void finalizingAnAlreadyFinalizedOccurrenceIsRejected() {
        Occurrence occurrence = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.FINALIZED);

        assertThatThrownBy(occurrence::markFinalized)
                .isInstanceOf(InvalidOccurrenceTransitionException.class);
        assertThat(occurrence.state()).isEqualTo(OccurrenceState.FINALIZED);
        assertThat(occurrence.pullDomainEvents()).isEmpty();
    }

    @Test
    void markingAPersonTwiceKeepsOnlyTheLatestValue() {
        Occurrence occurrence = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.OPEN_FOR_MARKING);
        UUID personId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID markedBy = UUID.fromString("00000000-0000-0000-0000-000000000004");

        occurrence.mark(personId, true, markedBy, Instant.parse("2026-05-23T19:00:00Z"));
        occurrence.mark(personId, false, markedBy, Instant.parse("2026-05-23T19:05:00Z"));

        assertThat(occurrence.markings()).hasSize(1);
        AttendanceMarking marking = occurrence.markings().iterator().next();
        assertThat(marking.personId()).isEqualTo(personId);
        assertThat(marking.present()).isFalse();
    }

    @Test
    void anIncomingMarkingWithAnOlderClientMarkedAtDoesNotOverwriteANewerOne() {
        Occurrence occurrence = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.OPEN_FOR_MARKING);
        UUID personId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID markedBy = UUID.fromString("00000000-0000-0000-0000-000000000004");

        Instant newer = Instant.parse("2026-05-23T19:05:00Z");
        Instant older = Instant.parse("2026-05-23T19:00:00Z");
        occurrence.mark(personId, true, markedBy, newer);
        occurrence.pullDomainEvents();

        occurrence.mark(personId, false, markedBy, older);

        assertThat(occurrence.markings()).hasSize(1);
        AttendanceMarking marking = occurrence.markings().iterator().next();
        assertThat(marking.present()).isTrue();
        assertThat(marking.clientMarkedAt()).isEqualTo(newer);
        assertThat(occurrence.pullDomainEvents()).isEmpty();
    }

    @Test
    void anIncomingMarkingWithAnEqualClientMarkedAtAppliesAsTieBreakerByArrival() {
        Occurrence occurrence = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.OPEN_FOR_MARKING);
        UUID personId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID markedBy = UUID.fromString("00000000-0000-0000-0000-000000000004");

        Instant tied = Instant.parse("2026-05-23T19:00:00Z");
        occurrence.mark(personId, true, markedBy, tied);
        occurrence.pullDomainEvents();

        occurrence.mark(personId, false, markedBy, tied);

        assertThat(occurrence.markings()).hasSize(1);
        assertThat(occurrence.markings().iterator().next().present()).isFalse();
    }

    @Test
    void markingExposesTheClientMarkedAtOnTheRecordedAttendance() {
        Occurrence occurrence = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.OPEN_FOR_MARKING);
        UUID personId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID markedBy = UUID.fromString("00000000-0000-0000-0000-000000000004");
        Instant clientMarkedAt = Instant.parse("2026-05-23T19:12:34Z");

        occurrence.mark(personId, true, markedBy, clientMarkedAt);

        AttendanceMarking marking = occurrence.markings().iterator().next();
        assertThat(marking.clientMarkedAt()).isEqualTo(clientMarkedAt);
    }
}
