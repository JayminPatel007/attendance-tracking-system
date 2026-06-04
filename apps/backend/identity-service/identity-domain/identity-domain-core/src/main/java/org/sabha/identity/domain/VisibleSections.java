package org.sabha.identity.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.sabha.common.Role;

/**
 * Pure domain rule mapping a User's authority to the web shell {@link Section}s
 * they may see (Slice 9 role-based visibility). State-level oversight
 * (Madhyastha Karyalaya) unlocks the structural / appointment sections per
 * ADR-0009 and ADR-0011; a Sanyojak also reaches Structural Admin, but only for
 * Kshetra creation within their Zone (ADR-0009, Slice 10). The Occurrence-reopen
 * section belongs to the Kshetra tiers (Nirikshak / Nirdeshak / Sah-Nirdeshak),
 * who alone may reopen a Finalized Occurrence — never MK or the other oversight
 * tiers (ADR-0001, Slice 13). The Sanchalak-proxy section belongs to the
 * Nirikshak (Slice 14). Everyone with a web login sees the Dashboard.
 *
 * <p>Stateless: the application service loads the membership flag and roles and
 * passes them in — the domain service never touches a repository (ADR-0019).</p>
 */
public final class VisibleSections {

    private VisibleSections() {
    }

    public static Set<Section> forMember(boolean isMadhyasthaKaryalaya, Set<Role> operationalRoles) {
        EnumSet<Section> sections = EnumSet.of(Section.DASHBOARD);
        if (isMadhyasthaKaryalaya) {
            sections.add(Section.ROLE_APPOINTMENT);
            sections.add(Section.STRUCTURAL_ADMIN);
            sections.add(Section.SABHA_DEFINITION);
        }
        if (operationalRoles.contains(Role.SANYOJAK)) {
            sections.add(Section.STRUCTURAL_ADMIN);
        }
        if (operationalRoles.contains(Role.NIRIKSHAK)) {
            sections.add(Section.SANCHALAK_PROXY);
        }
        if (!Collections.disjoint(operationalRoles, Role.REOPEN_TIERS)) {
            sections.add(Section.OCCURRENCE_REOPEN);
        }
        return sections;
    }
}
