package org.sabha.container;

import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Role appointment end-to-end over the BFF (ADR-0011, ADR-0022). Proves the
 * one-transaction flow through real HTTP + Postgres + Keycloak: a Nirdeshak
 * appoints a Sanchalak on a Sabha within their (Kshetra, demographic) scope;
 * picking an existing User only writes the RoleAssignment; the inline-create path
 * writes Person + initial Home Sabha + User credentials + RoleAssignment together;
 * the Authorization Engine rejects out-of-scope appointers (403); and a username
 * collision is rejected before commit (409), leaving nothing behind.
 *
 * <p>The seed Sabha {@code …0002} is a {@code REGULAR_YUVAK} Sabha in Kshetra
 * {@code …0001} (Slice 2 seed), so its scope resolves to (Kshetra …0001, YUVAK).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoleAppointmentIntegrationTest extends KeycloakIntegrationTest {

    private static final UUID KSHETRA = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SABHA = UUID.fromString("00000000-0000-0000-0000-000000000002");

    /** Seeded Sanchalak (non-Nirdeshak) Keycloak subject — realm-sabha.json. */
    private static final String SANCHALAK_SUBJECT = "00000000-0000-0000-0000-000000000005";

    private static final UUID NIRDESHAK_SUBJECT = UUID.fromString("00000000-0000-0000-0000-00000011a001");
    private static final UUID NIRDESHAK_PERSON = UUID.fromString("00000000-0000-0000-0000-00000011a002");
    private static final UUID NIRDESHAK_USER = UUID.fromString("00000000-0000-0000-0000-00000011a003");

    private static final UUID EXISTING_PERSON = UUID.fromString("00000000-0000-0000-0000-00000011b001");
    private static final UUID EXISTING_USER = UUID.fromString("00000000-0000-0000-0000-00000011b002");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry r) {
        registerMkBootstrap(r);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcClient jdbc;

    @Transactional
    @Test
    void appointingAnExistingUserOnlyWritesTheRoleAssignmentWithItsAudit() throws Exception {
        seedNirdeshak();
        seedExistingUser();

        String body = "{\"role\":\"SANCHALAK\",\"sabhaId\":\"" + SABHA + "\","
                + "\"existingPersonId\":\"" + EXISTING_PERSON + "\"}";

        mockMvc.perform(authedPost(NIRDESHAK_SUBJECT.toString(), "/bff/appointments", body))
                .andExpect(status().isCreated());

        Integer rows = jdbc.sql("""
                SELECT COUNT(*) AS c FROM role_assignments
                WHERE user_id = ? AND role = 'SANCHALAK' AND sabha_id = ?
                  AND appointed_by = ? AND appointed_at IS NOT NULL
                """)
                .param(EXISTING_USER).param(SABHA).param(NIRDESHAK_USER)
                .query((rs, n) -> rs.getInt("c")).single();
        assertThat(rows).isEqualTo(1);
    }

    @Transactional
    @Test
    void appointingANewPersonWritesPersonHomeSabhaUserAndRoleAssignmentTogether() throws Exception {
        seedNirdeshak();
        String mobile = "+919820222001";

        String body = "{\"role\":\"SANCHALAK\",\"sabhaId\":\"" + SABHA + "\","
                + "\"newPerson\":{\"fullName\":\"Fresh Sanchalak\",\"gender\":\"MALE\","
                + "\"mobile\":\"" + mobile + "\",\"homeSabhaId\":\"" + SABHA + "\"},"
                + "\"username\":\"fresh-sanchalak\",\"rawPassword\":\"TempPass1!\"}";

        mockMvc.perform(authedPost(NIRDESHAK_SUBJECT.toString(), "/bff/appointments", body))
                .andExpect(status().isCreated());

        UUID personId = jdbc.sql("SELECT id FROM persons WHERE mobile = ?")
                .param(mobile).query((rs, n) -> rs.getObject("id", UUID.class)).single();

        Integer homeSabhas = jdbc.sql("SELECT COUNT(*) AS c FROM home_sabhas WHERE person_id = ? AND sabha_id = ?")
                .param(personId).param(SABHA).query((rs, n) -> rs.getInt("c")).single();
        assertThat(homeSabhas).isEqualTo(1);

        UUID userId = jdbc.sql("SELECT id FROM users WHERE person_id = ? AND username = 'fresh-sanchalak'")
                .param(personId).query((rs, n) -> rs.getObject("id", UUID.class)).single();
        UUID keycloakUserId = jdbc.sql("SELECT keycloak_user_id FROM users WHERE id = ?")
                .param(userId).query((rs, n) -> rs.getObject("keycloak_user_id", UUID.class)).single();
        assertThat(keycloakUserId).isNotNull();

        Integer roleRows = jdbc.sql("""
                SELECT COUNT(*) AS c FROM role_assignments
                WHERE user_id = ? AND role = 'SANCHALAK' AND sabha_id = ? AND appointed_by = ?
                """)
                .param(userId).param(SABHA).param(NIRDESHAK_USER)
                .query((rs, n) -> rs.getInt("c")).single();
        assertThat(roleRows).isEqualTo(1);
    }

    @Transactional
    @Test
    void appointingFromOutsideTheNirdeshaksScopeIsForbidden() throws Exception {
        // The seeded Sanchalak holds no Nirdeshak role, so cannot appoint anyone.
        seedExistingUser();
        String body = "{\"role\":\"SANCHALAK\",\"sabhaId\":\"" + SABHA + "\","
                + "\"existingPersonId\":\"" + EXISTING_PERSON + "\"}";

        mockMvc.perform(authedPost(SANCHALAK_SUBJECT, "/bff/appointments", body))
                .andExpect(status().isForbidden());
    }

    @Test
    void aUsernameCollisionIsRejectedWith409AndNothingIsPersisted() throws Exception {
        seedNirdeshak();
        String mobile = "+919820222004";

        // 'sanchalak' is taken by the Slice 2 seed user.
        String body = "{\"role\":\"SANCHALAK\",\"sabhaId\":\"" + SABHA + "\","
                + "\"newPerson\":{\"fullName\":\"Clashing Name\",\"gender\":\"MALE\","
                + "\"mobile\":\"" + mobile + "\",\"homeSabhaId\":\"" + SABHA + "\"},"
                + "\"username\":\"sanchalak\",\"rawPassword\":\"TempPass1!\"}";

        mockMvc.perform(authedPost(NIRDESHAK_SUBJECT.toString(), "/bff/appointments", body))
                .andExpect(status().isConflict());

        Integer persons = jdbc.sql("SELECT COUNT(*) AS c FROM persons WHERE mobile = ?")
                .param(mobile).query((rs, n) -> rs.getInt("c")).single();
        assertThat(persons).isZero();
    }

    @Transactional
    @Test
    void appointingAThirdSahNirdeshakForAKshetraDemographicIsRejectedWith409() throws Exception {
        seedNirdeshak();
        seedExistingUser();
        seedSahNirdeshaks(2);

        String body = "{\"role\":\"SAH_NIRDESHAK\",\"kshetraId\":\"" + KSHETRA + "\","
                + "\"demographic\":\"YUVAK\",\"existingPersonId\":\"" + EXISTING_PERSON + "\"}";

        mockMvc.perform(authedPost(NIRDESHAK_SUBJECT.toString(), "/bff/appointments", body))
                .andExpect(status().isConflict());

        // The third was rejected before commit: still exactly the two seeded.
        assertThat(sahNirdeshakCount()).isEqualTo(2);
    }

    @Transactional
    @Test
    void appointingASecondSahNirdeshakForAKshetraDemographicSucceeds() throws Exception {
        seedNirdeshak();
        seedExistingUser();
        seedSahNirdeshaks(1);

        String body = "{\"role\":\"SAH_NIRDESHAK\",\"kshetraId\":\"" + KSHETRA + "\","
                + "\"demographic\":\"YUVAK\",\"existingPersonId\":\"" + EXISTING_PERSON + "\"}";

        mockMvc.perform(authedPost(NIRDESHAK_SUBJECT.toString(), "/bff/appointments", body))
                .andExpect(status().isCreated());

        assertThat(sahNirdeshakCount()).isEqualTo(2);
    }

    @Transactional
    @Test
    void theSahNirdeshakCapEndpointReportsTheActiveCountAgainstTheCap() throws Exception {
        seedSahNirdeshaks(1);

        mockMvc.perform(get("/bff/appointments/sah-nirdeshak-cap")
                        .param("kshetraId", KSHETRA.toString())
                        .param("demographic", "YUVAK")
                        .with(oidcLogin().idToken(t -> t.subject(NIRDESHAK_SUBJECT.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(1))
                .andExpect(jsonPath("$.cap").value(2))
                .andExpect(jsonPath("$.reached").value(false));
    }

    // --- helpers -----------------------------------------------------------

    private int sahNirdeshakCount() {
        return jdbc.sql("""
                SELECT COUNT(*) FROM role_assignments
                WHERE role = 'SAH_NIRDESHAK' AND kshetra_id = ? AND demographic = 'YUVAK'
                """)
                .param(KSHETRA).query(Integer.class).single();
    }

    private void seedSahNirdeshaks(int count) {
        for (int i = 0; i < count; i++) {
            UUID id = UUID.fromString("00000000-0000-0000-0000-00000011c00" + i);
            jdbc.sql("""
                    INSERT INTO persons (id, full_name, gender, mobile)
                    VALUES (?, 'Seeded Sah-Nirdeshak', 'MALE', ?)
                    ON CONFLICT (id) DO NOTHING
                    """).param(id).param("+91982033300" + i).update();
            jdbc.sql("""
                    INSERT INTO users (id, person_id, username, keycloak_user_id)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT (id) DO NOTHING
                    """).param(id).param(id).param("seeded-sah-nirdeshak-" + i).param(UUID.randomUUID()).update();
            jdbc.sql("""
                    INSERT INTO role_assignments (id, user_id, role, kshetra_id, demographic)
                    VALUES (?, ?, 'SAH_NIRDESHAK', ?, 'YUVAK')
                    ON CONFLICT (id) DO NOTHING
                    """).param(id).param(id).param(KSHETRA).update();
        }
    }

    private void seedNirdeshak() {
        jdbc.sql("""
                INSERT INTO persons (id, full_name, gender, mobile)
                VALUES (?, 'Nirdeshak Tracer', 'MALE', '+919820100888')
                ON CONFLICT (id) DO NOTHING
                """).param(NIRDESHAK_PERSON).update();
        jdbc.sql("""
                INSERT INTO users (id, person_id, username, keycloak_user_id)
                VALUES (?, ?, 'nirdeshak-appoint', ?)
                ON CONFLICT (id) DO NOTHING
                """).param(NIRDESHAK_USER).param(NIRDESHAK_PERSON).param(NIRDESHAK_SUBJECT).update();
        jdbc.sql("""
                INSERT INTO role_assignments (id, user_id, role, kshetra_id, demographic)
                VALUES (?, ?, 'NIRDESHAK', ?, 'YUVAK')
                ON CONFLICT (id) DO NOTHING
                """).param(NIRDESHAK_USER).param(NIRDESHAK_USER).param(KSHETRA).update();
    }

    private void seedExistingUser() {
        jdbc.sql("""
                INSERT INTO persons (id, full_name, gender, mobile)
                VALUES (?, 'Existing Karyakar', 'MALE', '+919820100999')
                ON CONFLICT (id) DO NOTHING
                """).param(EXISTING_PERSON).update();
        jdbc.sql("""
                INSERT INTO users (id, person_id, username, keycloak_user_id)
                VALUES (?, ?, 'existing-karyakar', ?)
                ON CONFLICT (id) DO NOTHING
                """).param(EXISTING_USER).param(EXISTING_PERSON).param(UUID.randomUUID()).update();
    }

    private MockHttpServletRequestBuilder authedPost(String subject, String path, String json) throws Exception {
        Cookie xsrf = mockMvc.perform(get("/bff/me").with(oidcLogin().idToken(t -> t.subject(subject))))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
        return post(path)
                .with(oidcLogin().idToken(t -> t.subject(subject)))
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content(json);
    }
}
