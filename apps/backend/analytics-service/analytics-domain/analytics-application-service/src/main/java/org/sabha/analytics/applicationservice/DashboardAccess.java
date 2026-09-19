package org.sabha.analytics.applicationservice;

import java.util.UUID;

import org.sabha.common.SantLookup;
import org.sabha.common.UserId;
import org.springframework.stereotype.Service;

/**
 * The dashboard Authorization Engine (Slice 17, ADR-0010). Decides which
 * {@link DashboardScope} a caller reads. Every role keeps the Slice 15
 * role-scoped view; a Sant is the one exception — they read any City in the
 * State, filtered to the City they last chose (their persisted default). A Sant
 * who has not chosen yet sees nothing. Stateless apart from its ports, so the
 * rule is exercised end-to-end without a database.
 *
 * <p>One decision, one return type. Picking a City is a command and lives in
 * {@link SantCityPreferenceService}; rendering the chip is a read and lives in
 * {@link CityChipQuery}. Both were methods here until ADR-0032 named the engine
 * contract: an engine returns a decision and holds no transaction.
 */
@Service
public class DashboardAccess {

    private final SantLookup sants;
    private final SantDefaultCity defaultCity;

    public DashboardAccess(SantLookup sants, SantDefaultCity defaultCity) {
        this.sants = sants;
        this.defaultCity = defaultCity;
    }

    /** The view to serve on landing: a Sant's chosen City, or the caller's role scope. */
    public DashboardScope viewFor(UserId caller) {
        UUID userId = caller.value();
        if (!sants.isSant(userId)) {
            return new DashboardScope.RoleScoped(userId);
        }
        return defaultCity.defaultCityOf(userId)
                .<DashboardScope>map(DashboardScope.CityScoped::new)
                .orElseGet(DashboardScope.NoCity::new);
    }
}
