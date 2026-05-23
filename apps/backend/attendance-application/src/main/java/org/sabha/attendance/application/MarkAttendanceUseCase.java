package org.sabha.attendance.application;

import java.util.UUID;

import org.sabha.attendance.domain.AttendanceMarking;
import org.sabha.attendance.domain.AttendanceMarkingRepository;
import org.sabha.attendance.domain.Occurrence;
import org.sabha.attendance.domain.OccurrenceRepository;
import org.sabha.common.CallerResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarkAttendanceUseCase {

    private final CallerResolver callerResolver;
    private final OccurrenceRepository occurrences;
    private final AttendanceMarkingRepository markings;

    public MarkAttendanceUseCase(
            CallerResolver callerResolver,
            OccurrenceRepository occurrences,
            AttendanceMarkingRepository markings) {
        this.callerResolver = callerResolver;
        this.occurrences = occurrences;
        this.markings = markings;
    }

    @Transactional
    public void execute(UUID keycloakSubject, UUID occurrenceId, UUID personId, boolean present) {
        UUID markedBy = callerResolver.resolveUserId(keycloakSubject)
                .orElseThrow(() -> new CallerUnknownException(keycloakSubject));

        Occurrence occurrence = occurrences.findById(occurrenceId)
                .orElseThrow(() -> new OccurrenceNotFoundException(occurrenceId));

        if (!occurrence.isOpenForMarking()) {
            throw new OccurrenceNotOpenForMarkingException(occurrenceId, occurrence.state());
        }

        markings.upsert(new AttendanceMarking(
                UUID.randomUUID(),
                occurrenceId,
                personId,
                present,
                markedBy));
    }
}
