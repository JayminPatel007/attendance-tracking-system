package org.sabha.container;

import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Structural creation end-to-end over the BFF (ADR-0009, ADR-0024, ADR-0022).
 * Proves the authority chain through real HTTP + Postgres: an MK member creates
 * Cities and Sabha Kinds; a Regional Team member creates Zones only within their
 * own City (the authority that moved down from MK — ADR-0024); a Sanyojak creates
 * Kshetras only within their Zone; non-authorized callers (including an MK
 * attempting a Zone) are rejected by the Authorization Engine; and the
 * Sanyukta-Regular-only invariant holds at the wire.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StructuralCreationIntegrationTest extends KeycloakIntegrationTest {

    /** Seeded Sanchalak (non-MK) Keycloak subject — infra/keycloak/realm-sabha.json. */
    private static final String SANCHALAK_SUBJECT = "00000000-0000-0000-0000-000000000005";

    private static final UUID SANYOJAK_SUBJECT = UUID.fromString("00000000-0000-0000-0000-0000000005a1");
    private static final UUID SANYOJAK_PERSON = UUID.fromString("00000000-0000-0000-0000-0000000005a2");
    private static final UUID SANYOJAK_USER = UUID.fromString("00000000-0000-0000-0000-0000000005a3");

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

    @Test
    void mkMemberCreatesACityPersistedWithCreatedBySetToThemselves() throws Exception {
        UUID mkUser = localUserId(MK_USERNAME);
        String mkSubject = keycloakSubject(MK_USERNAME);

        String body = mockMvc.perform(authedPost(mkSubject, "/bff/structure/cities", "{\"name\":\"Surat\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();
        UUID cityId = UUID.fromString(body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));

        UUID createdBy = jdbc.sql("SELECT created_by FROM cities WHERE id = ?")
                .param(cityId).query((rs, n) -> rs.getObject("created_by", UUID.class)).single();
        assertThat(createdBy).isEqualTo(mkUser);
    }

    @Test
    void aRegionalTeamMemberCreatesAZoneWithinTheirCityAttributedToThemselves() throws Exception {
        String mkSubject = keycloakSubject(MK_USERNAME);
        UUID cityId = createCity(mkSubject, "Vadodara");
        RegionalTeamMember rt = seedRegionalTeamMember(cityId, "vad");

        mockMvc.perform(authedPost(rt.subject().toString(), "/bff/structure/zones",
                        "{\"cityId\":\"" + cityId + "\",\"name\":\"VadodaraNorth\"}"))
                .andExpect(status().isCreated());

        UUID createdBy = jdbc.sql("SELECT created_by FROM zones WHERE city_id = ? AND name = 'VadodaraNorth'")
                .param(cityId).query((rs, n) -> rs.getObject("created_by", UUID.class)).single();
        assertThat(createdBy).isEqualTo(rt.userId());
    }

    @Test
    void anMkMemberCannotCreateAZone() throws Exception {
        // Zone creation moved MK -> Regional Team (ADR-0024); MK has no path at all.
        String mkSubject = keycloakSubject(MK_USERNAME);
        UUID cityId = createCity(mkSubject, "Vadodara");

        mockMvc.perform(authedPost(mkSubject, "/bff/structure/zones",
                        "{\"cityId\":\"" + cityId + "\",\"name\":\"VadodaraNorth\"}"))
                .andExpect(status().isForbidden());

        Long zones = jdbc.sql("SELECT count(*) FROM zones WHERE city_id = ?").param(cityId).query(Long.class).single();
        assertThat(zones).isEqualTo(0L);
    }

    @Test
    void aRegionalTeamMemberOfAnotherCityCannotCreateAZoneHere() throws Exception {
        String mkSubject = keycloakSubject(MK_USERNAME);
        UUID myCity = createCity(mkSubject, "Vadodara");
        UUID otherCity = createCity(mkSubject, "Anand");
        RegionalTeamMember rtOfOtherCity = seedRegionalTeamMember(otherCity, "anand");

        mockMvc.perform(authedPost(rtOfOtherCity.subject().toString(), "/bff/structure/zones",
                        "{\"cityId\":\"" + myCity + "\",\"name\":\"VadodaraNorth\"}"))
                .andExpect(status().isForbidden());

        // ...but the my-cities scope of their create form is exactly the City they hold.
        mockMvc.perform(get("/bff/structure/my-cities")
                        .with(oidcLogin().idToken(t -> t.subject(rtOfOtherCity.subject().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(otherCity.toString()));
    }

    @Test
    void mkMemberRegistersASabhaKind() throws Exception {
        String mkSubject = keycloakSubject(MK_USERNAME);

        mockMvc.perform(authedPost(mkSubject, "/bff/structure/sabha-kinds",
                        "{\"demographic\":\"YUVATI\",\"track\":\"YSS\"}"))
                .andExpect(status().isCreated());

        Long kinds = jdbc.sql("SELECT count(*) FROM sabha_kinds WHERE demographic='YUVATI' AND track='YSS'")
                .query(Long.class).single();
        assertThat(kinds).isEqualTo(1L);
    }

    @Test
    void mkRetiresThenReactivatesASabhaKindAttributedToThemAndReflectedInTheReadModel() throws Exception {
        String mkSubject = keycloakSubject(MK_USERNAME);
        UUID mkUser = localUserId(MK_USERNAME);
        UUID kindId = createSabhaKind(mkSubject, "BALIKA", "BSS");

        mockMvc.perform(authedPost(mkSubject, "/bff/structure/sabha-kinds/" + kindId + "/retire", ""))
                .andExpect(status().isNoContent());

        Object[] retired = jdbc.sql("SELECT retired_at, retired_by FROM sabha_kinds WHERE id = ?")
                .param(kindId).query((rs, n) -> new Object[] {
                    rs.getObject("retired_at"), rs.getObject("retired_by", UUID.class) }).single();
        assertThat(retired[0]).isNotNull();
        assertThat(retired[1]).isEqualTo(mkUser);

        mockMvc.perform(get("/bff/structure/sabha-kinds")
                        .with(oidcLogin().idToken(t -> t.subject(mkSubject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + kindId + "')].retiredAt").isNotEmpty());

        mockMvc.perform(authedPost(mkSubject, "/bff/structure/sabha-kinds/" + kindId + "/reactivate", ""))
                .andExpect(status().isNoContent());

        assertThat(isActive(kindId)).isTrue();
    }

    @Test
    void aNonMkUserCannotRetireASabhaKind() throws Exception {
        String mkSubject = keycloakSubject(MK_USERNAME);
        UUID kindId = createSabhaKind(mkSubject, "YUVATI", "BSS");

        mockMvc.perform(authedPost(SANCHALAK_SUBJECT, "/bff/structure/sabha-kinds/" + kindId + "/retire", ""))
                .andExpect(status().isForbidden());

        assertThat(isActive(kindId)).isTrue();
    }

    private boolean isActive(UUID kindId) {
        return jdbc.sql("SELECT retired_at IS NULL FROM sabha_kinds WHERE id = ?")
                .param(kindId).query(Boolean.class).single();
    }

    @Test
    void theKindBuilderRejectsASanyuktaSelectiveKind() throws Exception {
        String mkSubject = keycloakSubject(MK_USERNAME);

        mockMvc.perform(authedPost(mkSubject, "/bff/structure/sabha-kinds",
                        "{\"demographic\":\"SANYUKTA\",\"track\":\"BSS\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.title").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void aNonMkUserCannotCreateACity() throws Exception {
        mockMvc.perform(authedPost(SANCHALAK_SUBJECT, "/bff/structure/cities", "{\"name\":\"Rajkot\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void aSanyojakCreatesAKshetraWithinTheirZoneButNotOutsideIt() throws Exception {
        String mkSubject = keycloakSubject(MK_USERNAME);
        UUID cityId = createCity(mkSubject, "Bhavnagar");
        String rtSubject = seedRegionalTeamMember(cityId, "bhav").subject().toString();
        UUID myZone = createZone(rtSubject, cityId, "BhavnagarCentral");
        UUID otherZone = createZone(rtSubject, cityId, "BhavnagarEast");
        seedSanyojak(myZone);

        mockMvc.perform(authedPost(SANYOJAK_SUBJECT.toString(), "/bff/structure/kshetras",
                        "{\"zoneId\":\"" + myZone + "\",\"name\":\"Goregaon-2\"}"))
                .andExpect(status().isCreated());

        UUID createdBy = jdbc.sql("SELECT created_by FROM kshetras WHERE zone_id = ? AND name = 'Goregaon-2'")
                .param(myZone).query((rs, n) -> rs.getObject("created_by", UUID.class)).single();
        assertThat(createdBy).isEqualTo(SANYOJAK_USER);

        mockMvc.perform(authedPost(SANYOJAK_SUBJECT.toString(), "/bff/structure/kshetras",
                        "{\"zoneId\":\"" + otherZone + "\",\"name\":\"OutOfScope\"}"))
                .andExpect(status().isForbidden());

        // my-zones scopes the Sanyojak's create form to exactly the Zone they own.
        mockMvc.perform(get("/bff/structure/my-zones")
                        .with(oidcLogin().idToken(t -> t.subject(SANYOJAK_SUBJECT.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(myZone.toString()));
    }

    // --- helpers -----------------------------------------------------------

    private UUID createCity(String mkSubject, String name) throws Exception {
        String body = mockMvc.perform(authedPost(mkSubject, "/bff/structure/cities", "{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));
    }

    private UUID createSabhaKind(String mkSubject, String demographic, String track) throws Exception {
        String body = mockMvc.perform(authedPost(mkSubject, "/bff/structure/sabha-kinds",
                        "{\"demographic\":\"" + demographic + "\",\"track\":\"" + track + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));
    }

    /** Creates a Zone over the BFF as a Regional Team member (Zone authority, ADR-0024). */
    private UUID createZone(String regionalTeamSubject, UUID cityId, String name) throws Exception {
        String body = mockMvc.perform(authedPost(regionalTeamSubject, "/bff/structure/zones",
                        "{\"cityId\":\"" + cityId + "\",\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));
    }

    /**
     * Seeds a Regional Team member of {@code cityId} — a Person, their User, and a
     * {@code REGIONAL_TEAM} role_assignment carrying the City scope (the demographic
     * is irrelevant to Zone authority — ADR-0024). The Keycloak subject is the
     * seeded User's {@code keycloak_user_id}, so {@code oidcLogin} resolves to it.
     */
    private RegionalTeamMember seedRegionalTeamMember(UUID cityId, String tag) {
        UUID subject = UUID.randomUUID();
        UUID person = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        jdbc.sql("INSERT INTO persons (id, full_name, gender, mobile) VALUES (?, 'RT Member', 'MALE', ?)")
                .param(person).param(String.format("+9198201%05d", MOBILE_SEQ.getAndIncrement())).update();
        jdbc.sql("INSERT INTO users (id, person_id, username, keycloak_user_id) VALUES (?, ?, ?, ?)")
                .param(user).param(person).param("rt-struct-" + tag).param(subject).update();
        jdbc.sql("INSERT INTO role_assignments (id, user_id, role, city_id, demographic) VALUES (?, ?, 'REGIONAL_TEAM', ?, 'YUVAK')")
                .param(UUID.randomUUID()).param(user).param(cityId).update();
        return new RegionalTeamMember(subject, user);
    }

    private record RegionalTeamMember(UUID subject, UUID userId) {
    }

    private void seedSanyojak(UUID zoneId) {
        jdbc.sql("INSERT INTO persons (id, full_name, gender, mobile) VALUES (?, 'Sanyojak Tracer', 'MALE', '+919820100777')")
                .param(SANYOJAK_PERSON).update();
        jdbc.sql("INSERT INTO users (id, person_id, username, keycloak_user_id) VALUES (?, ?, 'sanyojak-struct', ?)")
                .param(SANYOJAK_USER).param(SANYOJAK_PERSON).param(SANYOJAK_SUBJECT).update();
        jdbc.sql("INSERT INTO role_assignments (id, user_id, role, zone_id) VALUES (?, ?, 'SANYOJAK', ?)")
                .param(UUID.randomUUID()).param(SANYOJAK_USER).param(zoneId).update();
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

    private String keycloakSubject(String username) {
        return jdbc.sql("SELECT keycloak_user_id FROM users WHERE username = ?")
                .param(username).query((rs, n) -> rs.getObject("keycloak_user_id", UUID.class)).single().toString();
    }

    private UUID localUserId(String username) {
        return jdbc.sql("SELECT id FROM users WHERE username = ?")
                .param(username).query((rs, n) -> rs.getObject("id", UUID.class)).single();
    }
}
