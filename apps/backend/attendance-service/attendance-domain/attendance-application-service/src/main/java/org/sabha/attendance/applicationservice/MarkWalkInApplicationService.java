package org.sabha.attendance.applicationservice;

import java.time.Instant;
import java.util.UUID;

import org.sabha.attendance.domain.Occurrence;
import org.sabha.common.CallerResolver;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.OptimisticLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records a Walk-in against an Occurrence (ADR-0007): a Person attending a Sabha
 * that is not one of their Home Sabhas. The Person is found via the online
 * Directory search (Slice 6) before this call; here we load the Occurrence,
 * record the Walk-in (always present, never touching Home Sabha), and save with
 * the same optimistic-lock retry as {@link MarkAttendanceApplicationService}
 * since the Sah-Sanchalak may be marking the same Occurrence concurrently.
 */
@Service
public class MarkWalkInApplicationService {

    private static final int MAX_OPTIMISTIC_LOCK_ATTEMPTS = 3;

    private final CallerResolver callerResolver;
    private final OccurrenceRepository occurrences;
    private final DomainEventPublisher events;

    public MarkWalkInApplicationService(
            CallerResolver callerResolver,
            OccurrenceRepository occurrences,
            DomainEventPublisher events) {
        this.callerResolver = callerResolver;
        this.occurrences = occurrences;
        this.events = events;
    }

    @Transactional
    public void execute(UUID keycloakSubject, UUID occurrenceId, UUID personId, Instant clientMarkedAt) {
        UUID markedBy = callerResolver.resolveUserId(keycloakSubject)
                .orElseThrow(() -> new CallerUnknownException(keycloakSubject));

        OptimisticLockException lastConflict = null;
        for (int attempt = 0; attempt < MAX_OPTIMISTIC_LOCK_ATTEMPTS; attempt++) {
            Occurrence occurrence = occurrences.findById(occurrenceId)
                    .orElseThrow(() -> new OccurrenceNotFoundException(occurrenceId));

            occurrence.markWalkIn(personId, markedBy, clientMarkedAt);

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
}
