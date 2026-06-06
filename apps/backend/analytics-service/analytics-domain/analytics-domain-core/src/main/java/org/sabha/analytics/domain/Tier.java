package org.sabha.analytics.domain;

/**
 * How urgent a re-engagement candidate is (ADR-0010). {@link #CANDIDATE} at the
 * lower consecutive-missed threshold ("call them"), {@link #PRIORITY} at the
 * higher one ("the Nirikshak should know"). Both thresholds are MK-tunable.
 */
public enum Tier {
    CANDIDATE,
    PRIORITY
}
