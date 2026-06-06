package org.sabha.analytics.applicationservice;

import java.util.List;

import org.sabha.analytics.domain.HomeSabhaHistory;
import org.sabha.analytics.domain.Scope;

/**
 * Port supplying the Calculator with each Person's per-Home-Sabha outcome stream
 * within a Scope. The production adapter reads the transactional tables (or the
 * projection that backs them); tests supply an in-memory stand-in.
 */
public interface HomeSabhaOccurrenceHistory {

    List<HomeSabhaHistory> within(Scope scope);
}
