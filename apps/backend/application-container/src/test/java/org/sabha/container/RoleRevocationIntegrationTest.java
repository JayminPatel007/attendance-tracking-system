package org.sabha.container;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.sabha.identity.applicationservice.IdentityProviderGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Role revocation end-to-end over the BFF (ADR-0026, Issue #89). Proves through
 * real HTTP + Postgres + Keycloak that revocation is a state change, not a delete:
 * the current holder of the appointing scope stamps {@code revoked_by/at} on the
 * row (which — with its {@code appointed_by} audit — survives), an out-of-scope
 * actor is refused, revoking an upper tier does not cascade to its appointees, the
 * Regional Team's last member cannot be revoked, and a User who loses their last
 * active role can no longer authenticate. The class is {@code @Transactional}, so
 * every seeded row rolls back; the one login-loss test provisions a throwaway
 * Keycloak user so the non-transactional disable never touches shared identities.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoleRevocationIntegrationTest extends KeycloakIntegrationTest {

    /** Seeded Sanchalak (non-authority) Keycloak subject — infra/keycloak/realm-sabha.json. */
    private static final String SANCHALAK_SUBJECT = "00000000-0000-0000-0000-000000000005";

    private static final String DEMOGRAPHIC = "YUVAK";

    /** Hands each seeded Person a unique mobile (the column is system-wide UNIQUE). */
    private static final AtomicInteger MOBILE_SEQ = new AtomicInteger();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry r) {
        registerMkBootstrap(r);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    IdentityProviderGateway identityProvider;

    @Test
    void aScopeHolderRevokesAndTheRowIsStampedNotDeleted() throws Exception {
        UUID kshetraId = seedKshetraRow();
        UUID sabhaId = seedSabha(kshetraId, "REGULAR_" + DEMOGRAPHIC);
        Authority appointer = seedUser("rev-original-appointer");
        Authority sanchalak = seedUser("rev-sanchalak");
        UUID assignmentId = seedSanchalak(sabhaId, sanchalak.userId(), appointer.userId());
        seedSpareRole(sanchalak.userId()); // keeps login: not their last active role
        // Authority is by CURRENT scope, not who appointed: a different Nirdeshak revokes.
        Authority nirdeshak = seedNirdeshak(kshetraId, DEMOGRAPHIC, "rev-nird");

        mockMvc.perform(authedPost(nirdeshak.subject().toString(), revokePath(assignmentId)))
                .andExpect(status().isNoContent());

        assertThat(exists(assignmentId)).isTrue();
        assertThat(isRevoked(assignmentId)).isTrue();
        assertThat(revokedBy(assignmentId)).isEqualTo(nirdeshak.userId());
        assertThat(appointedBy(assignmentId)).isEqualTo(appointer.userId());
    }

    @Test
    void anOutOfScopeActorIsForbiddenAndNothingIsRevoked() throws Exception {
        UUID kshetraId = seedKshetraRow();
        UUID sabhaId = seedSabha(kshetraId, "REGULAR_" + DEMOGRAPHIC);
        Authority sanchalak = seedUser("rev-forbidden-sanchalak");
        UUID assignmentId = seedSanchalak(sabhaId, sanchalak.userId(), sanchalak.userId());

        mockMvc.perform(authedPost(SANCHALAK_SUBJECT, revokePath(assignmentId)))
                .andExpect(status().isForbidden());

        assertThat(isRevoked(assignmentId)).isFalse();
    }

    @Test
    void revokingAnUnknownAssignmentIs404() throws Exception {
        Authority nirdeshak = seedNirdeshak(seedKshetraRow(), DEMOGRAPHIC, "rev-404");

        mockMvc.perform(authedPost(nirdeshak.subject().toString(), revokePath(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void revokingTheLastRegionalTeamMemberIsRejectedAndTheMemberSurvives() throws Exception {
        String mk = keycloakSubject(MK_USERNAME);
        UUID cityId = seedCity(mk, "RevoLast City");
        Authority soleMember = seedUser("rev-rt-sole");
        UUID assignmentId = seedRegionalTeam(cityId, soleMember.userId());

        mockMvc.perform(authedPost(mk, revokePath(assignmentId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("LAST_REGIONAL_TEAM_MEMBER"));

        assertThat(isRevoked(assignmentId)).isFalse();
    }

    @Test
    void aNonLastRegionalTeamMemberIsRevoked() throws Exception {
        String mk = keycloakSubject(MK_USERNAME);
        UUID cityId = seedCity(mk, "RevoNonLast City");
        Authority first = seedUser("rev-rt-first");
        Authority second = seedUser("rev-rt-second");
        UUID firstAssignment = seedRegionalTeam(cityId, first.userId());
        seedRegionalTeam(cityId, second.userId());
        seedSpareRole(first.userId()); // keeps login when their RT role goes

        mockMvc.perform(authedPost(mk, revokePath(firstAssignment)))
                .andExpect(status().isNoContent());

        assertThat(isRevoked(firstAssignment)).isTrue();
    }

    @Test
    void revokingAnUpperTierHolderDoesNotCascadeToItsAppointees() throws Exception {
        UUID cityId = seedCity(keycloakSubject(MK_USERNAME), "NoCascade City");
        UUID zoneId = seedZone(cityId, "NoCascade Zone");
        UUID kshetraId = seedKshetraUnderZone(zoneId, "NoCascade Kshetra");
        UUID sabhaId = seedSabha(kshetraId, "REGULAR_" + DEMOGRAPHIC);

        Authority nirdeshak = seedNirdeshak(kshetraId, DEMOGRAPHIC, "rev-nocascade-nird");
        seedSpareRole(nirdeshak.userId()); // keeps login when the Nirdeshak role goes
        UUID nirdeshakAssignment = onlyAssignment(nirdeshak.userId(), "NIRDESHAK");
        Authority sanchalak = seedUser("rev-nocascade-sanchalak");
        UUID appointeeAssignment = seedSanchalak(sabhaId, sanchalak.userId(), nirdeshak.userId());

        // The tier above the Nirdeshak (the Zone's Sanyojak) revokes.
        Authority sanyojak = seedSanyojak(zoneId, DEMOGRAPHIC, "rev-nocascade-sany");

        mockMvc.perform(authedPost(sanyojak.subject().toString(), revokePath(nirdeshakAssignment)))
                .andExpect(status().isNoContent());

        assertThat(isRevoked(nirdeshakAssignment)).isTrue();
        // The Sanchalak the Nirdeshak appointed is untouched — survives for a successor.
        assertThat(exists(appointeeAssignment)).isTrue();
        assertThat(isRevoked(appointeeAssignment)).isFalse();
    }

    @Test
    void aUserLosingTheirLastActiveRoleCanNoLongerAuthenticate() throws Exception {
        UUID kshetraId = seedKshetraRow();
        UUID sabhaId = seedSabha(kshetraId, "REGULAR_" + DEMOGRAPHIC);

        // A real, log-in-able Keycloak user (non-temporary password) holding one role.
        String username = "rev-login-loss";
        UUID keycloakId = identityProvider.createUserRequiringPasswordChange(username, "InitPass1!");
        identityProvider.resetPassword(keycloakId, "Permanent1!", false);
        Authority target = seedUserWithKeycloak(username, keycloakId);
        UUID assignmentId = seedSanchalak(sabhaId, target.userId(), target.userId());
        assertThat(canAuthenticate(username, "Permanent1!")).isTrue();

        Authority nirdeshak = seedNirdeshak(kshetraId, DEMOGRAPHIC, "rev-login-nird");

        mockMvc.perform(authedPost(nirdeshak.subject().toString(), revokePath(assignmentId)))
                .andExpect(status().isNoContent());

        assertThat(canAuthenticate(username, "Permanent1!")).isFalse();
    }

    // --- seeding -----------------------------------------------------------

    private UUID seedKshetraRow() {
        UUID kshetraId = UUID.randomUUID();
        jdbc.sql("INSERT INTO kshetras (id, name) VALUES (?, 'Revoke Kshetra')").param(kshetraId).update();
        return kshetraId;
    }

    private UUID seedKshetraUnderZone(UUID zoneId, String name) {
        UUID kshetraId = UUID.randomUUID();
        jdbc.sql("INSERT INTO kshetras (id, name, zone_id) VALUES (?, ?, ?)")
                .param(kshetraId).param(name).param(zoneId).update();
        return kshetraId;
    }

    private UUID seedCity(String creatorSubject, String name) {
        UUID cityId = UUID.randomUUID();
        jdbc.sql("INSERT INTO cities (id, name, created_by) VALUES (?, ?, ?)")
                .param(cityId).param(name).param(userIdOfSubject(creatorSubject)).update();
        return cityId;
    }

    private UUID seedZone(UUID cityId, String name) {
        UUID zoneId = UUID.randomUUID();
        jdbc.sql("INSERT INTO zones (id, city_id, name, created_by) VALUES (?, ?, ?, ?)")
                .param(zoneId).param(cityId).param(name).param(seedUser("zone-creator").userId()).update();
        return zoneId;
    }

    private UUID seedSabha(UUID kshetraId, String sabhaKind) {
        UUID sabhaId = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO sabhas (id, kshetra_id, sabha_kind, schedule_shape, standing_venue)
                VALUES (?, ?, ?, 'MONTHLY_AD_HOC', 'Hall')
                """).param(sabhaId).param(kshetraId).param(sabhaKind).update();
        return sabhaId;
    }

    private UUID seedSanchalak(UUID sabhaId, UUID userId, UUID appointedBy) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO role_assignments (id, user_id, role, sabha_id, appointed_by)
                VALUES (?, ?, 'SANCHALAK', ?, ?)
                """).param(id).param(userId).param(sabhaId).param(appointedBy).update();
        return id;
    }

    private Authority seedNirdeshak(UUID kshetraId, String demographic, String tag) {
        Authority a = seedUser("nirdeshak-" + tag);
        jdbc.sql("INSERT INTO role_assignments (id, user_id, role, kshetra_id, demographic) VALUES (?, ?, 'NIRDESHAK', ?, ?)")
                .param(UUID.randomUUID()).param(a.userId()).param(kshetraId).param(demographic).update();
        return a;
    }

    private Authority seedSanyojak(UUID zoneId, String demographic, String tag) {
        Authority a = seedUser("sanyojak-" + tag);
        jdbc.sql("INSERT INTO role_assignments (id, user_id, role, zone_id, demographic) VALUES (?, ?, 'SANYOJAK', ?, ?)")
                .param(UUID.randomUUID()).param(a.userId()).param(zoneId).param(demographic).update();
        return a;
    }

    private UUID seedRegionalTeam(UUID cityId, UUID userId) {
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO role_assignments (id, user_id, role, city_id, demographic) VALUES (?, ?, 'REGIONAL_TEAM', ?, ?)")
                .param(id).param(userId).param(cityId).param(DEMOGRAPHIC).update();
        return id;
    }

    /** A throwaway extra active role so revoking the role under test isn't the User's last. */
    private void seedSpareRole(UUID userId) {
        jdbc.sql("INSERT INTO role_assignments (id, user_id, role, kshetra_id, demographic) VALUES (?, ?, 'NIRIKSHAK', ?, 'BAAL')")
                .param(UUID.randomUUID()).param(userId).param(seedKshetraRow()).update();
    }

    private Authority seedUser(String username) {
        return seedUserWithKeycloak(username, UUID.randomUUID());
    }

    private Authority seedUserWithKeycloak(String username, UUID keycloakSubject) {
        UUID person = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        jdbc.sql("INSERT INTO persons (id, full_name, gender, mobile) VALUES (?, 'Revoke Authority', 'MALE', ?)")
                .param(person).param(String.format("+9198203%05d", MOBILE_SEQ.getAndIncrement())).update();
        jdbc.sql("INSERT INTO users (id, person_id, username, keycloak_user_id) VALUES (?, ?, ?, ?)")
                .param(user).param(person).param(username).param(keycloakSubject).update();
        return new Authority(keycloakSubject, user);
    }

    private record Authority(UUID subject, UUID userId) {
    }

    // --- reads -------------------------------------------------------------

    private static String revokePath(UUID assignmentId) {
        return "/bff/appointments/" + assignmentId + "/revoke";
    }

    private boolean exists(UUID assignmentId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM role_assignments WHERE id = ?)")
                .param(assignmentId).query(Boolean.class).single();
    }

    private UUID revokedBy(UUID assignmentId) {
        return jdbc.sql("SELECT revoked_by FROM role_assignments WHERE id = ?")
                .param(assignmentId).query((rs, n) -> rs.getObject("revoked_by", UUID.class)).single();
    }

    private boolean isRevoked(UUID assignmentId) {
        return jdbc.sql("SELECT revoked_at IS NOT NULL FROM role_assignments WHERE id = ?")
                .param(assignmentId).query(Boolean.class).single();
    }

    private UUID appointedBy(UUID assignmentId) {
        return jdbc.sql("SELECT appointed_by FROM role_assignments WHERE id = ?")
                .param(assignmentId).query((rs, n) -> rs.getObject("appointed_by", UUID.class)).single();
    }

    private UUID onlyAssignment(UUID userId, String role) {
        return jdbc.sql("SELECT id FROM role_assignments WHERE user_id = ? AND role = ?")
                .param(userId).param(role).query((rs, n) -> rs.getObject("id", UUID.class)).single();
    }

    private UUID userIdOfSubject(String subject) {
        return jdbc.sql("SELECT id FROM users WHERE keycloak_user_id = ?")
                .param(UUID.fromString(subject)).query((rs, n) -> rs.getObject("id", UUID.class)).single();
    }

    private String keycloakSubject(String username) {
        return jdbc.sql("SELECT keycloak_user_id FROM users WHERE username = ?")
                .param(username).query((rs, n) -> rs.getObject("keycloak_user_id", UUID.class)).single().toString();
    }

    private boolean canAuthenticate(String username, String password) throws Exception {
        String body = "grant_type=password&client_id=sabha-test"
                + "&username=" + username
                + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8)
                + "&scope=openid";
        HttpResponse<String> resp = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create(KEYCLOAK.getAuthServerUrl() + "/realms/sabha/protocol/openid-connect/token"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        return resp.statusCode() == 200;
    }

    private MockHttpServletRequestBuilder authedPost(String subject, String path) throws Exception {
        Cookie xsrf = mockMvc.perform(get("/bff/me").with(oidcLogin().idToken(t -> t.subject(subject))))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
        return post(path)
                .with(oidcLogin().idToken(t -> t.subject(subject)))
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType(MediaType.APPLICATION_JSON);
    }
}
