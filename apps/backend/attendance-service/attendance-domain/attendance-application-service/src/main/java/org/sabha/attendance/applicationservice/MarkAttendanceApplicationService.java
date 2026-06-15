package org.sabha.attendance.applicationservice;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.sabha.attendance.domain.MarkingType;
import org.sabha.attendance.domain.Occurrence;
import org.sabha.common.CallerResolver;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.OptimisticLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarkAttendanceApplicationService {

    private static final int MAX_OPTIMISTIC_LOCK_ATTEMPTS = 3;

    private final CallerResolver callerResolver;
    private final OccurrenceRepository occurrences;
    private final DomainEventPublisher events;

    public MarkAttendanceApplicationService(
            CallerResolver callerResolver,
            OccurrenceRepository occurrences,
            DomainEventPublisher events) {
        this.callerResolver = callerResolver;
        this.occurrences = occurrences;
        this.events = events;
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
        UUID markedBy = callerResolver.requireUserId(keycloakSubject);

        OptimisticLockException lastConflict = null;
        for (int attempt = 0; attempt < MAX_OPTIMISTIC_LOCK_ATTEMPTS; attempt++) {
            Occurrence occurrence = occurrences.findById(occurrenceId)
                    .orElseThrow(() -> new OccurrenceNotFoundException(occurrenceId));

            for (MarkItem item : items) {
                apply(occurrence, item, markedBy);
            }

            try {
                occurrences.save(occurrence);
            } catch (OptimisticLockException retry) {
                lastConflict = retry;
                continue;
            }

            events.publishAll(occurrence.pullDomainEvents());
            return;
        }
        throw new org.sabha.common.ConcurrentModificationException(occurrenceId, lastConflict);
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
