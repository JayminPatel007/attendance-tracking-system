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
 * Sabha definition end-to-end over the BFF (ADR-0012, ADR-0022). Proves the
 * single-transaction act through real HTTP + Postgres: a Nirdeshak over a
 * (Kshetra, demographic) defines a weekly Sabha and its Sanchalak is appointed in
 * the same transaction; the Authorization Engine rejects a caller outside that
 * scope and leaves nothing behind. Each test seeds fresh identities, since these
 * BFF writes commit (no rollback between tests in the shared context).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SabhaDefinitionIntegrationTest extends KeycloakIntegrationTest {

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry r) {
        registerMkBootstrap(r);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcClient jdbc;

    @Test
    void nirdeshakDefinesAWeeklySabhaAndItsSanchalakIsAppointedInOneTransaction() throws Exception {
        Fixture f = seedStructureAndNirdeshak("YUVAK", "REGULAR");

        String body = """
                {"kshetraId":"%s","sabhaKindId":"%s","weekly":true,
                 "dayOfWeek":"SUNDAY","startTime":"09:00","endTime":"10:30",
                 "standingVenue":"Goregaon Mandir",
                 "sanchalak":{"existingPersonId":"%s","username":"sanchalak-%s","rawPassword":"Temp#1234"}}
                """.formatted(f.kshetraId, f.kindId, f.sanchalakPersonId, shortId());

        String response = mockMvc.perform(authedPost(f.nirdeshakSubject.toString(), "/bff/sabhas", body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID sabhaId = UUID.fromString(response.replaceAll(".*\"sabhaId\":\"([^\"]+)\".*", "$1"));

        var sabha = jdbc.sql("""
                SELECT schedule_shape, sabha_kind, sabha_kind_id, created_by, day_of_week
                FROM sabhas WHERE id = ?
                """).param(sabhaId).query((rs, n) -> new Object[] {
                    rs.getString("schedule_shape"), rs.getString("sabha_kind"),
                    rs.getObject("sabha_kind_id", UUID.class), rs.getObject("created_by", UUID.class),
                    rs.getShort("day_of_week")
                }).single();
        assertThat(sabha[0]).isEqualTo("WEEKLY_RECURRING");
        assertThat(sabha[1]).isEqualTo("REGULAR_YUVAK");
        assertThat(sabha[2]).isEqualTo(f.kindId);
        assertThat(sabha[3]).isEqualTo(f.nirdeshakUser);
        assertThat(sabha[4]).isEqualTo((short) 0);

        Long sanchalakAssignments = jdbc.sql("""
                SELECT count(*) FROM role_assignments
                WHERE user_id = ? AND role = 'SANCHALAK' AND sabha_id = ?
                """).param(f.sanchalakUser).param(sabhaId).query(Long.class).single();
        assertThat(sanchalakAssignments).isEqualTo(1L);
    }

    @Test
    void aCallerOutsideTheirNirdeshakScopeIsForbiddenAndNoSabhaIsPersisted() throws Exception {
        Fixture f = seedStructureAndNirdeshak("YUVATI", "YSS");
        UUID outsiderSubject = seedUserWithoutRole("outsider");

        String body = """
                {"kshetraId":"%s","sabhaKindId":"%s","weekly":true,
                 "dayOfWeek":"SUNDAY","startTime":"09:00","endTime":"10:30",
                 "standingVenue":"Goregaon Mandir",
                 "sanchalak":{"existingPersonId":"%s","username":"sanchalak-%s","rawPassword":"Temp#1234"}}
                """.formatted(f.kshetraId, f.kindId, f.sanchalakPersonId, shortId());

        mockMvc.perform(authedPost(outsiderSubject.toString(), "/bff/sabhas", body))
                .andExpect(status().isForbidden());

        Long sabhas = jdbc.sql("SELECT count(*) FROM sabhas WHERE sabha_kind_id = ?")
                .param(f.kindId).query(Long.class).single();
        assertThat(sabhas).isEqualTo(0L);
    }

    @Test
    void retiringAKindBlocksNewSabhasWhileExistingOnesDrainAndReactivateReopensCreation() throws Exception {
        Fixture f = seedStructureAndNirdeshak("KISHORE", "REGULAR");

        // Existing usage: a Sabha defined while the kind is active.
        UUID firstSabha = defineWeekly(f, "Drain Hall A");

        // The MK soft-retires the kind.
        jdbc.sql("UPDATE sabha_kinds SET retired_at = now(), retired_by = ? WHERE id = ?")
                .param(bootstrapMk()).param(f.kindId).update();

        // New creation of that kind is rejected with the stable 409 contract...
        mockMvc.perform(authedPost(f.nirdeshakSubject.toString(), "/bff/sabhas", defineBody(f, "Drain Hall B")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SABHA_KIND_RETIRED"));

        // ...while the existing Sabha and its Sanchalak role keep functioning (drain).
        Long sabhasOfKind = jdbc.sql("SELECT count(*) FROM sabhas WHERE sabha_kind_id = ?")
                .param(f.kindId).query(Long.class).single();
        assertThat(sabhasOfKind).isEqualTo(1L);
        Long sanchalakStillAttached = jdbc.sql("""
                SELECT count(*) FROM role_assignments
                WHERE role = 'SANCHALAK' AND sabha_id = ?
                """).param(firstSabha).query(Long.class).single();
        assertThat(sanchalakStillAttached).isEqualTo(1L);

        // Reactivation reopens creation.
        jdbc.sql("UPDATE sabha_kinds SET retired_at = NULL, retired_by = NULL WHERE id = ?")
                .param(f.kindId).update();
        defineWeekly(f, "Drain Hall C");
        Long afterReactivate = jdbc.sql("SELECT count(*) FROM sabhas WHERE sabha_kind_id = ?")
                .param(f.kindId).query(Long.class).single();
        assertThat(afterReactivate).isEqualTo(2L);
    }

    // --- helpers -----------------------------------------------------------

    private UUID defineWeekly(Fixture f, String venue) throws Exception {
        String response = mockMvc.perform(authedPost(f.nirdeshakSubject.toString(), "/bff/sabhas", defineBody(f, venue)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(response.replaceAll(".*\"sabhaId\":\"([^\"]+)\".*", "$1"));
    }

    private String defineBody(Fixture f, String venue) {
        return """
                {"kshetraId":"%s","sabhaKindId":"%s","weekly":true,
                 "dayOfWeek":"SUNDAY","startTime":"09:00","endTime":"10:30",
                 "standingVenue":"%s",
                 "sanchalak":{"existingPersonId":"%s","username":"sanchalak-%s","rawPassword":"Temp#1234"}}
                """.formatted(f.kshetraId, f.kindId, venue, f.sanchalakPersonId, shortId());
    }


    private record Fixture(UUID kshetraId, UUID kindId, UUID nirdeshakSubject,
                           UUID nirdeshakUser, UUID sanchalakPersonId, UUID sanchalakUser) {
    }

    private Fixture seedStructureAndNirdeshak(String demographic, String track) {
        UUID cityId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        UUID kshetraId = UUID.randomUUID();
        UUID kindId = UUID.randomUUID();
        jdbc.sql("INSERT INTO cities (id, name, created_by) VALUES (?, ?, ?)")
                .param(cityId).param("DefCity-" + shortId()).param(bootstrapMk()).update();
        jdbc.sql("INSERT INTO zones (id, city_id, name, created_by) VALUES (?, ?, ?, ?)")
                .param(zoneId).param(cityId).param("DefZone-" + shortId()).param(bootstrapMk()).update();
        jdbc.sql("INSERT INTO kshetras (id, name, zone_id, created_by) VALUES (?, ?, ?, ?)")
                .param(kshetraId).param("DefKshetra-" + shortId()).param(zoneId).param(bootstrapMk()).update();
        jdbc.sql("INSERT INTO sabha_kinds (id, demographic, track, created_by) VALUES (?, ?, ?, ?)")
                .param(kindId).param(demographic).param(track).param(bootstrapMk()).update();

        UUID nirdeshakSubject = UUID.randomUUID();
        UUID nirdeshakPerson = UUID.randomUUID();
        UUID nirdeshakUser = UUID.randomUUID();
        jdbc.sql("INSERT INTO persons (id, full_name, gender, mobile) VALUES (?, 'Nirdeshak Def', 'MALE', ?)")
                .param(nirdeshakPerson).param(mobile()).update();
        jdbc.sql("INSERT INTO users (id, person_id, username, keycloak_user_id) VALUES (?, ?, ?, ?)")
                .param(nirdeshakUser).param(nirdeshakPerson).param("nirdeshak-" + shortId()).param(nirdeshakSubject).update();
        jdbc.sql("INSERT INTO role_assignments (id, user_id, role, kshetra_id, demographic) VALUES (?, ?, 'NIRDESHAK', ?, ?)")
                .param(UUID.randomUUID()).param(nirdeshakUser).param(kshetraId).param(demographic).update();

        UUID sanchalakPerson = UUID.randomUUID();
        UUID sanchalakUser = UUID.randomUUID();
        jdbc.sql("INSERT INTO persons (id, full_name, gender, mobile) VALUES (?, 'Sanchalak Def', 'MALE', ?)")
                .param(sanchalakPerson).param(mobile()).update();
        jdbc.sql("INSERT INTO users (id, person_id, username, keycloak_user_id) VALUES (?, ?, ?, ?)")
                .param(sanchalakUser).param(sanchalakPerson).param("sanchalak-" + shortId()).param(UUID.randomUUID()).update();

        return new Fixture(kshetraId, kindId, nirdeshakSubject, nirdeshakUser, sanchalakPerson, sanchalakUser);
    }

    private UUID seedUserWithoutRole(String label) {
        UUID subject = UUID.randomUUID();
        UUID person = UUID.randomUUID();
        jdbc.sql("INSERT INTO persons (id, full_name, gender, mobile) VALUES (?, ?, 'MALE', ?)")
                .param(person).param(label).param(mobile()).update();
        jdbc.sql("INSERT INTO users (id, person_id, username, keycloak_user_id) VALUES (?, ?, ?, ?)")
                .param(UUID.randomUUID()).param(person).param(label + "-" + shortId()).param(subject).update();
        return subject;
    }

    private UUID bootstrapMk() {
        return jdbc.sql("SELECT id FROM users WHERE username = ?")
                .param(MK_USERNAME)
                .query((rs, n) -> rs.getObject("id", UUID.class)).single();
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String mobile() {
        return "+9198" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode()) % 100000000);
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
