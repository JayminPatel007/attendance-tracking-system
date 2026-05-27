package org.sabha.attendance.applicationservice;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.sabha.attendance.domain.OccurrenceState;

/**
 * Read-side projection for the Sanchalak's mobile screen: the current open
 * Occurrence plus its roster of expected attendees (with any markings made so
 * far). The {@code rosterVersion} is the moment the server emitted this
 * snapshot — the mobile echoes it back when syncing markings so the server can
 * enforce the 7-day staleness gate (ADR-0007).
 */
public record CurrentRoster(OccurrenceView occurrence, List<RosterEntry> roster, Instant rosterVersion) {

    public record OccurrenceView(UUID id, LocalDate date, OccurrenceState state, UUID sabhaId) {
    }

    public record RosterEntry(UUID personId, String fullName, Boolean present) {
    }
}
