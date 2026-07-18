package org.sabha.identity.applicationservice.appointment;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.CallerResolver;
import org.sabha.common.ConflictException;
import org.sabha.common.SabhaScope;
import org.sabha.common.StructuralHierarchyLookup;
import org.sabha.identity.applicationservice.IdentityProviderGateway;
import org.sabha.identity.applicationservice.UserRepository;
import org.sabha.identity.domain.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Role revocation (ADR-0025 §1-2, ADR-0026): a state change authorized by the
 * actor's current scope — not by who made the assignment — guarded by the
 * Regional Team last-one-out rule and withdrawing login when the holder's last
 * active role goes. The assignment row and its appointing audit always survive,
 * and revocation never cascades to the holder's appointees.
 */
class RoleRevocationServiceTest {

    private static final String YUVAK = "YUVAK";

    private static final UUID NIRDESHAK_SUBJECT = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
    private static final UUID NIRDESHAK = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID OUTSIDER_SUBJECT = UUID.fromString("00000000-0000-0000-0000-0000000000f9");
    private static final UUID OUTSIDER = UUID.fromString("00000000-0000-0000-0000-0000000000a9");
    private static final UUID RT_PEER_SUBJECT = UUID.fromString("00000000-0000-0000-0000-0000000000f2");
    private static final UUID RT_PEER = UUID.fromString("00000000-0000-0000-0000-0000000000a2");

    private static final UUID SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID KSHETRA = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID CITY = UUID.fromString("00000000-0000-0000-0000-0000000000d1");

    private static final Instant NOW = Instant.parse("2026-06-26T10:15:30Z");

    @Test
    void aScopeHolderRevokesARoleRecordingTheRevoker() {
        Fixture f = new Fixture();
        // The Sanchalak was appointed by someone else entirely; authority is by scope.
        UUID assignment = f.activeSabhaRole(AppointableRole.SANCHALAK, SABHA, f.user("Sanchalak"));

        f.service().revoke(NIRDESHAK_SUBJECT, assignment);

        assertThat(f.assignments.revokedId).isEqualTo(assignment);
        assertThat(f.assignments.revokedBy).isEqualTo(NIRDESHAK);
        assertThat(f.assignments.revokedAt).isEqualTo(NOW);
    }

    @Test
    void anOutOfScopeActorIsDeniedAndNothingIsRevoked() {
        Fixture f = new Fixture();
        UUID assignment = f.activeSabhaRole(AppointableRole.SANCHALAK, SABHA, f.user("Sanchalak"));

        assertThatThrownBy(() -> f.service().revoke(OUTSIDER_SUBJECT, assignment))
                .isInstanceOf(AuthorizationDeniedException.class);

        assertThat(f.assignments.revokedId).isNull();
    }

    @Test
    void revokingAnUnknownOrAlreadyRevokedAssignmentIsNotFound() {
        Fixture f = new Fixture();

        assertThatThrownBy(() -> f.service().revoke(NIRDESHAK_SUBJECT, UUID.randomUUID()))
                .isInstanceOf(RoleAssignmentNotFoundException.class);

        assertThat(f.assignments.revokedId).isNull();
    }

    @Test
    void revokingTheLastRegionalTeamMemberOfACityDemographicIsRejected() {
        Fixture f = new Fixture();
        UUID assignment = f.activeCityRole(AppointableRole.REGIONAL_TEAM, CITY, YUVAK, f.user("Last RT"));
        f.assignments.regionalTeamCount = 1;

        assertThatThrownBy(() -> f.service().revoke(RT_PEER_SUBJECT, assignment))
                .isInstanceOf(LastRegionalTeamMemberException.class)
                .satisfies(e -> assertThat(((ConflictException) e).code()).isEqualTo("LAST_REGIONAL_TEAM_MEMBER"));

        assertThat(f.assignments.revokedId).isNull();
    }

    @Test
    void revokingANonLastRegionalTeamMemberSucceeds() {
        Fixture f = new Fixture();
        UUID assignment = f.activeCityRole(AppointableRole.REGIONAL_TEAM, CITY, YUVAK, f.user("Departing RT"));
        f.assignments.regionalTeamCount = 2;

        f.service().revoke(RT_PEER_SUBJECT, assignment);

        assertThat(f.assignments.revokedId).isEqualTo(assignment);
    }

    @Test
    void aUserLosingTheirLastActiveRoleLosesLogin() {
        Fixture f = new Fixture();
        UUID user = f.user("Only One Role");
        UUID assignment = f.activeSabhaRole(AppointableRole.SANCHALAK, SABHA, user);
        f.assignments.remainingActiveRolesFor.put(user, 0);

        f.service().revoke(NIRDESHAK_SUBJECT, assignment);

        assertThat(f.identityProvider.disabled).containsExactly(f.keycloakOf(user));
    }

    @Test
    void aUserKeepingAnotherActiveRoleRetainsLogin() {
        Fixture f = new Fixture();
        UUID user = f.user("Two Roles");
        UUID assignment = f.activeSabhaRole(AppointableRole.SANCHALAK, SABHA, user);
        f.assignments.remainingActiveRolesFor.put(user, 1);

        f.service().revoke(NIRDESHAK_SUBJECT, assignment);

        assertThat(f.identityProvider.disabled).isEmpty();
    }

    private static final class Fixture {
        final FakeCallerResolver callers = new FakeCallerResolver();
        final FakeHierarchy hierarchy = new FakeHierarchy();
        final FakeAuthority authority = new FakeAuthority();
        final FakeRevokableRoleAssignments assignments = new FakeRevokableRoleAssignments();
        final FakeUserRepository users = new FakeUserRepository();
        final FakeIdentityProvider identityProvider = new FakeIdentityProvider();
        final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        Fixture() {
            callers.map.put(NIRDESHAK_SUBJECT, NIRDESHAK);
            callers.map.put(OUTSIDER_SUBJECT, OUTSIDER);
            callers.map.put(RT_PEER_SUBJECT, RT_PEER);
            hierarchy.sabhaScopes.put(SABHA, new SabhaScope(KSHETRA, YUVAK, "REGULAR"));
            authority.nirdeshakScopes.add(NIRDESHAK + "|" + KSHETRA + "|" + YUVAK);
            authority.regionalTeamScopes.add(RT_PEER + "|" + CITY + "|" + YUVAK);
        }

        UUID user(String name) {
            UUID userId = UUID.randomUUID();
            users.byId.put(userId, new User(userId, UUID.randomUUID(), name, UUID.randomUUID()));
            return userId;
        }

        UUID keycloakOf(UUID userId) {
            return users.byId.get(userId).keycloakUserId();
        }

        UUID activeSabhaRole(AppointableRole role, UUID sabhaId, UUID userId) {
            return register(userId, AppointmentScope.onSabha(role, sabhaId));
        }

        UUID activeCityRole(AppointableRole role, UUID cityId, String demographic, UUID userId) {
            return register(userId, AppointmentScope.onCity(role, cityId, demographic));
        }

        private UUID register(UUID userId, AppointmentScope scope) {
            UUID id = UUID.randomUUID();
            assignments.active.put(id, new RevokableAssignment(id, userId, scope));
            return id;
        }

        RoleRevocationService service() {
            AppointmentAuthorization authz = new AppointmentAuthorization(
                    hierarchy, authority, userId -> false);
            return new RoleRevocationService(
                    callers, authz, assignments, users, identityProvider, clock);
        }
    }

    private static final class FakeCallerResolver implements CallerResolver {
        final Map<UUID, UUID> map = new HashMap<>();

        @Override
        public Optional<UUID> resolveUserId(UUID keycloakSubject) {
            return Optional.ofNullable(map.get(keycloakSubject));
        }
    }

    private static final class FakeHierarchy implements StructuralHierarchyLookup {
        final Map<UUID, SabhaScope> sabhaScopes = new HashMap<>();

        @Override
        public Optional<SabhaScope> sabhaScope(UUID sabhaId) {
            return Optional.ofNullable(sabhaScopes.get(sabhaId));
        }

        @Override
        public boolean isSabhaKindRetired(UUID sabhaId) {
            return false;
        }

        @Override
        public Optional<UUID> zoneOfKshetra(UUID kshetraId) {
            return Optional.empty();
        }

        @Override
        public Optional<UUID> cityOfZone(UUID zoneId) {
            return Optional.empty();
        }
    }

    private static final class FakeAuthority implements AppointerAuthorityLookup {
        final Set<String> nirdeshakScopes = new HashSet<>();
        final Set<String> regionalTeamScopes = new HashSet<>();

        @Override
        public boolean holdsNirdeshak(UUID userId, UUID kshetraId, String demographic) {
            return nirdeshakScopes.contains(userId + "|" + kshetraId + "|" + demographic);
        }

        @Override
        public boolean holdsSanyojak(UUID userId, UUID zoneId, String demographic) {
            return false;
        }

        @Override
        public boolean holdsRegionalTeam(UUID userId, UUID cityId, String demographic) {
            return regionalTeamScopes.contains(userId + "|" + cityId + "|" + demographic);
        }
    }

    private static final class FakeRevokableRoleAssignments implements RevokableRoleAssignments {
        final Map<UUID, RevokableAssignment> active = new HashMap<>();
        final Map<UUID, Integer> remainingActiveRolesFor = new HashMap<>();
        int regionalTeamCount;
        UUID revokedId;
        UUID revokedBy;
        Instant revokedAt;

        @Override
        public Optional<RevokableAssignment> findActive(UUID assignmentId) {
            return Optional.ofNullable(active.get(assignmentId));
        }

        @Override
        public void markRevoked(UUID assignmentId, UUID revokedBy, Instant revokedAt) {
            this.revokedId = assignmentId;
            this.revokedBy = revokedBy;
            this.revokedAt = revokedAt;
            this.active.remove(assignmentId);
        }

        @Override
        public int activeRegionalTeamCount(UUID cityId, String demographic) {
            return regionalTeamCount;
        }

        @Override
        public int activeRoleCountForUser(UUID userId) {
            return remainingActiveRolesFor.getOrDefault(userId, 1);
        }
    }

    private static final class FakeUserRepository implements UserRepository {
        final Map<UUID, User> byId = new HashMap<>();

        @Override
        public Optional<User> findByKeycloakUserId(UUID keycloakUserId) {
            return byId.values().stream().filter(u -> u.keycloakUserId().equals(keycloakUserId)).findFirst();
        }

        @Override
        public Optional<User> findByPersonId(UUID personId) {
            return byId.values().stream().filter(u -> u.personId().equals(personId)).findFirst();
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return byId.values().stream().filter(u -> u.username().equals(username)).findFirst();
        }

        @Override
        public Optional<User> findById(UUID userId) {
            return Optional.ofNullable(byId.get(userId));
        }

        @Override
        public boolean existsByUsername(String username) {
            return byId.values().stream().anyMatch(u -> u.username().equals(username));
        }

        @Override
        public void save(User user) {
            byId.put(user.id(), user);
        }
    }

    private static final class FakeIdentityProvider implements IdentityProviderGateway {
        final List<UUID> disabled = new ArrayList<>();

        @Override
        public UUID createUserRequiringPasswordChange(String username, String rawPassword) {
            throw new UnsupportedOperationException("not used by revocation");
        }

        @Override
        public void resetPassword(UUID keycloakUserId, String rawPassword, boolean requirePasswordChange) {
            throw new UnsupportedOperationException("not used by revocation");
        }

        @Override
        public void disableUser(UUID keycloakUserId) {
            disabled.add(keycloakUserId);
        }
    }
}
