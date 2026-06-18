package org.sabha.identity.applicationservice.session;

import java.util.Set;

import org.sabha.identity.domain.Section;

/**
 * What the web shell needs to render after login (Slice 9): the User's display
 * name, whether they are a Madhyastha Karyalaya member, whether they are a
 * Regional Team member (which now owns Zone creation — ADR-0024, issue #84), and
 * the set of {@link Section}s their authority unlocks.
 */
public record WebSession(
        String username,
        boolean madhyasthaKaryalaya,
        boolean regionalTeam,
        Set<Section> sections) {
}
