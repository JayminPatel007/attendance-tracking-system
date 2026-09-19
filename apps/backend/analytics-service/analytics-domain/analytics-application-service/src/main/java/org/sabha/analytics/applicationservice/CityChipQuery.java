package org.sabha.analytics.applicationservice;

import java.util.List;
import java.util.UUID;

import org.sabha.common.CallerAuthority;
import org.springframework.stereotype.Service;

/**
 * Assembles the dashboard City chip (Slice 17) — a read, not a decision, which is
 * why it sits here rather than on the {@link DashboardAccess} engine (ADR-0032,
 * rule R3: an engine returns a decision, never a DTO).
 *
 * <p>Not a {@code *Queries} port, because the Sant test reads identity's
 * {@code role_assignments} and that table's SQL is confined to
 * {@code identity-data-access} (ADR-0029, rule R1). The chip is composed above
 * the ports instead, in this ring, and stays exercisable without a database —
 * and since ADR-0032 the Sant test needs no port at all, because it is a question
 * about the caller's own rows.
 */
@Service
public class CityChipQuery {

    private final SantDefaultCity defaultCity;
    private final CityDirectory cities;

    public CityChipQuery(SantDefaultCity defaultCity, CityDirectory cities) {
        this.defaultCity = defaultCity;
        this.cities = cities;
    }

    /** What the dashboard City chip should render for this caller. */
    public CityChip forCaller(CallerAuthority caller) {
        UUID userId = caller.userId().value();
        if (!caller.isSant()) {
            return new CityChip(false, null, List.of());
        }
        return new CityChip(true, defaultCity.defaultCityOf(userId).orElse(null), cities.allCities());
    }
}
