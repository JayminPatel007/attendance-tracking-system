package org.sabha.analytics.applicationservice;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.UserId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the dashboard City chip renders (Slice 17): a Sant gets every City and
 * their current pick; every other role gets an inert chip, which is how the web
 * knows to draw a static scope indicator instead of a picker.
 */
class CityChipQueryTest {

    private static final UUID SANT = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID NIRDESHAK = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID CITY_A = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

    private final FakeSantLookup sants = new FakeSantLookup();
    private final FakeSantDefaultCity defaults = new FakeSantDefaultCity();
    private final FakeCityDirectory cities = new FakeCityDirectory();
    private final CityChipQuery chip = new CityChipQuery(sants, defaults, cities);

    @Test
    void aSantsChipOffersEveryCityAndHighlightsTheirCurrentChoice() {
        sants.add(SANT);
        cities.add(CITY_A);
        defaults.choose(SANT, CITY_A);

        CityChip rendered = chip.forCaller(UserId.of(SANT));

        assertThat(rendered.sant()).isTrue();
        assertThat(rendered.selectedCityId()).isEqualTo(CITY_A);
        assertThat(rendered.cities()).extracting(CityOption::id).containsExactly(CITY_A);
    }

    @Test
    void aSantWhoHasNotPickedYetGetsAnInteractiveChipWithNoSelection() {
        sants.add(SANT);
        cities.add(CITY_A);

        CityChip rendered = chip.forCaller(UserId.of(SANT));

        assertThat(rendered.sant()).isTrue();
        assertThat(rendered.selectedCityId()).isNull();
        assertThat(rendered.cities()).extracting(CityOption::id).containsExactly(CITY_A);
    }

    @Test
    void aNonSantGetsAnInertChipWithNoCities() {
        cities.add(CITY_A);

        CityChip rendered = chip.forCaller(UserId.of(NIRDESHAK));

        assertThat(rendered.sant()).isFalse();
        assertThat(rendered.selectedCityId()).isNull();
        assertThat(rendered.cities()).isEmpty();
    }
}
