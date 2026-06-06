package org.sabha.identity.domain;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.sabha.common.Role;

import static org.assertj.core.api.Assertions.assertThat;

class VisibleSectionsTest {

    @Test
    void aMadhyasthaKaryalayaMemberSeesTheStateOversightSections() {
        Set<Section> sections = VisibleSections.forMember(true, Set.of());

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
            Set<Section> sections = VisibleSections.forMember(false, Set.of(tier));

            assertThat(sections)
                    .as("%s should see Occurrence Reopen", tier)
                    .contains(Section.OCCURRENCE_REOPEN);
        }
    }

    @Test
    void aSanchalakDoesNotSeeTheOccurrenceReopenSection() {
        Set<Section> sections = VisibleSections.forMember(false, Set.of(Role.SANCHALAK));

        assertThat(sections).doesNotContain(Section.OCCURRENCE_REOPEN);
    }

    @Test
    void aNonMkUserDoesNotSeeTheMkOnlySections() {
        Set<Section> sections = VisibleSections.forMember(false, Set.of());

        assertThat(sections).containsExactly(Section.DASHBOARD);
        assertThat(sections).doesNotContain(
                Section.ROLE_APPOINTMENT,
                Section.STRUCTURAL_ADMIN,
                Section.SABHA_DEFINITION,
                Section.OCCURRENCE_REOPEN);
    }

    @Test
    void aNirikshakSeesTheSanchalakProxySection() {
        Set<Section> sections = VisibleSections.forMember(false, Set.of(Role.NIRIKSHAK));

        assertThat(sections).contains(Section.SANCHALAK_PROXY);
    }

    @Test
    void aNirdeshakSeesTheSelectionSection() {
        Set<Section> sections = VisibleSections.forMember(false, Set.of(Role.NIRDESHAK));

        assertThat(sections).contains(Section.SELECTION);
    }

    @Test
    void aSanchalakDoesNotSeeTheSelectionSection() {
        Set<Section> sections = VisibleSections.forMember(false, Set.of(Role.SANCHALAK));

        assertThat(sections).doesNotContain(Section.SELECTION);
    }

    @Test
    void aSanyojakSeesTheStructuralAdminSectionForKshetraCreation() {
        Set<Section> sections = VisibleSections.forMember(false, Set.of(Role.SANYOJAK));

        assertThat(sections).contains(Section.STRUCTURAL_ADMIN);
        // ...but not the MK-only sections.
        assertThat(sections).doesNotContain(Section.ROLE_APPOINTMENT, Section.SABHA_DEFINITION);
    }
}
