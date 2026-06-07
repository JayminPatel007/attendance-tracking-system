package org.sabha.analytics.applicationservice;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * The dashboard Authorization Engine (Slice 17, ADR-0010). Decides which
 * {@link DashboardScope} a caller reads. Every role keeps the Slice 15
 * role-scoped view; a Sant is the one exception — they read any City in the
 * State, filtered to the City they last chose (their persisted default). A Sant
 * who has not chosen yet sees nothing. Stateless apart from its ports, so the
 * rule is exercised end-to-end without a database.
 */
@Service
public class DashboardAccess {

    private final SantDirectory sants;
    private final SantDefaultCity defaultCity;
    private final CityDirectory cities;

    public DashboardAccess(SantDirectory sants, SantDefaultCity defaultCity, CityDirectory cities) {
        this.sants = sants;
        this.defaultCity = defaultCity;
        this.cities = cities;
    }

    /** The view to serve on landing: a Sant's chosen City, or the caller's role scope. */
    public DashboardScope viewFor(UUID userId) {
        if (!sants.isSant(userId)) {
            return new DashboardScope.RoleScoped(userId);
        }
        return defaultCity.defaultCityOf(userId)
                .<DashboardScope>map(DashboardScope.CityScoped::new)
                .orElseGet(DashboardScope.NoCity::new);
    }

    /**
     * A Sant picks a City: persist it as their default (the chosen City <em>is</em>
     * the default) and return the City-scoped view to render immediately.
     */
    public DashboardScope selectCity(UUID userId, UUID cityId) {
        if (!sants.isSant(userId)) {
            throw new NotASantException(userId);
        }
        if (!cities.exists(cityId)) {
            throw new CityNotFoundException(cityId);
        }
        defaultCity.choose(userId, cityId);
        return new DashboardScope.CityScoped(cityId);
    }

    /**
     * What the dashboard City chip should render for this caller: a Sant gets the
     * full City list and their current choice; everyone else gets an inert chip
     * (the web shows a non-interactive scope indicator instead).
     */
    public CityChip cityChip(UUID userId) {
        if (!sants.isSant(userId)) {
            return new CityChip(false, null, List.of());
        }
        return new CityChip(true, defaultCity.defaultCityOf(userId).orElse(null), cities.allCities());
    }

    public record CityChip(boolean sant, UUID selectedCityId, List<CityOption> cities) {
    }
}
