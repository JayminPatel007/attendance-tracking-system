package org.sabha.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SabhaKindTest {

    @Test
    void parses_the_track_and_demographic_around_the_first_underscore() {
        SabhaKind kind = SabhaKind.parse("REGULAR_YUVAK");

        assertThat(kind.track()).isEqualTo("REGULAR");
        assertThat(kind.demographic()).isEqualTo("YUVAK");
    }

    @Test
    void demographic_is_everything_after_the_first_underscore_even_when_multi_token() {
        // The encoding is TRACK_DEMOGRAPHIC; only the first underscore is the
        // track/demographic boundary, so a future multi-token demographic stays whole.
        SabhaKind kind = SabhaKind.parse("REGULAR_BAL_MANDAL");

        assertThat(kind.track()).isEqualTo("REGULAR");
        assertThat(kind.demographic()).isEqualTo("BAL_MANDAL");
    }

    @Test
    void encode_round_trips_with_parse() {
        assertThat(SabhaKind.encode("YSS", "YUVAK")).isEqualTo("YSS_YUVAK");
    }

    @Test
    void demographicSql_derives_the_demographic_from_a_qualified_sabha_kind_column() {
        assertThat(SabhaKind.demographicSql("s")).isEqualTo("split_part(s.sabha_kind, '_', 2)");
    }
}
