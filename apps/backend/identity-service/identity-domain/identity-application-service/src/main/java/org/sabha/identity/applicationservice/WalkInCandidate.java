package org.sabha.identity.applicationservice;

import java.util.List;
import java.util.UUID;

/**
 * A Directory match offered when recording a Walk-in (Slice 7): a Person the
 * Sanchalak might be registering as attending a Sabha that is not one of their
 * Home Sabhas. Carries all of the Person's current Home Sabha kinds (a Person has
 * one per kind they qualify for, CONTEXT.md) so the confirm sheet can show where
 * they normally belong (now away) — the demographic Sabha, not just whichever
 * kind sorts first — before queuing the Walk-in.
 */
public record WalkInCandidate(UUID personId, String fullName, List<String> homeSabhas) {
}
