package org.sabha.attendance.applicationservice;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.sabha.attendance.domain.MarkingType;
import org.sabha.attendance.domain.Occurrence;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records Attendance Markings against an Occurrence. This service owns only the
 * marking vocabulary — Roster presence vs Walk-in; the load-retry-save-publish
 * cycle it shares with every other writer of the aggregate lives in {@link
 * OccurrenceWriter}. Marking changes no lifecycle state, so it takes the writer's
 * unaudited path: each marking carries its own {@code markedBy}, and no
 * state-transition row is appended.
 */
@Service
public class MarkAttendanceApplicationService {

    private final OccurrenceWriter writer;

    public MarkAttendanceApplicationService(OccurrenceWriter writer) {
        this.writer = writer;
    }

    @Transactional
    public void execute(UUID keycloakSubject, UUID occurrenceId, UUID personId, boolean present,
                        Instant clientMarkedAt) {
        executeBatch(keycloakSubject, occurrenceId,
                List.of(MarkItem.roster(personId, present, clientMarkedAt)));
    }

    /**
     * Applies several markings against a single Occurrence inside one
     * load-mutate-save cycle (plus retry on optimistic-lock conflict). Used by
     * {@link SyncAttendanceApplicationService} so a batch of N items against one
     * Occurrence is N marks but only one DB load + one save. Each item carries its
     * own {@link MarkingType}, so a Walk-in is just an item with
     * {@code markingType = WALK_IN} and rides the same load/retry/save/publish path.
     */
    @Transactional
    public void executeBatch(UUID keycloakSubject, UUID occurrenceId, List<MarkItem> items) {
        writer.mutateUnaudited(occurrenceId, keycloakSubject, (occurrence, markedBy) -> {
            for (MarkItem item : items) {
                apply(occurrence, item, markedBy);
            }
        });
    }

    private static void apply(Occurrence occurrence, MarkItem item, UUID markedBy) {
        switch (item.markingType()) {
            case ROSTER -> occurrence.mark(item.personId(), item.present(), markedBy, item.clientMarkedAt());
            case WALK_IN -> occurrence.markWalkIn(item.personId(), markedBy, item.clientMarkedAt());
        }
    }

    /**
     * One marking to apply to an Occurrence. {@link #roster} is the Roster-presence
     * case (present may be true or false); {@link #walkIn} is a Person attending a
     * Sabha that is not one of their Home Sabhas — always present.
     */
    public record MarkItem(UUID personId, boolean present, Instant clientMarkedAt, MarkingType markingType) {

        public static MarkItem roster(UUID personId, boolean present, Instant clientMarkedAt) {
            return new MarkItem(personId, present, clientMarkedAt, MarkingType.ROSTER);
        }

        public static MarkItem walkIn(UUID personId, Instant clientMarkedAt) {
            return new MarkItem(personId, true, clientMarkedAt, MarkingType.WALK_IN);
        }
    }
}
