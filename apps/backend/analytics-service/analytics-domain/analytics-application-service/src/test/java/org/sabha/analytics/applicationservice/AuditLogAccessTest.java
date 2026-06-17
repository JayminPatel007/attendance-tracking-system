package org.sabha.analytics.applicationservice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.MadhyasthaKaryalayaLookup;
import org.sabha.common.SantLookup;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit behaviours of the audit-log Authorization Engine (ADR-0023, Slice 19): the
 * State-wide oversight bodies read everything, the Kshetra/Zone/City tiers read
 * their resolved geography, and every tier below Nirdeshak is denied. Exercised
 * DB-free through fakes for the three ports.
 */
class AuditLogAccessTest {

    private static final UUID MK = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID SANT = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID NIRDESHAK = UUID.fromString("00000000-0000-0000-0000-0000000000a3");
    private static final UUID SANYOJAK = UUID.fromString("00000000-0000-0000-0000-0000000000a4");
    private static final UUID SANCHALAK = UUID.fromString("00000000-0000-0000-0000-0000000000a5");
    private static final UUID KSHETRA = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID ZONE = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    private final FakeMk mk = new FakeMk();
    private final FakeSants sants = new FakeSants();
    private final FakeScopes scopes = new FakeScopes();
    private final AuditLogAccess access = new AuditLogAccess(mk, sants, scopes);

    @Test
    void aMadhyasthaKaryalayaMemberReadsTheWholeState() {
        mk.add(MK);

        assertThat(access.scopeFor(MK)).isEqualTo(new AuditScope.Unrestricted());
    }

    @Test
    void aSantReadsTheWholeStateViaTheUniversalReadException() {
        sants.add(SANT);

        assertThat(access.scopeFor(SANT)).isEqualTo(new AuditScope.Unrestricted());
    }

    @Test
    void aNirdeshakReadsTheirResolvedKshetraScope() {
        scopes.put(NIRDESHAK, new AuditScope.Scoped(Set.of(KSHETRA), Set.of(), Set.of()));

        assertThat(access.scopeFor(NIRDESHAK))
                .isEqualTo(new AuditScope.Scoped(Set.of(KSHETRA), Set.of(), Set.of()));
    }

    @Test
    void aSanyojakReadsTheirResolvedZoneScope() {
        scopes.put(SANYOJAK, new AuditScope.Scoped(Set.of(), Set.of(ZONE), Set.of()));

        assertThat(access.scopeFor(SANYOJAK))
                .isEqualTo(new AuditScope.Scoped(Set.of(), Set.of(ZONE), Set.of()));
    }

    @Test
    void aTierBelowNirdeshakResolvesToNoScopeAndIsDenied() {
        // A Sanchalak holds no Nirdeshak-and-above geographic row, so the lookup
        // returns empty sets and the engine folds that to Denied.
        scopes.put(SANCHALAK, new AuditScope.Scoped(Set.of(), Set.of(), Set.of()));

        assertThat(access.scopeFor(SANCHALAK)).isEqualTo(new AuditScope.Denied());
    }

    @Test
    void anUnknownCallerIsDenied() {
        assertThat(access.scopeFor(UUID.randomUUID())).isEqualTo(new AuditScope.Denied());
    }

    private static final class FakeMk implements MadhyasthaKaryalayaLookup {
        private final Set<UUID> members = new HashSet<>();

        void add(UUID id) {
            members.add(id);
        }

        @Override
        public boolean isMember(UUID userId) {
            return members.contains(userId);
        }
    }

    private static final class FakeSants implements SantLookup {
        private final Set<UUID> sants = new HashSet<>();

        void add(UUID id) {
            sants.add(id);
        }

        @Override
        public boolean isSant(UUID userId) {
            return sants.contains(userId);
        }
    }

    private static final class FakeScopes implements AuditScopeLookup {
        private final Map<UUID, AuditScope.Scoped> byUser = new HashMap<>();

        void put(UUID id, AuditScope.Scoped scope) {
            byUser.put(id, scope);
        }

        @Override
        public AuditScope.Scoped scopeOf(UUID userId) {
            return byUser.getOrDefault(userId, new AuditScope.Scoped(Set.of(), Set.of(), Set.of()));
        }
    }
}
