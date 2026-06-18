package org.sabha.identity.domain;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.sabha.common.Role;

import static org.assertj.core.api.Assertions.assertThat;

class VisibleSectionsTest {

    @Test
    void aMadhyasthaKaryalayaMemberSeesTheStateOversightSections() {
        Set<Section> sections = VisibleSections.forMember(madhyasthaKaryalaya());

        assertThat(sections).contains(
                Section.DASHBOARD,
                Section.ROLE_APPOINTMENT,
                Section.STRUCTURAL_ADMIN,
                Section.SABHA_DEFINITION);
        // MK is an oversight tier, explicitly kept out of the reopen data-edit path
        // (ADR-0001), so it does not see the Occurrence-reopen section.
        assertThat(sections).doesNotContain(Section.OCCURRENCE_REOPEN);
    }

    @Test
    void theKshetraTiersSeeTheOccurrenceReopenSection() {
        for (Role tier : Set.of(Role.NIRIKSHAK, Role.NIRDESHAK, Role.SAH_NIRDESHAK)) {
            Set<Section> sections = VisibleSections.forMember(operational(tier));

            assertThat(sections)
                    .as("%s should see Occurrence Reopen", tier)
                    .contains(Section.OCCURRENCE_REOPEN);
        }
    }

    @Test
    void aSanchalakDoesNotSeeTheOccurrenceReopenSection() {
        Set<Section> sections = VisibleSections.forMember(operational(Role.SANCHALAK));

        assertThat(sections).doesNotContain(Section.OCCURRENCE_REOPEN);
    }

    @Test
    void aNonMkUserDoesNotSeeTheMkOnlySections() {
        Set<Section> sections = VisibleSections.forMember(operational());

        assertThat(sections).containsExactly(Section.DASHBOARD);
        assertThat(sections).doesNotContain(
                Section.ROLE_APPOINTMENT,
                Section.STRUCTURAL_ADMIN,
                Section.SABHA_DEFINITION,
                Section.OCCURRENCE_REOPEN);
    }

    @Test
    void aNirikshakSeesTheSanchalakProxySection() {
        Set<Section> sections = VisibleSections.forMember(operational(Role.NIRIKSHAK));

        assertThat(sections).contains(Section.SANCHALAK_PROXY);
    }

    @Test
    void aNirdeshakSeesTheSelectionSection() {
        Set<Section> sections = VisibleSections.forMember(operational(Role.NIRDESHAK));

        assertThat(sections).contains(Section.SELECTION);
    }

    @Test
    void aSanchalakDoesNotSeeTheSelectionSection() {
        Set<Section> sections = VisibleSections.forMember(operational(Role.SANCHALAK));

        assertThat(sections).doesNotContain(Section.SELECTION);
    }

    @Test
    void aSanyojakSeesTheStructuralAdminSectionForKshetraCreation() {
        Set<Section> sections = VisibleSections.forMember(operational(Role.SANYOJAK));

        assertThat(sections).contains(Section.STRUCTURAL_ADMIN);
        // ...but not the MK-only sections.
        assertThat(sections).doesNotContain(Section.ROLE_APPOINTMENT, Section.SABHA_DEFINITION);
    }

    @Test
    void aRegionalTeamMemberSeesTheStructuralAdminSectionForZoneCreation() {
        // Zone creation moved MK -> Regional Team (ADR-0024), so an RT member now
        // reaches Structural Admin — but RT is City-level oversight, not an MK, so
        // it gets none of the MK-only sections.
        Set<Section> sections = VisibleSections.forMember(regionalTeam());

        assertThat(sections).contains(Section.STRUCTURAL_ADMIN);
        assertThat(sections).doesNotContain(
                Section.ROLE_APPOINTMENT,
                Section.SABHA_DEFINITION,
                Section.OCCURRENCE_REOPEN);
    }

    @Test
    void aMemberWhoCanReadTheAuditLogSeesTheAuditLogSection() {
        // The web gate no longer enumerates audit tiers itself: it trusts the BFF's
        // audit-scope resolution (AuditLogAccess) handed in as canReadAudit, so the
        // sidebar admits exactly the set the engine admits — including the Regional
        // Team, which is not an operational Role (issue #80, ADR-0023). Which tiers
        // resolve to read access is asserted in AuditLogAccessTest, the one authority.
        assertThat(VisibleSections.forMember(auditReader()))
                .contains(Section.AUDIT_LOG);
    }

    @Test
    void aMemberWhoCannotReadTheAuditLogDoesNotSeeTheAuditLogSection() {
        // Even a Nirdeshak — the tier the old hand-maintained AUDIT_TIERS fold
        // unlocked from the role set alone — gets no section when canReadAudit is
        // false. There is no enumeration left to drift from the engine.
        assertThat(VisibleSections.forMember(operational(Role.NIRDESHAK)))
                .doesNotContain(Section.AUDIT_LOG);
    }

    // --- authority fixtures: each names the single fact under test --------------

    private static MemberAuthority madhyasthaKaryalaya() {
        return new MemberAuthority(true, false, false, Set.of());
    }

    private static MemberAuthority regionalTeam() {
        return new MemberAuthority(false, true, false, Set.of());
    }

    private static MemberAuthority auditReader() {
        return new MemberAuthority(false, false, true, Set.of());
    }

    private static MemberAuthority operational(Role... roles) {
        return new MemberAuthority(false, false, false, Set.of(roles));
    }
}
