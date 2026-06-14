package org.sabha.container;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.sabha.identity.applicationservice.IdentityProviderGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice 18 end-to-end (ADR-0004): proves the password-reset wiring through real
 * HTTP + Postgres + Keycloak. The self-service two-step flow is reachable
 * unauthenticated and actually changes the credential at the identity provider;
 * the "who appointed me?" lookup is reachable unauthenticated; and the
 * assigner-reissue BFF endpoint records an audit row for the appointing Karyakar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PasswordResetIntegrationTest {

    private static final UUID RESET_PERSON = UUID.fromString("00000000-0000-0000-0000-00000018a001");
    private static final UUID RESET_USER = UUID.fromString("00000000-0000-0000-0000-00000018a002");
    private static final String RESET_USERNAME = "reset-subject";
    private static final String RESET_MOBILE = "+919820181001";

    private static final UUID APPOINTER_PERSON = UUID.fromString("00000000-0000-0000-0000-00000018b001");
    private static final UUID APPOINTER_USER = UUID.fromString("00000000-0000-0000-0000-00000018b002");
    private static final UUID APPOINTER_SUBJECT = UUID.fromString("00000000-0000-0000-0000-00000018b003");
    private static final UUID TARGET_PERSON = UUID.fromString("00000000-0000-0000-0000-00000018c001");
    private static final UUID TARGET_USER = UUID.fromString("00000000-0000-0000-0000-00000018c002");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static KeycloakContainer keycloak = new KeycloakContainer().withRealmImportFile("/realm-sabha.json");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry r) {
        r.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloak.getAuthServerUrl() + "/realms/sabha");
        r.add("sabha.keycloak.issuer-uri", () -> keycloak.getAuthServerUrl() + "/realms/sabha");
        r.add("sabha.keycloak.admin-base-url", keycloak::getAuthServerUrl);
        r.add("sabha.keycloak.realm", () -> "sabha");
        r.add("sabha.keycloak.admin-username", keycloak::getAdminUsername);
        r.add("sabha.keycloak.admin-password", keycloak::getAdminPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    IdentityProviderGateway identityProvider;

    @AfterEach
    void cleanUp() {
        jdbc.sql("DELETE FROM password_resets").update();
        jdbc.sql("DELETE FROM role_assignments WHERE user_id IN (?, ?, ?)")
                .param(RESET_USER).param(APPOINTER_USER).param(TARGET_USER).update();
        jdbc.sql("DELETE FROM users WHERE id IN (?, ?, ?)")
                .param(RESET_USER).param(APPOINTER_USER).param(TARGET_USER).update();
        jdbc.sql("DELETE FROM persons WHERE id IN (?, ?, ?)")
                .param(RESET_PERSON).param(APPOINTER_PERSON).param(TARGET_PERSON).update();
    }

    @Test
    void selfServiceResetIsPublicAndChangesTheCredentialEndToEnd() throws Exception {
        UUID keycloakId = identityProvider.createUserRequiringPasswordChange(RESET_USERNAME, "InitPass1!");
        seedUser(RESET_PERSON, RESET_USER, RESET_USERNAME, RESET_MOBILE, keycloakId);

        // request — no Authorization header: the public chain must accept it.
        MvcResult requested = mockMvc.perform(post("/api/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + RESET_USERNAME + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        UUID resetId = UUID.fromString(json(requested, "resetId"));

        String otp = jdbc.sql("SELECT otp_code FROM password_resets WHERE id = ?")
                .param(resetId).query((rs, n) -> rs.getString("otp_code")).single();

        MvcResult verified = mockMvc.perform(post("/api/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resetId\":\"" + resetId + "\",\"otpCode\":\"" + otp + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String resetToken = json(verified, "resetToken");

        mockMvc.perform(post("/api/password-reset/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resetToken\":\"" + resetToken + "\",\"newPassword\":\"BrandNew1!\"}"))
                .andExpect(status().isOk());

        // The reset is recorded COMPLETED, and the new password authenticates.
        String status = jdbc.sql("SELECT status FROM password_resets WHERE id = ?")
                .param(resetId).query((rs, n) -> rs.getString("status")).single();
        assertThat(status).isEqualTo("COMPLETED");
        assertThat(canAuthenticate(RESET_USERNAME, "BrandNew1!")).isTrue();
    }

    @Test
    void requestForAnUnknownUsernameIs404OnThePublicChain() throws Exception {
        mockMvc.perform(post("/api/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"no-such-user\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void aSecondResetRequestWithinTheCooldownIs429ProblemJson() throws Exception {
        // The request step is Postgres-only (Keycloak is touched only at `complete`),
        // so seed a local User without provisioning a Keycloak account — that keeps
        // this test isolated from the end-to-end test that owns this username there.
        seedUser(RESET_PERSON, RESET_USER, RESET_USERNAME, RESET_MOBILE, UUID.randomUUID());

        mockMvc.perform(post("/api/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + RESET_USERNAME + "\"}"))
                .andExpect(status().isOk());

        // Immediately again, inside the 30s resend cooldown (OtpSendPolicy) → 429.
        mockMvc.perform(post("/api/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + RESET_USERNAME + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("Too Many Requests"))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void whoAppointedMeReturnsTheAppointerContactUnauthenticated() throws Exception {
        seedUser(APPOINTER_PERSON, APPOINTER_USER, "appointer-k", "+919820182001", UUID.randomUUID());
        jdbc.sql("UPDATE persons SET full_name = 'Appointer Karyakar' WHERE id = ?")
                .param(APPOINTER_PERSON).update();
        seedUser(TARGET_PERSON, TARGET_USER, "target-k", "+919820182002", UUID.randomUUID());
        jdbc.sql("""
                INSERT INTO role_assignments (id, user_id, role, appointed_by)
                VALUES (?, ?, 'SANCHALAK', ?)
                """).param(UUID.randomUUID()).param(TARGET_USER).param(APPOINTER_USER).update();

        mockMvc.perform(get("/api/who-appointed-me").param("username", "target-k"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contacts[0].name").value("Appointer Karyakar"))
                .andExpect(jsonPath("$.contacts[0].mobile").value("+919820182001"));
    }

    @Test
    void assignerReissueOverBffRecordsTheAuditForTheAppointer() throws Exception {
        seedUser(APPOINTER_PERSON, APPOINTER_USER, "appointer-r", "+919820183001", APPOINTER_SUBJECT);
        UUID targetKeycloakId = identityProvider.createUserRequiringPasswordChange("target-r", "InitPass1!");
        seedUser(TARGET_PERSON, TARGET_USER, "target-r", "+919820183002", targetKeycloakId);
        jdbc.sql("""
                INSERT INTO role_assignments (id, user_id, role, appointed_by)
                VALUES (?, ?, 'SANCHALAK', ?)
                """).param(UUID.randomUUID()).param(TARGET_USER).param(APPOINTER_USER).update();

        String body = "{\"targetUserId\":\"" + TARGET_USER + "\",\"newPassword\":\"Reissued1!\"}";
        mockMvc.perform(authedPost(APPOINTER_SUBJECT.toString(), "/bff/password-reissue", body))
                .andExpect(status().isNoContent());

        Integer rows = jdbc.sql("""
                SELECT COUNT(*) AS c FROM password_resets
                WHERE method = 'ASSIGNER_REISSUE' AND target_user_id = ?
                  AND actor_user_id = ? AND completed_at IS NOT NULL
                """)
                .param(TARGET_USER).param(APPOINTER_USER).query((rs, n) -> rs.getInt("c")).single();
        assertThat(rows).isEqualTo(1);
    }

    // --- helpers -----------------------------------------------------------

    private void seedUser(UUID personId, UUID userId, String username, String mobile, UUID keycloakId) {
        jdbc.sql("""
                INSERT INTO persons (id, full_name, gender, mobile)
                VALUES (?, 'Reset Subject', 'MALE', ?)
                ON CONFLICT (id) DO NOTHING
                """).param(personId).param(mobile).update();
        jdbc.sql("""
                INSERT INTO users (id, person_id, username, keycloak_user_id)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """).param(userId).param(personId).param(username).param(keycloakId).update();
    }

    private String json(MvcResult result, String field) throws Exception {
        return new ObjectMapper().readTree(result.getResponse().getContentAsString()).get(field).asText();
    }

    private boolean canAuthenticate(String username, String password) throws Exception {
        String body = "grant_type=password&client_id=sabha-test"
                + "&username=" + username
                + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8)
                + "&scope=openid";
        HttpResponse<String> resp = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create(keycloak.getAuthServerUrl() + "/realms/sabha/protocol/openid-connect/token"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        return resp.statusCode() == 200;
    }

    private MockHttpServletRequestBuilder authedPost(String subject, String path, String json) throws Exception {
        Cookie xsrf = mockMvc.perform(get("/bff/me").with(oidcLogin().idToken(t -> t.subject(subject))))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
        return post(path)
                .with(oidcLogin().idToken(t -> t.subject(subject)))
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
    }
}
