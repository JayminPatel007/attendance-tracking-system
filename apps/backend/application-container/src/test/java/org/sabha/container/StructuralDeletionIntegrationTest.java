package org.sabha.container;

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
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Structural deletion end-to-end over the BFF (ADR-0026). Proves block-if-non-empty
 * and the tier-above authority chain through real HTTP + Postgres: each entity is
 * deletable only by its owning scope holder (MK / Regional Team / Sanyojak /
 * Nirdeshak), only while empty, and a non-empty delete is rejected with its
 * human-readable blocking reason — never cascading, so children survive a blocked
 * delete. The class is {@code @Transactional}, so every seeded row rolls back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StructuralDeletionIntegrationTest extends KeycloakIntegrationTest {

    /** Seeded Sanchalak (non-authority) Keycloak subject — infra/keycloak/realm-sabha.json. */
    private static final String SANCHALAK_SUBJECT = "00000000-0000-0000-0000-000000000005";

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

    // --- City (MK) ---------------------------------------------------------

    @Test
    void anMkMemberDeletesAnEmptyCity() throws Exception {
        String mk = keycloakSubject(MK_USERNAME);
        UUID cityId = createCity(mk, "Surat");

        mockMvc.perform(authedDelete(mk, "/bff/structure/cities/" + cityId))
                .andExpect(status().isNoContent());

        assertThat(exists("cities", cityId)).isFalse();
    }

    @Test
    void deletingACityWithAZoneIsBlockedAndTheCitySurvives() throws Exception {
        String mk = keycloakSubject(MK_USERNAME);
        UUID cityId = createCity(mk, "Mumbai");
        Authority rt = seedRegionalTeamMember(cityId, "mum-del");
        createZone(rt.subject().toString(), cityId, "MumbaiWest");

        mockMvc.perform(authedDelete(mk, "/bff/structure/cities/" + cityId))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("STRUCTURE_NOT_EMPTY"))
                .andExpect(jsonPath("$.detail").value("has 1 Zone"));

        assertThat(exists("cities", cityId)).isTrue();
    }

    @Test
    void aNonMkUserCannotDeleteACity() throws Exception {
        String mk = keycloakSubject(MK_USERNAME);
        UUID cityId = createCity(mk, "Rajkot");

        mockMvc.perform(authedDelete(SANCHALAK_SUBJECT, "/bff/structure/cities/" + cityId))
                .andExpect(status().isForbidden());

        assertThat(exists("cities", cityId)).isTrue();
    }

    // --- Zone (Regional Team) ---------------------------------------------

    @Test
    void aRegionalTeamMemberDeletesAnEmptyZoneInTheirCity() throws Exception {
        String mk = keycloakSubject(MK_USERNAME);
        UUID cityId = createCity(mk, "Vadodara");
        Authority rt = seedRegionalTeamMember(cityId, "vad-del");
        UUID zoneId = createZone(rt.subject().toString(), cityId, "VadodaraNorth");

        mockMvc.perform(authedDelete(rt.subject().toString(), "/bff/structure/zones/" + zoneId))
                .andExpect(status().isNoContent());

        assertThat(exists("zones", zoneId)).isFalse();
    }

    @Test
    void deletingAZoneWithAKshetraIsBlockedAndTheZoneSurvives() throws Exception {
        String mk = keycloakSubject(MK_USERNAME);
        UUID cityId = createCity(mk, "Anand");
        Authority rt = seedRegionalTeamMember(cityId, "anand-del");
        UUID zoneId = createZone(rt.subject().toString(), cityId, "AnandCentral");
        Authority sanyojak = seedSanyojak(zoneId, "anand-del");
        createKshetra(sanyojak.subject().toString(), zoneId, "AnandK1");

        mockMvc.perform(authedDelete(rt.subject().toString(), "/bff/structure/zones/" + zoneId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("has 1 Kshetra"));

        assertThat(exists("zones", zoneId)).isTrue();
    }

    @Test
    void aRegionalTeamMemberOfAnotherCityCannotDeleteThisZone() throws Exception {
        String mk = keycloakSubject(MK_USERNAME);
        UUID cityId = createCity(mk, "Bhuj");
        UUID otherCity = createCity(mk, "Morbi");
        Authority rtHere = seedRegionalTeamMember(cityId, "bhuj-del");
        Authority rtElsewhere = seedRegionalTeamMember(otherCity, "morbi-del");
        UUID zoneId = createZone(rtHere.subject().toString(), cityId, "BhujNorth");

        mockMvc.perform(authedDelete(rtElsewhere.subject().toString(), "/bff/structure/zones/" + zoneId))
                .andExpect(status().isForbidden());

        assertThat(exists("zones", zoneId)).isTrue();
    }

    // --- Kshetra (Sanyojak) -----------------------------------------------

    @Test
    void theZonesSanyojakDeletesAnEmptyKshetra() throws Exception {
        String mk = keycloakSubject(MK_USERNAME);
        UUID cityId = createCity(mk, "Surendranagar");
        Authority rt = seedRegionalTeamMember(cityId, "snr-del");
        UUID zoneId = createZone(rt.subject().toString(), cityId, "SnrCentral");
        Authority sanyojak = seedSanyojak(zoneId, "snr-del");
        UUID kshetraId = createKshetra(sanyojak.subject().toString(), zoneId, "SnrK1");

        mockMvc.perform(authedDelete(sanyojak.subject().toString(), "/bff/structure/kshetras/" + kshetraId))
                .andExpect(status().isNoContent());

        assertThat(exists("kshetras", kshetraId)).isFalse();
    }

    @Test
    void deletingAKshetraWithASabhaIsBlockedAndTheKshetraSurvives() throws Exception {
        String mk = keycloakSubject(MK_USERNAME);
        UUID cityId = createCity(mk, "Junagadh");
        Authority rt = seedRegionalTeamMember(cityId, "jun-del");
        UUID zoneId = createZone(rt.subject().toString(), cityId, "JunCentral");
        Authority sanyojak = seedSanyojak(zoneId, "jun-del");
        UUID kshetraId = createKshetra(sanyojak.subject().toString(), zoneId, "JunK1");
        seedSabha(kshetraId, "REGULAR_YUVAK");

        mockMvc.perform(authedDelete(sanyojak.subject().toString(), "/bff/structure/kshetras/" + kshetraId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("has 1 Sabha"));

        assertThat(exists("kshetras", kshetraId)).isTrue();
    }

    @Test
    void aSanyojakOfAnotherZoneCannotDeleteThisKshetra() throws Exception {
        String mk = keycloakSubject(MK_USERNAME);
        UUID cityId = createCity(mk, "Gondal");
        Authority rt = seedRegionalTeamMember(cityId, "gon-del");
        UUID zoneId = createZone(rt.subject().toString(), cityId, "GonCentral");
        UUID otherZoneId = createZone(rt.subject().toString(), cityId, "GonEast");
        Authority sanyojakHere = seedSanyojak(zoneId, "gon-del");
        Authority sanyojakElsewhere = seedSanyojak(otherZoneId, "gon-del2");
        UUID kshetraId = createKshetra(sanyojakHere.subject().toString(), zoneId, "GonK1");

        mockMvc.perform(authedDelete(sanyojakElsewhere.subject().toString(), "/bff/structure/kshetras/" + kshetraId))
                .andExpect(status().isForbidden());

        assertThat(exists("kshetras", kshetraId)).isTrue();
    }

    // --- Sabha (Nirdeshak) -------------------------------------------------

    @Test
    void theSabhasNirdeshakDeletesAnEmptySabha() throws Exception {
        String mk = keycloakSubject(MK_USERNAME);
        UUID kshetraId = seedKshetraRow();
        UUID sabhaId = seedSabha(kshetraId, "REGULAR_YUVAK");
        Authority nirdeshak = seedNirdeshak(kshetraId, "YUVAK", "nird-del");

        mockMvc.perform(authedDelete(nirdeshak.subject().toString(), "/bff/sabhas/" + sabhaId))
                .andExpect(status().isNoContent());

        assertThat(exists("sabhas", sabhaId)).isFalse();
    }

    @Test
    void deletingASabhaWithAnOccurrenceIsBlockedAndTheSabhaSurvives() throws Exception {
        UUID kshetraId = seedKshetraRow();
        UUID sabhaId = seedSabha(kshetraId, "REGULAR_YUVAK");
        seedOccurrence(sabhaId);
        Authority nirdeshak = seedNirdeshak(kshetraId, "YUVAK", "nird-del2");

        mockMvc.perform(authedDelete(nirdeshak.subject().toString(), "/bff/sabhas/" + sabhaId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("has 1 Occurrence"));

        assertThat(exists("sabhas", sabhaId)).isTrue();
    }

    @Test
    void aNirdeshakOfAnotherDemographicCannotDeleteThisSabha() throws Exception {
        UUID kshetraId = seedKshetraRow();
        UUID sabhaId = seedSabha(kshetraId, "REGULAR_YUVAK");
        // Holds Nirdeshak over a different demographic in the same Kshetra.
        Authority nirdeshak = seedNirdeshak(kshetraId, "BAAL", "nird-del3");

        mockMvc.perform(authedDelete(nirdeshak.subject().toString(), "/bff/sabhas/" + sabhaId))
                .andExpect(status().isForbidden());

        assertThat(exists("sabhas", sabhaId)).isTrue();
    }

    // --- helpers -----------------------------------------------------------

    private UUID createCity(String mkSubject, String name) throws Exception {
        return idOf(mockMvc.perform(authedPost(mkSubject, "/bff/structure/cities", "{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private UUID createZone(String rtSubject, UUID cityId, String name) throws Exception {
        return idOf(mockMvc.perform(authedPost(rtSubject, "/bff/structure/zones",
                        "{\"cityId\":\"" + cityId + "\",\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private UUID createKshetra(String sanyojakSubject, UUID zoneId, String name) throws Exception {
        return idOf(mockMvc.perform(authedPost(sanyojakSubject, "/bff/structure/kshetras",
                        "{\"zoneId\":\"" + zoneId + "\",\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    /** Seeds a bare Kshetra row (no Zone parent needed for the Sabha-scope path). */
    private UUID seedKshetraRow() {
        UUID kshetraId = UUID.randomUUID();
        jdbc.sql("INSERT INTO kshetras (id, name) VALUES (?, 'Sabha-Del Kshetra')").param(kshetraId).update();
        return kshetraId;
    }

    private UUID seedSabha(UUID kshetraId, String sabhaKind) {
        UUID sabhaId = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO sabhas (id, kshetra_id, sabha_kind, schedule_shape, standing_venue)
                VALUES (?, ?, ?, 'MONTHLY_AD_HOC', 'Hall')
                """).param(sabhaId).param(kshetraId).param(sabhaKind).update();
        return sabhaId;
    }

    private void seedOccurrence(UUID sabhaId) {
        jdbc.sql("""
                INSERT INTO occurrences (id, sabha_id, occurrence_date, state)
                VALUES (?, ?, DATE '2026-01-01', 'FINALIZED')
                """).param(UUID.randomUUID()).param(sabhaId).update();
    }

    private Authority seedRegionalTeamMember(UUID cityId, String tag) {
        Authority a = seedUser("rt-del-" + tag);
        jdbc.sql("INSERT INTO role_assignments (id, user_id, role, city_id, demographic) VALUES (?, ?, 'REGIONAL_TEAM', ?, 'YUVAK')")
                .param(UUID.randomUUID()).param(a.userId()).param(cityId).update();
        return a;
    }

    private Authority seedSanyojak(UUID zoneId, String tag) {
        Authority a = seedUser("sanyojak-del-" + tag);
        jdbc.sql("INSERT INTO role_assignments (id, user_id, role, zone_id) VALUES (?, ?, 'SANYOJAK', ?)")
                .param(UUID.randomUUID()).param(a.userId()).param(zoneId).update();
        return a;
    }

    private Authority seedNirdeshak(UUID kshetraId, String demographic, String tag) {
        Authority a = seedUser("nirdeshak-del-" + tag);
        jdbc.sql("INSERT INTO role_assignments (id, user_id, role, kshetra_id, demographic) VALUES (?, ?, 'NIRDESHAK', ?, ?)")
                .param(UUID.randomUUID()).param(a.userId()).param(kshetraId).param(demographic).update();
        return a;
    }

    private Authority seedUser(String username) {
        UUID subject = UUID.randomUUID();
        UUID person = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        jdbc.sql("INSERT INTO persons (id, full_name, gender, mobile) VALUES (?, 'Del Authority', 'MALE', ?)")
                .param(person).param(String.format("+9198202%05d", MOBILE_SEQ.getAndIncrement())).update();
        jdbc.sql("INSERT INTO users (id, person_id, username, keycloak_user_id) VALUES (?, ?, ?, ?)")
                .param(user).param(person).param(username).param(subject).update();
        return new Authority(subject, user);
    }

    private record Authority(UUID subject, UUID userId) {
    }

    private boolean exists(String table, UUID id) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM " + table + " WHERE id = ?)")
                .param(id).query(Boolean.class).single();
    }

    private static UUID idOf(String body) {
        return UUID.fromString(body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));
    }

    private MockHttpServletRequestBuilder authedPost(String subject, String path, String json) throws Exception {
        Cookie xsrf = xsrf(subject);
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                .with(oidcLogin().idToken(t -> t.subject(subject)))
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue())
                .contentType("application/json")
                .content(json);
    }

    private MockHttpServletRequestBuilder authedDelete(String subject, String path) throws Exception {
        Cookie xsrf = xsrf(subject);
        return delete(path)
                .with(oidcLogin().idToken(t -> t.subject(subject)))
                .cookie(xsrf)
                .header("X-XSRF-TOKEN", xsrf.getValue());
    }

    private Cookie xsrf(String subject) throws Exception {
        return mockMvc.perform(get("/bff/me").with(oidcLogin().idToken(t -> t.subject(subject))))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
    }

    private String keycloakSubject(String username) {
        return jdbc.sql("SELECT keycloak_user_id FROM users WHERE username = ?")
                .param(username).query((rs, n) -> rs.getObject("keycloak_user_id", UUID.class)).single().toString();
    }
}
