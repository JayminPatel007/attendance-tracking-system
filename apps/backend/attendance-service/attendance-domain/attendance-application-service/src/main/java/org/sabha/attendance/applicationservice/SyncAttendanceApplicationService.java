package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sabha.attendance.applicationservice.MarkAttendanceApplicationService.MarkItem;
import org.sabha.common.UserActivityRecorder;
import org.sabha.common.UserId;
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

    private final MarkAttendanceApplicationService markAttendance;
    private final UserActivityRecorder activity;
    private final Clock clock;

    public SyncAttendanceApplicationService(
            MarkAttendanceApplicationService markAttendance,
            UserActivityRecorder activity,
            Clock clock) {
        this.markAttendance = markAttendance;
        this.activity = activity;
        this.clock = clock;
    }

    public SyncResult execute(UserId caller, Instant clientRosterVersion, List<SyncRequestItem> items) {
        Instant now = clock.instant();
        Duration age = Duration.between(clientRosterVersion, now);
        if (age.compareTo(MAX_ROSTER_AGE) > 0) {
            throw new StaleRosterException(clientRosterVersion, now, MAX_ROSTER_AGE);
        }

        Map<UUID, List<MarkItem>> byOccurrence = groupByOccurrence(items);
        byOccurrence.forEach((occurrenceId, batch) ->
                markAttendance.executeBatch(caller, occurrenceId, batch));
        activity.recordSync(caller.value(), now);
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
