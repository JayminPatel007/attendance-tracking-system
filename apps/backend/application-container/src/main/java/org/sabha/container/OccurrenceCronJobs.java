package org.sabha.container;

import org.sabha.attendance.applicationservice.AutoFinalizeScanner;
import org.sabha.attendance.applicationservice.AutoOpenScanner;
import org.sabha.attendance.applicationservice.WeeklyMaterializationScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cron entry points for Slice 3's auto-Open and auto-Finalize transitions
 * (PRD-0001 + ADR-0001). Spring's {@code @Scheduled} drives both scanners.
 *
 * <p>Cadence:</p>
 * <ul>
 *   <li>Auto-Open at the top of every minute — granular enough to honour
 *       per-Sabha start times that fall on quarter-hour boundaries.</li>
 *   <li>Auto-Finalize at the top of every hour — Occurrences finalize 24h
 *       after their scheduled end, so an hourly cadence keeps the lag
 *       bounded to well within the analytics-stability budget.</li>
 *   <li>Weekly materialization once daily — rolls each weekly Sabha's
 *       Occurrences forward on an 8-week window (Slice 12 / ADR-0012); a daily
 *       cadence is ample for a rolling weekly schedule and is idempotent.</li>
 * </ul>
 */
@Component
@EnableScheduling
public class OccurrenceCronJobs {

    private static final Logger log = LoggerFactory.getLogger(OccurrenceCronJobs.class);

    private final AutoOpenScanner autoOpenScanner;
    private final AutoFinalizeScanner autoFinalizeScanner;
    private final WeeklyMaterializationScanner weeklyMaterializationScanner;

    public OccurrenceCronJobs(AutoOpenScanner autoOpenScanner,
                              AutoFinalizeScanner autoFinalizeScanner,
                              WeeklyMaterializationScanner weeklyMaterializationScanner) {
        this.autoOpenScanner = autoOpenScanner;
        this.autoFinalizeScanner = autoFinalizeScanner;
        this.weeklyMaterializationScanner = weeklyMaterializationScanner;
    }

    @Scheduled(cron = "${sabha.cron.auto-open:0 * * * * *}")
    public void runAutoOpen() {
        try {
            autoOpenScanner.scan();
        } catch (RuntimeException e) {
            log.error("Auto-Open scan failed", e);
        }
    }

    @Scheduled(cron = "${sabha.cron.auto-finalize:0 0 * * * *}")
    public void runAutoFinalize() {
        try {
            autoFinalizeScanner.scan();
        } catch (RuntimeException e) {
            log.error("Auto-Finalize scan failed", e);
        }
    }

    @Scheduled(cron = "${sabha.cron.weekly-materialization:0 0 2 * * *}")
    public void runWeeklyMaterialization() {
        try {
            weeklyMaterializationScanner.scan();
        } catch (RuntimeException e) {
            log.error("Weekly materialization scan failed", e);
        }
    }
}
