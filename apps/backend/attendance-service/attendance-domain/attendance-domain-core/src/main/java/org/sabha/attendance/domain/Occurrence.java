package org.sabha.attendance.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sabha.common.AggregateRoot;

public class Occurrence extends AggregateRoot<UUID> {

    private final UUID id;
    private final UUID sabhaId;
    private final LocalDate date;
    private OccurrenceState state;
    private String venueOverride;
    private LocalDate rescheduledDate;
    private LocalTime rescheduledStartTime;
    private LocalTime rescheduledEndTime;
    private final Map<UUID, AttendanceMarking> markings = new LinkedHashMap<>();
    private final List<AttendanceMarking> pendingMarkings = new ArrayList<>();

    public Occurrence(UUID id, UUID sabhaId, LocalDate date, OccurrenceState state) {
        this.id = id;
        this.sabhaId = sabhaId;
        this.date = date;
        this.state = state;
    }

    /**
     * Rehydration constructor used by data-access adapters. Restores the
     * aggregate from persisted state without registering any domain events.
     */
    public Occurrence(UUID id, UUID sabhaId, LocalDate date, OccurrenceState state,
                      Long version, Collection<AttendanceMarking> existingMarkings) {
        this(id, sabhaId, date, state);
        this.version = version;
        for (AttendanceMarking m : existingMarkings) {
            this.markings.put(m.personId(), m);
        }
    }

    public static Occurrence scheduled(UUID id, UUID sabhaId, LocalDate date) {
        return new Occurrence(id, sabhaId, date, OccurrenceState.SCHEDULED);
    }

    /**
     * Creates a Scheduled Occurrence carrying its own date, time, and venue
     * (ADR-0012). Used for a monthly-ad-hoc Sabha's manually-created Occurrence,
     * which has no standing schedule to fall back to — the picked start/end time
     * and venue are held in the per-Occurrence override fields the cron scanners
     * already prefer.
     */
    public static Occurrence scheduledAt(UUID id, UUID sabhaId, LocalDate date,
                                         LocalTime startTime, LocalTime endTime, String venue) {
        Occurrence occurrence = new Occurrence(id, sabhaId, date, OccurrenceState.SCHEDULED);
        occurrence.rescheduledStartTime = startTime;
        occurrence.rescheduledEndTime = endTime;
        occurrence.venueOverride = venue;
        return occurrence;
    }

    public void open() {
        if (state != OccurrenceState.SCHEDULED && state != OccurrenceState.RESCHEDULED) {
            throw new InvalidOccurrenceTransitionException(id, state, OccurrenceState.OPEN_FOR_MARKING);
        }
        state = OccurrenceState.OPEN_FOR_MARKING;
        registerEvent(new OccurrenceOpened(id, Instant.now()));
    }

    /**
     * Sanchalak-only Sabha-shaping: cancel a Scheduled Occurrence (ADR-0001).
     * Reversible via {@link #revert()}; the cancellation reason is recorded on
     * the audit transition by the application service, not on the aggregate.
     */
    public void cancel() {
        if (state != OccurrenceState.SCHEDULED) {
            throw new InvalidOccurrenceTransitionException(id, state, OccurrenceState.CANCELLED);
        }
        state = OccurrenceState.CANCELLED;
    }

    /**
     * Reverts a Cancelled Occurrence back to Scheduled (ADR-0001). The 24h grace
     * window is enforced by the application service, which has the Clock and the
     * Sabha schedule needed to compute the scheduled-end cutoff.
     */
    public void revert() {
        if (state != OccurrenceState.CANCELLED) {
            throw new InvalidOccurrenceTransitionException(id, state, OccurrenceState.SCHEDULED);
        }
        state = OccurrenceState.SCHEDULED;
    }

    /**
     * Sanchalak-only Sabha-shaping: move this Occurrence to a new date/time
     * without touching the Sabha's standing schedule (ADR-0001). The override
     * date/time drive the cron scanners' open/finalize timing.
     */
    public void reschedule(LocalDate newDate, LocalTime newStartTime, LocalTime newEndTime) {
        if (state != OccurrenceState.SCHEDULED) {
            throw new InvalidOccurrenceTransitionException(id, state, OccurrenceState.RESCHEDULED);
        }
        this.rescheduledDate = newDate;
        this.rescheduledStartTime = newStartTime;
        this.rescheduledEndTime = newEndTime;
        state = OccurrenceState.RESCHEDULED;
    }

    /**
     * Sanchalak-only Sabha-shaping: set a one-off venue for this Occurrence
     * without changing the Sabha's standing venue (ADR-0001). Allowed up until
     * the Occurrence enters Open for Marking.
     */
    public void overrideVenue(String venue) {
        if (state != OccurrenceState.SCHEDULED && state != OccurrenceState.RESCHEDULED) {
            throw new VenueOverrideNotAllowedException(id, state);
        }
        this.venueOverride = venue;
    }

    /**
     * Reopens a Finalized Occurrence back to Open for Marking (ADR-0001). The
     * grace window and the Kshetra-tier authority (Nirikshak / Nirdeshak /
     * Sah-Nirdeshak — never Sanyojak, Sant, or MK) are enforced by the
     * application service; the reopener and reason are recorded on the audit
     * transition, and the "reopened" badge is derived from that transition log.
     */
    public void reopen() {
        if (state != OccurrenceState.FINALIZED) {
            throw new InvalidOccurrenceTransitionException(id, state, OccurrenceState.OPEN_FOR_MARKING);
        }
        state = OccurrenceState.OPEN_FOR_MARKING;
        registerEvent(new OccurrenceReopened(id, Instant.now()));
    }

    public void markFinalized() {
        if (state != OccurrenceState.OPEN_FOR_MARKING) {
            throw new InvalidOccurrenceTransitionException(id, state, OccurrenceState.FINALIZED);
        }
        state = OccurrenceState.FINALIZED;
        registerEvent(new OccurrenceFinalized(id, Instant.now()));
    }

    public void mark(UUID personId, boolean present, UUID markedBy, Instant clientMarkedAt) {
        record(personId, present, markedBy, clientMarkedAt, MarkingType.ROSTER);
    }

    /**
     * Records a Walk-in: a Person attending this Occurrence who is not on its
     * Roster (ADR-0007). A Walk-in is always present — the Sanchalak only
     * registers someone who showed up — and never touches the Person's Home
     * Sabha. Shares the Open-for-marking guard and last-write-wins semantics of
     * {@link #mark}.
     */
    public void markWalkIn(UUID personId, UUID markedBy, Instant clientMarkedAt) {
        record(personId, true, markedBy, clientMarkedAt, MarkingType.WALK_IN);
    }

    private void record(UUID personId, boolean present, UUID markedBy,
                        Instant clientMarkedAt, MarkingType markingType) {
        if (state != OccurrenceState.OPEN_FOR_MARKING) {
            throw new OccurrenceNotOpenForMarkingException(id, state);
        }
        AttendanceMarking existing = markings.get(personId);
        if (existing != null && existing.clientMarkedAt().isAfter(clientMarkedAt)) {
            return;
        }
        AttendanceMarking marking = new AttendanceMarking(
                UUID.randomUUID(), id, personId, present, markedBy, clientMarkedAt, markingType);
        markings.put(personId, marking);
        pendingMarkings.add(marking);
        registerEvent(new AttendanceMarked(id, personId, present, markedBy, markingType, Instant.now()));
    }

    /**
     * Drains the markings that mutated since the last pull. Repositories iterate
     * this set (not {@link #markings()}) so a save only writes rows that
     * actually changed — keeping the per-save cost O(pending), independent of
     * roster size. Mirrors the {@code pullDomainEvents} contract on
     * {@link org.sabha.common.AggregateRoot}.
     */
    public List<AttendanceMarking> pullPendingMarkings() {
        List<AttendanceMarking> drained = List.copyOf(pendingMarkings);
        pendingMarkings.clear();
        return drained;
    }

    public Collection<AttendanceMarking> markings() {
        return Collections.unmodifiableCollection(markings.values());
    }

    public boolean isOpenForMarking() {
        return state == OccurrenceState.OPEN_FOR_MARKING;
    }

    @Override
    public UUID id() {
        return id;
    }

    public UUID sabhaId() {
        return sabhaId;
    }

    public LocalDate date() {
        return date;
    }

    public OccurrenceState state() {
        return state;
    }

    public String venueOverride() {
        return venueOverride;
    }

    public LocalDate rescheduledDate() {
        return rescheduledDate;
    }

    public LocalTime rescheduledStartTime() {
        return rescheduledStartTime;
    }

    public LocalTime rescheduledEndTime() {
        return rescheduledEndTime;
    }

    /** Restores persisted shaping overrides during rehydration. */
    public void restoreShaping(String venueOverride, LocalDate rescheduledDate,
                               LocalTime rescheduledStartTime, LocalTime rescheduledEndTime) {
        this.venueOverride = venueOverride;
        this.rescheduledDate = rescheduledDate;
        this.rescheduledStartTime = rescheduledStartTime;
        this.rescheduledEndTime = rescheduledEndTime;
    }
}
