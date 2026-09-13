package org.sabha.attendance.applicationservice;

import java.util.Optional;

import org.sabha.common.UserId;
import org.springframework.stereotype.Service;

/**
 * The Occurrence the calling Sanchalak can currently shape. An empty result means
 * "nothing in the window right now" and nothing else — an unidentifiable caller
 * was already rejected at the edge (ADR-0030).
 */
@Service
public class GetCurrentOccurrenceUseCase {

    private final CurrentOccurrenceQuery query;

    public GetCurrentOccurrenceUseCase(CurrentOccurrenceQuery query) {
        this.query = query;
    }

    public Optional<CurrentOccurrence> execute(UserId caller) {
        return query.findShapeableForSanchalak(caller.value());
    }
}
