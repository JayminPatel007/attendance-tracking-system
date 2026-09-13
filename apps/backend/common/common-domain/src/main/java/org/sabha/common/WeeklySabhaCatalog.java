package org.sabha.common;

import java.util.List;

/**
 * Cross-context read port (ADR-0019): all weekly-recurring Sabhas with their
 * standing schedules. The attendance context's weekly materialization cron
 * (ADR-0012) iterates these to roll Occurrences forward on an 8-week window;
 * monthly-ad-hoc Sabhas are excluded. The implementation reads the sabha-owned
 * {@code sabhas} table and lives in {@code sabha-data-access}.
 */
public interface WeeklySabhaCatalog {

    List<WeeklySabhaRef> findAllWeekly();
}
