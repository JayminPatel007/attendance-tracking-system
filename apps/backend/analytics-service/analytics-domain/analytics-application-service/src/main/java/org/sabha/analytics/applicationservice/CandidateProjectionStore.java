package org.sabha.analytics.applicationservice;

import java.util.List;

import org.sabha.analytics.domain.Candidate;

/**
 * Port the projection scanner writes through: it replaces the entire
 * re-engagement read-model with the latest calculator output in one atomic step,
 * so the dashboard never observes a half-rebuilt projection.
 */
public interface CandidateProjectionStore {

    void replaceAll(List<Candidate> candidates);
}
