package org.sabha.analytics.applicationservice;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.MadhyasthaKaryalayaLookup;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code AuditReadAccess} seam the identity web shell consults to gate the
 * Audit-log section (issue #80). It must answer "yes" for exactly the callers
 * {@link AuditLogAccess} resolves to a non-{@code Denied} scope and "no" for the
 * rest, so the sidebar can never drift from the BFF engine.
 */
class AuditReadAccessAdapterTest {

    private static final UUID MK = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID REGIONAL_TEAM = UUID.fromString("00000000-0000-0000-0000-0000000000a6");
    private static final UUID SANCHALAK = UUID.fromString("00000000-0000-0000-0000-0000000000a5");
    private static final UUID CITY = UUID.fromString("00000000-0000-0000-0000-0000000000b3");

    private final FakeMk mk = new FakeMk();
    private final FakeScopes scopes = new FakeScopes();
    private final AuditLogAccess access = new AuditLogAccess(mk, userId -> false, scopes);
    private final AuditReadAccessAdapter adapter = new AuditReadAccessAdapter(access);

    @Test
    void admitsAnUnrestrictedOversightBody() {
        mk.add(MK);

        assertThat(adapter.canRead(MK)).isTrue();
    }

    @Test
    void admitsAScopedRegionalTeamMember() {
        scopes.put(REGIONAL_TEAM, new AuditScope.Scoped(Set.of(), Set.of(), Set.of(CITY)));

        assertThat(adapter.canRead(REGIONAL_TEAM)).isTrue();
    }

    @Test
    void deniesACallerWhoResolvesToNoScope() {
        assertThat(adapter.canRead(SANCHALAK)).isFalse();
    }

    private static final class FakeMk implements MadhyasthaKaryalayaLookup {
        private final java.util.Set<UUID> members = new java.util.HashSet<>();

        void add(UUID id) {
            members.add(id);
        }

        @Override
        public boolean isMember(UUID userId) {
            return members.contains(userId);
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
