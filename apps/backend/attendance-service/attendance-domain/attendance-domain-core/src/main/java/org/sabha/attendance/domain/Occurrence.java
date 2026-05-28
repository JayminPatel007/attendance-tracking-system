package org.sabha.attendance.domain;

import java.time.Instant;
import java.time.LocalDate;
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

    public void open() {
        if (state != OccurrenceState.SCHEDULED) {
            throw new InvalidOccurrenceTransitionException(id, state, OccurrenceState.OPEN_FOR_MARKING);
        }
        state = OccurrenceState.OPEN_FOR_MARKING;
        registerEvent(new OccurrenceOpened(id, Instant.now()));
    }

    public void markFinalized() {
        if (state != OccurrenceState.OPEN_FOR_MARKING) {
            throw new InvalidOccurrenceTransitionException(id, state, OccurrenceState.FINALIZED);
        }
        state = OccurrenceState.FINALIZED;
        registerEvent(new OccurrenceFinalized(id, Instant.now()));
    }

    public void mark(UUID personId, boolean present, UUID markedBy, Instant clientMarkedAt) {
        if (state != OccurrenceState.OPEN_FOR_MARKING) {
            throw new OccurrenceNotOpenForMarkingException(id, state);
        }
        AttendanceMarking existing = markings.get(personId);
        if (existing != null && existing.clientMarkedAt().isAfter(clientMarkedAt)) {
            return;
        }
        AttendanceMarking marking = new AttendanceMarking(
                UUID.randomUUID(), id, personId, present, markedBy, clientMarkedAt);
        markings.put(personId, marking);
        pendingMarkings.add(marking);
        registerEvent(new AttendanceMarked(id, personId, present, markedBy, Instant.now()));
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
}
