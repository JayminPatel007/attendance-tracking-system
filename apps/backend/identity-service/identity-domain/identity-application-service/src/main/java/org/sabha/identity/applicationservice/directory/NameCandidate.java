package org.sabha.identity.applicationservice.directory;

import java.util.List;
import java.util.UUID;

/**
 * A possible duplicate surfaced by the name soft-warn (ADR-0013): an existing
 * Person whose name is phonetically or edit-distance close to the one being
 * added, within the same geographic scope. Carries just enough for the adder to
 * recognise the Person on the add screen — including all their current Home Sabha
 * kinds (a Person has one per kind they qualify for, CONTEXT.md), so the adder
 * sees the demographic Sabha and not just whichever sorts first.
 */
public record NameCandidate(UUID personId, String fullName, List<String> homeSabhas) {
}
