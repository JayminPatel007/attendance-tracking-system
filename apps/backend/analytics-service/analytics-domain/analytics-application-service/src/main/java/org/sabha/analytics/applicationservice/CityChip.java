package org.sabha.analytics.applicationservice;

import java.util.List;
import java.util.UUID;

/**
 * View-model for the dashboard City chip (Slice 17), assembled by
 * {@link CityChipQuery}. A Sant gets {@code sant = true}, every City in the State
 * and their current pick; every other role gets an inert chip the web replaces
 * with a static scope indicator.
 *
 * <p>A top-level record rather than a member of an engine: it was nested in
 * {@code DashboardAccess}, which made that engine return a DTO — the shape rule
 * R3 forbids (ADR-0032). The wire contract is unchanged; springdoc names the
 * schema {@code CityChip} either way.
 */
public record CityChip(boolean sant, UUID selectedCityId, List<CityOption> cities) {
}
