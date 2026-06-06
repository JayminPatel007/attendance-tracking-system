package org.sabha.analytics.applicationservice;

import org.sabha.analytics.domain.Thresholds;

/**
 * Port exposing the current MK-owned re-engagement {@link Thresholds} (ADR-0010).
 * Read once per calculation; tunable, not hardcoded.
 */
public interface ThresholdConfig {

    Thresholds current();
}
