package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

        int applied = 0;
        for (SyncRequestItem item : items) {
            markAttendance.execute(keycloakSubject, item.occurrenceId(), item.personId(),
                    item.present(), item.clientMarkedAt());
            applied++;
        }
        return new SyncResult(applied);
    }
}
