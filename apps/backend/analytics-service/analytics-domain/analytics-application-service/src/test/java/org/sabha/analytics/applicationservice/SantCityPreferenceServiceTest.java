package org.sabha.analytics.applicationservice;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.CityNotFoundException;
import org.sabha.common.UserId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A Sant picking a dashboard City (Slice 17): the chosen City is the persisted
 * default, so it survives across logins. Both guards run before the write, and
 * both leave nothing behind — the assertions on the default are what make the
 * order observable.
 */
class SantCityPreferenceServiceTest {

    private static final UUID SANT = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID NIRDESHAK = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID CITY_A = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

    private final FakeSantLookup sants = new FakeSantLookup();
    private final FakeSantDefaultCity defaults = new FakeSantDefaultCity();
    private final FakeCityDirectory cities = new FakeCityDirectory();
    private final SantCityPreferenceService preference =
            new SantCityPreferenceService(sants, defaults, cities);

    @Test
    void aSantPickingACityScopesToItAndPersistsItAsTheDefault() {
        sants.add(SANT);
        cities.add(CITY_A);

        DashboardScope scope = preference.selectCity(UserId.of(SANT), CITY_A);

        assertThat(scope).isEqualTo(new DashboardScope.CityScoped(CITY_A));
        assertThat(defaults.defaultCityOf(SANT)).contains(CITY_A);
    }

    @Test
    void aNonSantCannotPickACityAndNothingIsPersisted() {
        cities.add(CITY_A);

        assertThatThrownBy(() -> preference.selectCity(UserId.of(NIRDESHAK), CITY_A))
                .isInstanceOf(NotASantException.class);
        assertThat(defaults.defaultCityOf(NIRDESHAK)).isEmpty();
    }

    @Test
    void pickingAnUnknownCityIsRejectedAndNothingIsPersisted() {
        sants.add(SANT);

        assertThatThrownBy(() -> preference.selectCity(UserId.of(SANT), CITY_A))
                .isInstanceOf(CityNotFoundException.class);
        assertThat(defaults.defaultCityOf(SANT)).isEmpty();
    }
}
