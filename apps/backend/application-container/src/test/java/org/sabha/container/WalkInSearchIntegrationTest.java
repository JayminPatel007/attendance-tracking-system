package org.sabha.container;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.identity.applicationservice.SearchWalkInCandidatesUseCase;
import org.sabha.identity.applicationservice.WalkInCandidate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives walk-in Directory search against a real Postgres + the slice-2/slice-6
 * seed. From the Yuvak tracer Sabha's context it finds the seeded Baal-Sabha
 * Person (a cross-demographic visitor), by fuzzy name and by exact mobile, each
 * carrying their current Home Sabha for the confirm sheet (Slice 7, issue #8).
 */
@SpringBootTest
@Import(WalkInSearchIntegrationTest.NoAuthConfig.class)
@Testcontainers
class WalkInSearchIntegrationTest {

    // Seeded by slice-2/002-seed.sql and slice-6/001-person-directory.sql.
    private static final UUID SABHA_TRACER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SEEDED_RAMESH = UUID.fromString("00000000-0000-0000-0000-000000000110");
    private static final String RAMESH_MOBILE = "+910000000110";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    SearchWalkInCandidatesUseCase searchWalkIn;

    @Test
    void aFuzzyNameSearchFindsACrossDemographicVisitorWithTheirHomeSabha() {
        List<WalkInCandidate> results = searchWalkIn.search(SABHA_TRACER, "Ramish Shah");

        assertThat(results).extracting(WalkInCandidate::personId).contains(SEEDED_RAMESH);
        assertThat(results)
                .filteredOn(c -> c.personId().equals(SEEDED_RAMESH))
                .singleElement()
                .extracting(WalkInCandidate::homeSabha)
                .isEqualTo("REGULAR_BAAL");
    }

    @Test
    void anExactMobileSearchFindsThePersonWithTheirHomeSabha() {
        List<WalkInCandidate> results = searchWalkIn.search(SABHA_TRACER, RAMESH_MOBILE);

        assertThat(results).singleElement().satisfies(c -> {
            assertThat(c.personId()).isEqualTo(SEEDED_RAMESH);
            assertThat(c.fullName()).isEqualTo("Ramesh Shah");
            assertThat(c.homeSabha()).isEqualTo("REGULAR_BAAL");
        });
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
