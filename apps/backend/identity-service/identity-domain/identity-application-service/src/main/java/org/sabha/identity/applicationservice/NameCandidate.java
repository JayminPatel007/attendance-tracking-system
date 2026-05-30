package org.sabha.identity.applicationservice;

import java.util.UUID;

/**
 * A possible duplicate surfaced by the name soft-warn (ADR-0013): an existing
 * Person whose name is phonetically or edit-distance close to the one being
 * added, within the same geographic scope. Carries just enough for the adder to
 * recognise the Person on the add screen.
 */
public record NameCandidate(UUID personId, String fullName, String homeSabhaName) {
}
