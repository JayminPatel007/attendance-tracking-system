package org.sabha.identity.applicationservice;

import java.util.Set;

import org.sabha.identity.domain.Section;

/**
 * What the web shell needs to render after login (Slice 9): the User's display
 * name, whether they are a Madhyastha Karyalaya member, and the set of
 * {@link Section}s their authority unlocks.
 */
public record WebSession(String username, boolean madhyasthaKaryalaya, Set<Section> sections) {
}
