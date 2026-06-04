package org.sabha.attendance.applicationservice;

import java.util.List;
import java.util.UUID;

/**
 * Read-side projection backing the web Occurrence-reopen screen (Slice 13).
 * Returns the Occurrences a reopener (Nirikshak / Nirdeshak / Sah-Nirdeshak) may
 * act on: those whose Sabha sits in a Kshetra and demographic the caller holds a
 * reopen role on, restricted to the states the screen cares about (Finalized, or
 * anything already reopened). The "reopened" badge and last reason are derived
 * from the state-transition audit log.
 */
public interface OccurrenceReopenQueries {

    List<ReopenListItem> listForReopener(UUID userId);
}
