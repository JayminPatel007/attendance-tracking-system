package org.sabha.container;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.analytics.applicationservice.CandidateRow;
import org.sabha.analytics.applicationservice.DashboardAccess;
import org.sabha.analytics.applicationservice.DashboardOverview;
import org.sabha.analytics.applicationservice.DashboardQueries;
import org.sabha.analytics.applicationservice.DashboardScope;
import org.sabha.analytics.applicationservice.ReEngagementProjectionScanner;
import org.sabha.analytics.applicationservice.SabhaTree;
import org.sabha.analytics.applicationservice.ThresholdAdmin;
import org.sabha.analytics.applicationservice.ThresholdConfig;
import org.sabha.analytics.domain.Thresholds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end for the re-engagement read-model (Slice 15, ADR-0010): the projection
 * scanner runs the Calculator State-wide, then the dashboard queries serve the caller
 * only its own scope. Two Yuvak Sabhas in different Kshetras of one Zone, each with
 * one 3-missed candidate, let us pin that the scope is applied through the projection —
 * a Sanchalak sees only their Sabha, the MK both — and that the Zone → Kshetra → Sabha
 * tree rolls the counts up. The per-tier predicate itself is covered canonically by
 * {@link CallerVisibilityIntegrationTest}; this test does not re-enumerate every tier.
 * Thresholds round-trip through the admin port.
 */
@SpringBootTest
@Import(ReEngagementDashboardIntegrationTest.NoAuthConfig.class)
@Testcontainers
@Transactional
class ReEngagementDashboardIntegrationTest {

    private static final UUID CITY = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
    private static final UUID ZONE = UUID.fromString("00000000-0000-0000-0000-0000000000d2");
    private static final UUID KSHETRA_1 = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final UUID KSHETRA_2 = UUID.fromString("00000000-0000-0000-0000-0000000000e2");
    private static final UUID SABHA_1 = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
    private static final UUID SABHA_2 = UUID.fromString("00000000-0000-0000-0000-0000000000f2");

    private static final UUID PERSON_1 = UUID.fromString("00000000-0000-0000-0000-000000000a01");
    private static final UUID PERSON_2 = UUID.fromString("00000000-0000-0000-0000-000000000a02");

    private static final UUID MK_USER = UUID.fromString("00000000-0000-0000-0000-000000000b01");
    private static final UUID SANCHALAK_USER = UUID.fromString("00000000-0000-0000-0000-000000000b02");
    private static final UUID SANT_USER = UUID.fromString("00000000-0000-0000-0000-000000000b04");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    JdbcClient jdbc;

    @Autowired
    ReEngagementProjectionScanner scanner;

    @Autowired
    DashboardQueries dashboard;

    @Autowired
    DashboardAccess access;

    @Autowired
    ThresholdConfig thresholdConfig;

    @Autowired
    ThresholdAdmin thresholdAdmin;

    @Test
    void theProjectionIsServedThroughTheCallerScopeAfterARefresh() {
        seedTwoSabhasEachWithACandidate();
        scanner.refresh();

        // A scoped caller is narrowed to their own Sabha; the unrestricted MK sees both —
        // enough to prove the scope predicate is applied to the refreshed projection.
        // Tier-by-tier scoping lives in CallerVisibilityIntegrationTest.
        assertThat(dashboard.people(new DashboardScope.RoleScoped(SANCHALAK_USER))).extracting(CandidateRow::personId)
                .containsExactly(PERSON_1);
        assertThat(dashboard.people(new DashboardScope.RoleScoped(MK_USER))).extracting(CandidateRow::personId)
                .contains(PERSON_1, PERSON_2);
    }

    @Test
    void aSantReadsAnyCityAcrossKshetrasAndTheirPickPersists() {
        seedTwoSabhasEachWithACandidate();
        santUser(SANT_USER, "sant-user");
        scanner.refresh();

        // Picking the City persists it as the default and scopes the view to it.
        assertThat(access.selectCity(SANT_USER, CITY)).isEqualTo(new DashboardScope.CityScoped(CITY));

        // Universal read: the Sant holds no role over either Kshetra, yet sees both
        // candidates — the rejection that limited the Nirdeshak to PERSON_1 does not
        // apply (ADR-0011 Sant exception).
        assertThat(dashboard.people(access.viewFor(SANT_USER))).extracting(CandidateRow::personId)
                .contains(PERSON_1, PERSON_2);

        // The default survives a fresh resolution (across logins).
        assertThat(access.viewFor(SANT_USER)).isEqualTo(new DashboardScope.CityScoped(CITY));
    }

    @Test
    void overviewKpisAreScopedAndTheTreeRollsCountsUp() {
        seedTwoSabhasEachWithACandidate();
        scanner.refresh();

        DashboardOverview sanchalakView = dashboard.overview(new DashboardScope.RoleScoped(SANCHALAK_USER));
        assertThat(sanchalakView.kpis()).isEqualTo(new DashboardOverview.Kpis(1, 0, 1));
        assertThat(sanchalakView.headlineCandidates()).extracting(CandidateRow::personId).containsExactly(PERSON_1);

        SabhaTree.Zone zone = dashboard.sabhaTree(new DashboardScope.RoleScoped(MK_USER)).zones().stream()
                .filter(z -> ZONE.equals(z.zoneId()))
                .findFirst()
                .orElseThrow();
        assertThat(zone.candidateCount()).isEqualTo(2);
        assertThat(zone.kshetras()).extracting(SabhaTree.Kshetra::kshetraId, SabhaTree.Kshetra::candidateCount)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(KSHETRA_1, 1),
                        org.assertj.core.groups.Tuple.tuple(KSHETRA_2, 1));
    }

    @Test
    void thresholdsRoundTripThroughTheAdminPort() {
        user(MK_USER, "mk-user"); // updated_by stamps a real User
        assertThat(thresholdConfig.current()).isEqualTo(new Thresholds(3, 6));

        thresholdAdmin.update(new Thresholds(2, 5), MK_USER);

        assertThat(thresholdConfig.current()).isEqualTo(new Thresholds(2, 5));
    }

    private void seedTwoSabhasEachWithACandidate() {
        person(PERSON_1, "+910000000a01");
        person(PERSON_2, "+910000000a02");
        mkUser(MK_USER, "mk-user");
        user(SANCHALAK_USER, "sanchalak-user");

        city(CITY);
        zone(ZONE, CITY);
        kshetra(KSHETRA_1, ZONE);
        kshetra(KSHETRA_2, ZONE);
        sabha(SABHA_1, KSHETRA_1);
        sabha(SABHA_2, KSHETRA_2);

        candidate(PERSON_1, SABHA_1);
        candidate(PERSON_2, SABHA_2);

        roleAssignment(SANCHALAK_USER, "SANCHALAK", "sabha_id", SABHA_1, null);
    }

    /** Three Finalized Occurrences with no marking for the Person => a 3-missed streak. */
    private void candidate(UUID personId, UUID sabhaId) {
        homeSabha(personId, sabhaId, LocalDate.of(2026, 1, 1));
        finalizedOccurrence(sabhaId, LocalDate.of(2026, 1, 7));
        finalizedOccurrence(sabhaId, LocalDate.of(2026, 1, 14));
        finalizedOccurrence(sabhaId, LocalDate.of(2026, 1, 21));
    }

    private void person(UUID id, String mobile) {
        jdbc.sql("INSERT INTO persons (id, full_name, gender, mobile) VALUES (?, 'Test Person', 'MALE', ?)")
                .params(id, mobile).update();
    }

    private void user(UUID id, String username) {
        jdbc.sql("""
                INSERT INTO persons (id, full_name, gender, mobile) VALUES (?, ?, 'MALE', ?)
                """).params(id, username + "-person", "+91" + username.hashCode()).update();
        jdbc.sql("""
                INSERT INTO users (id, person_id, username, keycloak_user_id) VALUES (?, ?, ?, ?)
                """).params(id, id, username, UUID.randomUUID()).update();
    }

    private void mkUser(UUID id, String username) {
        user(id, username);
        jdbc.sql("INSERT INTO role_assignments (id, user_id, role) VALUES (?, ?, 'MADHYASTHA_KARYALAYA')")
                .params(UUID.randomUUID(), id).update();
    }

    /** A Sant: a role_assignments row with role = 'SANT' and no operational scope. */
    private void santUser(UUID id, String username) {
        user(id, username);
        jdbc.sql("INSERT INTO role_assignments (id, user_id, role) VALUES (?, ?, 'SANT')")
                .params(UUID.randomUUID(), id).update();
    }

    private void city(UUID id) {
        jdbc.sql("INSERT INTO cities (id, name, created_by) VALUES (?, 'Test City', ?)")
                .params(id, MK_USER).update();
    }

    private void zone(UUID id, UUID cityId) {
        jdbc.sql("INSERT INTO zones (id, city_id, name, created_by) VALUES (?, ?, 'Test Zone', ?)")
                .params(id, cityId, MK_USER).update();
    }

    private void kshetra(UUID id, UUID zoneId) {
        jdbc.sql("INSERT INTO kshetras (id, name, zone_id, created_by) VALUES (?, 'Test Kshetra', ?, ?)")
                .params(id, zoneId, MK_USER).update();
    }

    private void sabha(UUID id, UUID kshetraId) {
        jdbc.sql("""
                INSERT INTO sabhas (id, kshetra_id, sabha_kind, schedule_shape, standing_venue)
                VALUES (?, ?, 'REGULAR_YUVAK', 'WEEKLY_RECURRING', 'Test Venue')
                """).params(id, kshetraId).update();
    }

    private void homeSabha(UUID personId, UUID sabhaId, LocalDate assignedAt) {
        jdbc.sql("INSERT INTO home_sabhas (person_id, sabha_id, assigned_at) VALUES (?, ?, ?)")
                .params(personId, sabhaId, assignedAt.atStartOfDay()).update();
    }

    private void finalizedOccurrence(UUID sabhaId, LocalDate date) {
        jdbc.sql("INSERT INTO occurrences (id, sabha_id, occurrence_date, state) VALUES (?, ?, ?, 'FINALIZED')")
                .params(UUID.randomUUID(), sabhaId, date).update();
    }

    private void roleAssignment(UUID userId, String role, String scopeColumn, UUID scopeId, String demographic) {
        jdbc.sql("INSERT INTO role_assignments (id, user_id, role, " + scopeColumn + ", demographic) VALUES (?, ?, ?, ?, ?)")
                .params(UUID.randomUUID(), userId, role, scopeId, demographic).update();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class NoAuthConfig {
        @Bean
        @Primary
        JwtDecoder testJwtDecoder() {
            return token -> {
                throw new UnsupportedOperationException("JWT decoding not exercised");
            };
        }
    }
}
