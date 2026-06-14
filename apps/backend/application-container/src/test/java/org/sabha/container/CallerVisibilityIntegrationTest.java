package org.sabha.container;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sabha.common.CallerVisibility;
import org.sabha.common.Role;
import org.sabha.common.VisibilityTier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The one canonical caller-visibility predicate (issue #66) exercised per role tier
 * against the real schema. A single Zone holds two Kshetras; one Kshetra carries a
 * YUVAK and a BALIKA Sabha so demographic matching is observable, the other a YUVAK
 * Sabha so geographic widening is observable. Each tier is granted in isolation and
 * we assert exactly which Sabhas its predicate selects — the assertions the dashboard
 * and Occurrence-reopen adapter tests used to each restate.
 */
@SpringBootTest
@Import(CallerVisibilityIntegrationTest.NoAuthConfig.class)
@Transactional
class CallerVisibilityIntegrationTest extends PostgresIntegrationTest {

    private static final UUID CITY = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID ZONE = UUID.fromString("00000000-0000-0000-0000-0000000000c2");
    private static final UUID KSHETRA_1 = UUID.fromString("00000000-0000-0000-0000-0000000000c3");
    private static final UUID KSHETRA_2 = UUID.fromString("00000000-0000-0000-0000-0000000000c4");
    private static final UUID SABHA_1_YUVAK = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
    private static final UUID SABHA_1_BALIKA = UUID.fromString("00000000-0000-0000-0000-0000000000d2");
    private static final UUID SABHA_2_YUVAK = UUID.fromString("00000000-0000-0000-0000-0000000000d3");

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void seedHierarchy() {
        UUID creator = user();
        city(CITY, creator);
        zone(ZONE, CITY, creator);
        kshetra(KSHETRA_1, ZONE);
        kshetra(KSHETRA_2, ZONE);
        sabha(SABHA_1_YUVAK, KSHETRA_1, "REGULAR_YUVAK");
        sabha(SABHA_1_BALIKA, KSHETRA_1, "REGULAR_BALIKA");
        sabha(SABHA_2_YUVAK, KSHETRA_2, "REGULAR_YUVAK");
    }

    @Test
    void sanchalakSeesOnlyTheirOwnSabha() {
        UUID sanchalak = user();
        sabhaScopedRole(sanchalak, "SANCHALAK", SABHA_1_YUVAK);

        assertThat(visibleSabhas(sanchalak, EnumSet.of(VisibilityTier.SANCHALAK)))
                .containsExactly(SABHA_1_YUVAK);
    }

    @Test
    void nirdeshakSeesTheirKshetraTimesDemographicNotOtherDemographicsOrKshetras() {
        UUID nirdeshak = user();
        kshetraScopedRole(nirdeshak, "NIRDESHAK", KSHETRA_1, "YUVAK");

        assertThat(visibleSabhas(nirdeshak, EnumSet.of(VisibilityTier.NIRDESHAK)))
                .containsExactly(SABHA_1_YUVAK);
    }

    @Test
    void sanyojakSeesTheirZoneTimesDemographicAcrossKshetras() {
        UUID sanyojak = user();
        zoneScopedRole(sanyojak, "SANYOJAK", ZONE, "YUVAK");

        assertThat(visibleSabhas(sanyojak, EnumSet.of(VisibilityTier.SANYOJAK)))
                .containsExactlyInAnyOrder(SABHA_1_YUVAK, SABHA_2_YUVAK);
    }

    @Test
    void regionalTeamSeesTheirCityTimesDemographic() {
        UUID rt = user();
        cityScopedRole(rt, "REGIONAL_TEAM", CITY, "YUVAK");

        assertThat(visibleSabhas(rt, EnumSet.of(VisibilityTier.REGIONAL_TEAM)))
                .containsExactlyInAnyOrder(SABHA_1_YUVAK, SABHA_2_YUVAK);
    }

    @Test
    void madhyasthaKaryalayaSeesEverythingUnrestricted() {
        UUID mk = user();
        unscopedRole(mk, "MADHYASTHA_KARYALAYA");

        // Unrestricted: sees every demographic (incl. BALIKA, which no YUVAK-scoped tier
        // sees) across both Kshetras. Asserted with contains, not exactly, because the MK
        // also sees any migration-seeded Sabhas in the container.
        assertThat(visibleSabhas(mk, EnumSet.of(VisibilityTier.MADHYASTHA_KARYALAYA)))
                .contains(SABHA_1_YUVAK, SABHA_1_BALIKA, SABHA_2_YUVAK);
    }

    @Test
    void nirikshakReopenTierSeesTheirKshetraTimesDemographicViaRoleAssignment() {
        UUID nirikshak = user();
        kshetraScopedRole(nirikshak, "NIRIKSHAK", KSHETRA_1, "YUVAK");

        assertThat(visibleSabhas(nirikshak, EnumSet.of(VisibilityTier.NIRIKSHAK)))
                .containsExactly(SABHA_1_YUVAK);
    }

    @Test
    void nirikshakProxyTierSeesOnlyTheirExplicitlyAssignedSabhas() {
        UUID nirikshak = user();
        UUID assigner = user();
        nirikshakAssignment(nirikshak, SABHA_2_YUVAK, assigner);

        assertThat(visibleSabhas(nirikshak, EnumSet.of(VisibilityTier.NIRIKSHAK_PROXY)))
                .containsExactly(SABHA_2_YUVAK);
    }

    @Test
    void grantingMultipleTiersUnionsTheirScopes() {
        UUID user = user();
        sabhaScopedRole(user, "SANCHALAK", SABHA_1_YUVAK);
        nirikshakAssignment(user, SABHA_2_YUVAK, user());

        assertThat(visibleSabhas(user, EnumSet.of(VisibilityTier.SANCHALAK, VisibilityTier.NIRIKSHAK_PROXY)))
                .containsExactlyInAnyOrder(SABHA_1_YUVAK, SABHA_2_YUVAK);
    }

    @Test
    void reopenAuthoritiesMapFromReopenRolesToVisibilityTiers() {
        // Role.REOPEN_TIERS -> the role_assignments Kshetra-tier predicate the reopen
        // read model composes; NIRIKSHAK here resolves via role_assignments, not proxy.
        UUID nirikshak = user();
        kshetraScopedRole(nirikshak, "NIRIKSHAK", KSHETRA_1, "YUVAK");

        assertThat(visibleSabhas(nirikshak, CallerVisibility.tiersFor(Role.REOPEN_TIERS)))
                .containsExactly(SABHA_1_YUVAK);
    }

    private Set<UUID> visibleSabhas(UUID userId, Set<VisibilityTier> tiers) {
        String predicate = CallerVisibility.predicate(
                new CallerVisibility.Aliases("s", "k", "z"), tiers);
        return Set.copyOf(jdbc.sql("""
                        SELECT s.id
                        FROM sabhas s
                        JOIN kshetras k ON k.id = s.kshetra_id
                        LEFT JOIN zones z ON z.id = k.zone_id
                        WHERE %s
                        """.formatted(predicate))
                .param(CallerVisibility.USER_PARAM, userId)
                .query(UUID.class)
                .list());
    }

    private UUID user() {
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO persons (id, full_name, gender, mobile) VALUES (?, 'Test', 'MALE', ?)")
                .params(id, "+91" + Math.abs(id.hashCode())).update();
        jdbc.sql("INSERT INTO users (id, person_id, username, keycloak_user_id) VALUES (?, ?, ?, ?)")
                .params(id, id, id.toString(), UUID.randomUUID()).update();
        return id;
    }

    private void sabhaScopedRole(UUID userId, String role, UUID sabhaId) {
        jdbc.sql("INSERT INTO role_assignments (id, user_id, role, sabha_id) VALUES (?, ?, ?, ?)")
                .params(UUID.randomUUID(), userId, role, sabhaId).update();
    }

    private void kshetraScopedRole(UUID userId, String role, UUID kshetraId, String demographic) {
        jdbc.sql("INSERT INTO role_assignments (id, user_id, role, kshetra_id, demographic) VALUES (?, ?, ?, ?, ?)")
                .params(UUID.randomUUID(), userId, role, kshetraId, demographic).update();
    }

    private void zoneScopedRole(UUID userId, String role, UUID zoneId, String demographic) {
        jdbc.sql("INSERT INTO role_assignments (id, user_id, role, zone_id, demographic) VALUES (?, ?, ?, ?, ?)")
                .params(UUID.randomUUID(), userId, role, zoneId, demographic).update();
    }

    private void cityScopedRole(UUID userId, String role, UUID cityId, String demographic) {
        jdbc.sql("INSERT INTO role_assignments (id, user_id, role, city_id, demographic) VALUES (?, ?, ?, ?, ?)")
                .params(UUID.randomUUID(), userId, role, cityId, demographic).update();
    }

    private void unscopedRole(UUID userId, String role) {
        jdbc.sql("INSERT INTO role_assignments (id, user_id, role) VALUES (?, ?, ?)")
                .params(UUID.randomUUID(), userId, role).update();
    }

    private void nirikshakAssignment(UUID nirikshakUserId, UUID sabhaId, UUID assignedBy) {
        jdbc.sql("""
                INSERT INTO nirikshak_sabha_assignments (id, nirikshak_user_id, sabha_id, assigned_by)
                VALUES (?, ?, ?, ?)
                """).params(UUID.randomUUID(), nirikshakUserId, sabhaId, assignedBy).update();
    }

    private void city(UUID id, UUID createdBy) {
        jdbc.sql("INSERT INTO cities (id, name, created_by) VALUES (?, 'Test City', ?)").params(id, createdBy).update();
    }

    private void zone(UUID id, UUID cityId, UUID createdBy) {
        jdbc.sql("INSERT INTO zones (id, city_id, name, created_by) VALUES (?, ?, 'Test Zone', ?)")
                .params(id, cityId, createdBy).update();
    }

    private void kshetra(UUID id, UUID zoneId) {
        jdbc.sql("INSERT INTO kshetras (id, name, zone_id, created_by) VALUES (?, 'Test Kshetra', ?, NULL)")
                .params(id, zoneId).update();
    }

    private void sabha(UUID id, UUID kshetraId, String kind) {
        jdbc.sql("""
                INSERT INTO sabhas (id, kshetra_id, sabha_kind, schedule_shape, standing_venue)
                VALUES (?, ?, ?, 'WEEKLY_RECURRING', 'Test Venue')
                """).params(id, kshetraId, kind).update();
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
