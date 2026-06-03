package org.sabha.attendance.applicationservice;

import org.sabha.attendance.domain.Occurrence;

/**
 * Driven port for inserting a newly-created Occurrence (Slice 12 / ADR-0012):
 * the weekly materialization cron and monthly-ad-hoc manual creation both add
 * fresh {@code Scheduled} Occurrences. Segregated from {@link OccurrenceRepository}
 * (whose {@code save} is an optimistic-locked update of an existing aggregate) so
 * inserters depend only on what they use. The JDBC implementation lives in
 * attendance-data-access.
 */
public interface OccurrenceInsert {

    void add(Occurrence occurrence);
}
