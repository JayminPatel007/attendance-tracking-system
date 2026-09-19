package org.sabha.analytics.applicationservice;

import java.util.UUID;

import org.sabha.common.CityNotFoundException;
import org.sabha.common.SantLookup;
import org.sabha.common.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A Sant's choice of dashboard City (Slice 17): guard it, persist it, and hand
 * back the view to render. This lived on the {@link DashboardAccess} engine until
 * ADR-0032 — a command with a guard in front, throwing twice and writing through
 * {@link SantDefaultCity#choose}, inside a class documented as a pure decision.
 * Here throwing and mutating are the contract.
 *
 * <p>{@code @Transactional} is the correctness half of that move: {@code
 * DashboardAccess} carried none, so this write ran untransacted and ADR-0018's
 * placement rule (issue #69) had nothing to fire on. A return-type rule cannot
 * see a write — R3 would have passed {@code selectCity}, which returns a sealed
 * scope — so relocation is the only instrument that fixes it.
 */
@Service
public class SantCityPreferenceService {

    private final SantLookup sants;
    private final SantDefaultCity defaultCity;
    private final CityDirectory cities;

    public SantCityPreferenceService(SantLookup sants, SantDefaultCity defaultCity, CityDirectory cities) {
        this.sants = sants;
        this.defaultCity = defaultCity;
        this.cities = cities;
    }

    /**
     * Persist the Sant's pick as their default (the chosen City <em>is</em> the
     * default) and return the City-scoped view. The BFF answers 204 and discards
     * the scope; it is still returned because the guards and the write are what a
     * caller has to see succeed together, and a caller rendering in place gets the
     * new view without a second round trip.
     *
     * @throws NotASantException     the caller is not a Sant (403) — no other role has a City chip
     * @throws CityNotFoundException the City does not exist (404)
     */
    @Transactional
    public DashboardScope selectCity(UserId caller, UUID cityId) {
        UUID userId = caller.value();
        if (!sants.isSant(userId)) {
            throw new NotASantException(userId);
        }
        if (!cities.exists(cityId)) {
            throw new CityNotFoundException(cityId);
        }
        defaultCity.choose(userId, cityId);
        return new DashboardScope.CityScoped(cityId);
    }
}
