package org.sabha.attendance.applicationservice;

import java.util.Optional;

import org.sabha.common.UserId;
import org.springframework.stereotype.Service;

/**
 * The Roster the calling Sanchalak should be marking against. An empty result
 * means "no Sabha to mark right now" and nothing else — an unidentifiable caller
 * was already rejected at the edge (ADR-0030).
 */
@Service
public class GetCurrentRosterUseCase {

    private final CurrentRosterQuery query;

    public GetCurrentRosterUseCase(CurrentRosterQuery query) {
        this.query = query;
    }

    public Optional<CurrentRoster> execute(UserId caller) {
        return query.findForSanchalak(caller.value());
    }
}
