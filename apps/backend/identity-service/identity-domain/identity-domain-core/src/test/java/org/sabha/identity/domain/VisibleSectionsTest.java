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
                Section.SABHA_DEFINITION,
                Section.OCCURRENCE_REOPEN);
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
}
