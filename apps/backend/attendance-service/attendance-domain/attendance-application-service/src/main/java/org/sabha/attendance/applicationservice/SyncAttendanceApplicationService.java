package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sabha.attendance.applicationservice.MarkAttendanceApplicationService.MarkItem;
import org.sabha.common.CallerResolver;
import org.springframework.stereotype.Service;

/**
 * Applies a batch of offline-queued Attendance Markings from a Sanchalak's
 * mobile (ADR-0007). Enforces the 7-day Roster freshness gate up front, then
 * delegates each item to {@link MarkAttendanceApplicationService}, which
 * applies LWW by {@code clientMarkedAt}.
 */
@Service
public class SyncAttendanceApplicationService {

    public static final Duration MAX_ROSTER_AGE = Duration.ofDays(7);

    private final CallerResolver callerResolver;
    private final MarkAttendanceApplicationService markAttendance;
    private final Clock clock;

    public SyncAttendanceApplicationService(
            CallerResolver callerResolver,
            MarkAttendanceApplicationService markAttendance,
            Clock clock) {
        this.callerResolver = callerResolver;
        this.markAttendance = markAttendance;
        this.clock = clock;
    }

    public SyncResult execute(UUID keycloakSubject, Instant clientRosterVersion, List<SyncRequestItem> items) {
        callerResolver.resolveUserId(keycloakSubject)
                .orElseThrow(() -> new CallerUnknownException(keycloakSubject));

        Instant now = clock.instant();
        Duration age = Duration.between(clientRosterVersion, now);
        if (age.compareTo(MAX_ROSTER_AGE) > 0) {
            throw new StaleRosterException(clientRosterVersion, now, MAX_ROSTER_AGE);
        }

        Map<UUID, List<MarkItem>> byOccurrence = groupByOccurrence(items);
        byOccurrence.forEach((occurrenceId, batch) ->
                markAttendance.executeBatch(keycloakSubject, occurrenceId, batch));
        return new SyncResult(items.size());
    }

    private static Map<UUID, List<MarkItem>> groupByOccurrence(List<SyncRequestItem> items) {
        Map<UUID, List<MarkItem>> grouped = new LinkedHashMap<>();
        for (SyncRequestItem item : items) {
            grouped.computeIfAbsent(item.occurrenceId(), k -> new java.util.ArrayList<>())
                    .add(MarkItem.roster(item.personId(), item.present(), item.clientMarkedAt()));
        }
        return grouped;
    }
}
