package org.sabha.analytics.applicationservice;

import org.sabha.analytics.domain.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Wholesale projection rebuild. {@code @Transactional} here — not on the
     * {@code CandidateProjectionStore} adapter — keeps the transaction boundary
     * in the use-case tier per ADR-0018: the clear-and-reinsert must be atomic so
     * a concurrent dashboard read sees the old projection or the new one, never a
     * partial rebuild.
     */
    @Transactional
    public void refresh() {
        store.replaceAll(calculator.candidatesFor(new Scope.Everything()));
    }
}
