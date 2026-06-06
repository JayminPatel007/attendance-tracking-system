package org.sabha.container;

import org.sabha.analytics.applicationservice.ReEngagementProjectionScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cron entry point for the re-engagement candidate projection (ADR-0008,
 * ADR-0010). Spring scheduling is enabled globally by {@link OccurrenceCronJobs};
 * this rebuilds the dashboard read-model on a background cadence (every 15 minutes
 * by default) so analytics never run live against the transactional tables. The
 * refresh is idempotent and wholesale, so a missed tick simply self-heals on the
 * next run.
 */
@Component
public class AnalyticsCronJobs {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsCronJobs.class);

    private final ReEngagementProjectionScanner projectionScanner;

    public AnalyticsCronJobs(ReEngagementProjectionScanner projectionScanner) {
        this.projectionScanner = projectionScanner;
    }

    @Scheduled(cron = "${sabha.cron.reengagement-projection:0 0/15 * * * *}")
    public void refreshReEngagementProjection() {
        try {
            projectionScanner.refresh();
        } catch (RuntimeException e) {
            log.error("Re-engagement projection refresh failed", e);
        }
    }
}
