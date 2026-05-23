package org.sabha.attendance.applicationservice;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.sabha.attendance.domain.OccurrenceState;

/**
 * Read-side projection for the Sanchalak's mobile screen: the current open
 * Occurrence plus its roster of expected attendees (with any markings made so
 * far). Built by {@link CurrentRosterQuery}.
 */
public record CurrentRoster(OccurrenceView occurrence, List<RosterEntry> roster) {

    public record OccurrenceView(UUID id, LocalDate date, OccurrenceState state, UUID sabhaId) {
    }

    public record RosterEntry(UUID personId, String fullName, Boolean present) {
    }
}
