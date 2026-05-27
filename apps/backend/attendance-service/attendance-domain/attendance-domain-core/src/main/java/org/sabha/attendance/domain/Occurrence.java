package org.sabha.attendance.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.sabha.common.AggregateRoot;

public class Occurrence extends AggregateRoot<UUID> {

    private final UUID id;
    private final UUID sabhaId;
    private final LocalDate date;
    private OccurrenceState state;
    private final Map<UUID, AttendanceMarking> markings = new LinkedHashMap<>();

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

    public void mark(UUID personId, boolean present, UUID markedBy) {
        if (state != OccurrenceState.OPEN_FOR_MARKING) {
            throw new OccurrenceNotOpenForMarkingException(id, state);
        }
        markings.put(personId, new AttendanceMarking(
                UUID.randomUUID(), id, personId, present, markedBy));
        registerEvent(new AttendanceMarked(id, personId, present, markedBy, Instant.now()));
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
