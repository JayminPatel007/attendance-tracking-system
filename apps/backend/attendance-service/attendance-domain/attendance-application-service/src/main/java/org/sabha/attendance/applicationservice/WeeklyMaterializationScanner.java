package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import org.sabha.attendance.domain.Occurrence;
import org.sabha.common.SabhaSchedule;
import org.sabha.common.WeeklySabhaCatalog;
import org.sabha.common.WeeklySabhaRef;
import org.springframework.stereotype.Component;

/**
 * Weekly Occurrence materialization (Slice 12 / ADR-0012). Driven by Spring
 * scheduling: for every weekly-recurring Sabha, it ensures a {@code Scheduled}
 * Occurrence exists on each standing slot across a rolling 8-week forward window.
 *
 * <p>The window starts at the first slot at least 24h out — the next calendar
 * day-of-week whose start is {@code >= 24h} away; if that next slot is sooner than
 * 24h (or already past), it skips to the following week. Monthly-ad-hoc Sabhas are
 * not in the catalog, so they are never materialized. The scan is idempotent:
 * dates already present (via {@link OccurrenceCalendar}) are left untouched.</p>
 */
@Component
public class WeeklyMaterializationScanner {

    private static final Duration MIN_LEAD = Duration.ofHours(24);
    private static final int WINDOW_WEEKS = 8;

    private final WeeklySabhaCatalog catalog;
    private final OccurrenceCalendar calendar;
    private final OccurrenceInsert occurrences;
    private final Clock clock;

    public WeeklyMaterializationScanner(
            WeeklySabhaCatalog catalog,
            OccurrenceCalendar calendar,
            OccurrenceInsert occurrences,
            Clock clock) {
        this.catalog = catalog;
        this.calendar = calendar;
        this.occurrences = occurrences;
        this.clock = clock;
    }

    public void scan() {
        Instant now = clock.instant();
        ZoneId zone = clock.getZone();
        LocalDate windowEnd = LocalDate.ofInstant(now, zone).plusWeeks(WINDOW_WEEKS);

        for (WeeklySabhaRef ref : catalog.findAllWeekly()) {
            LocalDate slot = firstSlotWithLead(now, zone, ref.schedule());
            while (!slot.isAfter(windowEnd)) {
                if (!calendar.exists(ref.sabhaId(), slot)) {
                    occurrences.add(Occurrence.scheduled(UUID.randomUUID(), ref.sabhaId(), slot));
                }
                slot = slot.plusWeeks(1);
            }
        }
    }

    private LocalDate firstSlotWithLead(Instant now, ZoneId zone, SabhaSchedule schedule) {
        LocalDate today = LocalDate.ofInstant(now, zone);
        LocalDate candidate = today.with(TemporalAdjusters.nextOrSame(schedule.dayOfWeek()));
        Instant slotStart = ZonedDateTime.of(candidate, schedule.startTime(), zone).toInstant();
        if (Duration.between(now, slotStart).compareTo(MIN_LEAD) < 0) {
            candidate = candidate.plusWeeks(1);
        }
        return candidate;
    }
}
