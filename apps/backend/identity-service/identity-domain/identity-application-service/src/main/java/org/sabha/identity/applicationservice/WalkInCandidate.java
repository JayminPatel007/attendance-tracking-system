package org.sabha.identity.applicationservice;

import java.util.UUID;

/**
 * A Directory match offered when recording a Walk-in (Slice 7): a Person the
 * Sanchalak might be registering as attending a Sabha that is not one of their
 * Home Sabhas. Carries the Person's current {@code homeSabha} so the confirm
 * sheet can show where they normally belong (now away) before queuing the
 * Walk-in.
 */
public record WalkInCandidate(UUID personId, String fullName, String homeSabha) {
}
