package org.sabha.analytics.applicationservice;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.sabha.common.SantLookup;

/**
 * In-memory {@link SantLookup} shared by the three classes ADR-0032 split
 * {@code DashboardAccess} into — the engine, the City command and the chip query
 * all turn on the same Sant test.
 */
final class FakeSantLookup implements SantLookup {

    private final Set<UUID> sants = new HashSet<>();

    void add(UUID userId) {
        sants.add(userId);
    }

    @Override
    public boolean isSant(UUID userId) {
        return sants.contains(userId);
    }
}
