package org.sabha.analytics.applicationservice;

import org.sabha.analytics.domain.Scope;
import org.springframework.stereotype.Service;

/**
 * Rebuilds the re-engagement candidate read-model (ADR-0008, ADR-0010): runs the
 * Calculator State-wide and replaces the projection with its output. Driven on a
 * background cadence by the container's scheduler, never live against the
 * transactional tables.
 */
@Service
public class ReEngagementProjectionScanner {

    private final ReEngagementCandidateCalculator calculator;
    private final CandidateProjectionStore store;

    public ReEngagementProjectionScanner(ReEngagementCandidateCalculator calculator, CandidateProjectionStore store) {
        this.calculator = calculator;
        this.store = store;
    }

    public void refresh() {
        store.replaceAll(calculator.candidatesFor(new Scope.Everything()));
    }
}
