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
    void cancellingAScheduledOccurrenceTransitionsToCancelled() {
        Occurrence occurrence = Occurrence.scheduled(OCCURRENCE_ID, SABHA_ID, LocalDate.of(2026, 5, 23));

        occurrence.cancel();

        assertThat(occurrence.state()).isEqualTo(OccurrenceState.CANCELLED);
    }

    @Test
    void cancellingAnOccurrenceThatIsNotScheduledIsRejected() {
        Occurrence occurrence = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.OPEN_FOR_MARKING);

        assertThatThrownBy(occurrence::cancel)
                .isInstanceOf(InvalidOccurrenceTransitionException.class);
        assertThat(occurrence.state()).isEqualTo(OccurrenceState.OPEN_FOR_MARKING);
    }

    @Test
    void revertingACancelledOccurrenceTransitionsBackToScheduled() {
        Occurrence occurrence = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.CANCELLED);

        occurrence.revert();

        assertThat(occurrence.state()).isEqualTo(OccurrenceState.SCHEDULED);
    }

    @Test
    void revertingAnOccurrenceThatIsNotCancelledIsRejected() {
        Occurrence occurrence = Occurrence.scheduled(OCCURRENCE_ID, SABHA_ID, LocalDate.of(2026, 5, 23));

        assertThatThrownBy(occurrence::revert)
                .isInstanceOf(InvalidOccurrenceTransitionException.class);
        assertThat(occurrence.state()).isEqualTo(OccurrenceState.SCHEDULED);
    }

    @Test
    void reschedulingAScheduledOccurrenceStoresTheOverrideAndTransitionsToRescheduled() {
        Occurrence occurrence = Occurrence.scheduled(OCCURRENCE_ID, SABHA_ID, LocalDate.of(2026, 5, 23));
        LocalDate newDate = LocalDate.of(2026, 5, 30);

        occurrence.reschedule(newDate, java.time.LocalTime.of(18, 0), java.time.LocalTime.of(19, 30));

        assertThat(occurrence.state()).isEqualTo(OccurrenceState.RESCHEDULED);
        assertThat(occurrence.rescheduledDate()).isEqualTo(newDate);
        assertThat(occurrence.rescheduledStartTime()).isEqualTo(java.time.LocalTime.of(18, 0));
        assertThat(occurrence.rescheduledEndTime()).isEqualTo(java.time.LocalTime.of(19, 30));
    }

    @Test
    void reschedulingAnOccurrenceThatIsNotScheduledIsRejected() {
        Occurrence occurrence = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.FINALIZED);

        assertThatThrownBy(() -> occurrence.reschedule(
                LocalDate.of(2026, 5, 30), java.time.LocalTime.of(18, 0), java.time.LocalTime.of(19, 30)))
                .isInstanceOf(InvalidOccurrenceTransitionException.class);
        assertThat(occurrence.state()).isEqualTo(OccurrenceState.FINALIZED);
    }

    @Test
    void openingARescheduledOccurrenceTransitionsToOpenForMarking() {
        Occurrence occurrence = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.RESCHEDULED);

        occurrence.open();

        assertThat(occurrence.state()).isEqualTo(OccurrenceState.OPEN_FOR_MARKING);
    }

    @Test
    void overridingVenueOnAScheduledOccurrenceStoresItWithoutChangingState() {
        Occurrence occurrence = Occurrence.scheduled(OCCURRENCE_ID, SABHA_ID, LocalDate.of(2026, 5, 23));

        occurrence.overrideVenue("Community Hall Annexe");

        assertThat(occurrence.venueOverride()).isEqualTo("Community Hall Annexe");
        assertThat(occurrence.state()).isEqualTo(OccurrenceState.SCHEDULED);
    }

    @Test
    void overridingVenueOnAnOccurrenceOpenForMarkingIsRejected() {
        Occurrence occurrence = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.OPEN_FOR_MARKING);

        assertThatThrownBy(() -> occurrence.overrideVenue("Community Hall Annexe"))
                .isInstanceOf(VenueOverrideNotAllowedException.class);
        assertThat(occurrence.venueOverride()).isNull();
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
    void markingAPersonRegistersThatMarkingAsPending() {
        Occurrence occurrence = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.OPEN_FOR_MARKING);
        UUID personId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID markedBy = UUID.fromString("00000000-0000-0000-0000-000000000004");

        occurrence.mark(personId, true, markedBy, Instant.parse("2026-05-23T19:00:00Z"));

        List<AttendanceMarking> pending = occurrence.pullPendingMarkings();
        assertThat(pending).singleElement()
                .satisfies(m -> {
                    assertThat(m.personId()).isEqualTo(personId);
                    assertThat(m.present()).isTrue();
                });
    }

    @Test
    void pullingPendingMarkingsDrainsThemSoTheSecondPullReturnsEmpty() {
        Occurrence occurrence = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.OPEN_FOR_MARKING);
        UUID personId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID markedBy = UUID.fromString("00000000-0000-0000-0000-000000000004");

        occurrence.mark(personId, true, markedBy, Instant.parse("2026-05-23T19:00:00Z"));

        assertThat(occurrence.pullPendingMarkings()).hasSize(1);
        assertThat(occurrence.pullPendingMarkings()).isEmpty();
    }

    @Test
    void rehydratedMarkingsAreNotPending() {
        UUID personA = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID personB = UUID.fromString("00000000-0000-0000-0000-000000000102");
        UUID markedBy = UUID.fromString("00000000-0000-0000-0000-000000000004");
        AttendanceMarking existingA = new AttendanceMarking(UUID.randomUUID(), OCCURRENCE_ID,
                personA, true, markedBy, Instant.parse("2026-05-23T19:00:00Z"));
        AttendanceMarking existingB = new AttendanceMarking(UUID.randomUUID(), OCCURRENCE_ID,
                personB, false, markedBy, Instant.parse("2026-05-23T19:01:00Z"));

        Occurrence occurrence = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.OPEN_FOR_MARKING,
                0L, java.util.List.of(existingA, existingB));

        assertThat(occurrence.markings()).hasSize(2);
        assertThat(occurrence.pullPendingMarkings()).isEmpty();
    }

    @Test
    void anLwwSkippedMarkingDoesNotRegisterAsPending() {
        Occurrence occurrence = new Occurrence(OCCURRENCE_ID, SABHA_ID,
                LocalDate.of(2026, 5, 23), OccurrenceState.OPEN_FOR_MARKING);
        UUID personId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID markedBy = UUID.fromString("00000000-0000-0000-0000-000000000004");

        Instant newer = Instant.parse("2026-05-23T19:05:00Z");
        Instant older = Instant.parse("2026-05-23T19:00:00Z");
        occurrence.mark(personId, true, markedBy, newer);
        occurrence.pullPendingMarkings();

        occurrence.mark(personId, false, markedBy, older);

        assertThat(occurrence.pullPendingMarkings()).isEmpty();
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
