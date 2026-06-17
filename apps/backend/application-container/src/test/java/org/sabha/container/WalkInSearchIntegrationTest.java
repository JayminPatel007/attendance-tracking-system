package org.sabha.container;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.identity.applicationservice.directory.SearchWalkInCandidatesUseCase;
import org.sabha.identity.applicationservice.directory.WalkInCandidate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives walk-in Directory search against a real Postgres + the slice-2/slice-6
 * seed. From the Yuvak tracer Sabha's context it finds the seeded Baal-Sabha
 * Person (a cross-demographic visitor), by fuzzy name and by exact mobile, each
 * carrying their current Home Sabha for the confirm sheet (Slice 7, issue #8).
 */
@SpringBootTest
@Import(WalkInSearchIntegrationTest.NoAuthConfig.class)
@Transactional
class WalkInSearchIntegrationTest extends PostgresIntegrationTest {

    // Seeded by slice-2/002-seed.sql and slice-6/001-person-directory.sql.
    private static final UUID KSHETRA_TRACER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SABHA_TRACER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID BAAL_SABHA = UUID.fromString("00000000-0000-0000-0000-000000000111");
    private static final UUID SEEDED_RAMESH = UUID.fromString("00000000-0000-0000-0000-000000000110");
    private static final String RAMESH_MOBILE = "+910000000110";

    // Inserted per-test (and cleaned up) to exercise a Person with more than one
    // Home Sabha — the case MIN(sabha_kind) used to collapse.
    private static final UUID MULTI_HOME_PERSON = UUID.fromString("00000000-0000-0000-0000-0000000002a1");
    private static final UUID SANYUKTA_SABHA = UUID.fromString("00000000-0000-0000-0000-0000000002a2");
    private static final String MULTI_HOME_MOBILE = "+910000000201";

    @Autowired
    SearchWalkInCandidatesUseCase searchWalkIn;

    @Autowired
    JdbcClient jdbc;

    @Test
    void aFuzzyNameSearchFindsACrossDemographicVisitorWithTheirHomeSabha() {
        List<WalkInCandidate> results = searchWalkIn.search(SABHA_TRACER, "Ramish Shah");

        assertThat(results).extracting(WalkInCandidate::personId).contains(SEEDED_RAMESH);
        assertThat(results)
                .filteredOn(c -> c.personId().equals(SEEDED_RAMESH))
                .singleElement()
                .satisfies(c -> assertThat(c.homeSabhas()).containsExactly("REGULAR_BAAL"));
    }

    @Test
    void anExactMobileSearchFindsThePersonWithTheirHomeSabha() {
        List<WalkInCandidate> results = searchWalkIn.search(SABHA_TRACER, RAMESH_MOBILE);

        assertThat(results).singleElement().satisfies(c -> {
            assertThat(c.personId()).isEqualTo(SEEDED_RAMESH);
            assertThat(c.fullName()).isEqualTo("Ramesh Shah");
            assertThat(c.homeSabhas()).containsExactly("REGULAR_BAAL");
        });
    }

    @Test
    void aSearchReturnsAllOfAMultiHomeSabhaPersonsKinds() {
        // A Person with both a demographic (Baal) and the universal Sanyukta Home
        // Sabha. MIN(sabha_kind) would surface only "REGULAR_BAAL"; the candidate
        // must carry both (CONTEXT.md: one Home Sabha per kind).
        jdbc.sql("""
                INSERT INTO sabhas (id, kshetra_id, sabha_kind, schedule_shape,
                                    day_of_week, start_time, end_time, standing_venue)
                VALUES (?, ?, 'REGULAR_SANYUKTA', 'WEEKLY_RECURRING', 0,
                        '09:00:00', '10:00:00', 'Sanyukta Hall, Kshetra Tracer')
                """)
                .param(SANYUKTA_SABHA).param(KSHETRA_TRACER).update();
        jdbc.sql("INSERT INTO persons (id, full_name, gender, mobile) VALUES (?, 'Multi Home Tester', 'MALE', ?)")
                .param(MULTI_HOME_PERSON).param(MULTI_HOME_MOBILE).update();
        jdbc.sql("INSERT INTO home_sabhas (person_id, sabha_id) VALUES (?, ?)")
                .param(MULTI_HOME_PERSON).param(BAAL_SABHA).update();
        jdbc.sql("INSERT INTO home_sabhas (person_id, sabha_id) VALUES (?, ?)")
                .param(MULTI_HOME_PERSON).param(SANYUKTA_SABHA).update();

        List<WalkInCandidate> results = searchWalkIn.search(SABHA_TRACER, MULTI_HOME_MOBILE);

        assertThat(results).singleElement().satisfies(c ->
                assertThat(c.homeSabhas()).containsExactly("REGULAR_BAAL", "REGULAR_SANYUKTA"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class NoAuthConfig {

        // No JWT is decoded in this test; the use case is exercised in-process.
        @Bean
        @Primary
        JwtDecoder testJwtDecoder() {
            return token -> {
                throw new UnsupportedOperationException("JWT decoding not exercised");
            };
        }
    }
}
