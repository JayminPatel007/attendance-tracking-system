package org.sabha.identity.applicationservice;

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
import org.sabha.common.SabhaScope;
import org.sabha.common.StructuralHierarchyLookup;
import org.sabha.identity.domain.Gender;
import org.sabha.identity.domain.MobileAlreadyRegisteredException;
import org.sabha.identity.domain.Person;
import org.sabha.identity.domain.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The one-transaction role-appointment flow (ADR-0011): resolve-or-create the
 * Person (Slice 6 de-dup), create User credentials only when the Person is not
 * already a User, and record the RoleAssignment with its appointing audit. The
 * Authorization Engine gates the whole act.
 */
class RoleAppointmentServiceTest {

    private static final String YUVAK = "YUVAK";

    private static final UUID NIRDESHAK_SUBJECT = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
    private static final UUID NIRDESHAK = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID OUTSIDER_SUBJECT = UUID.fromString("00000000-0000-0000-0000-0000000000f9");
    private static final UUID OUTSIDER = UUID.fromString("00000000-0000-0000-0000-0000000000a9");
    private static final UUID SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID KSHETRA = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID HOME_SABHA = SABHA;

    private static final Instant NOW = Instant.parse("2026-06-03T10:15:30Z");

    @Test
    void appointingAnExistingUserOnlyRecordsTheRoleAssignment() {
        Fixture f = new Fixture();
        UUID personId = f.existingUser("Existing Karyakar");

        AppointmentResult result = f.service().appoint(NIRDESHAK_SUBJECT,
                RoleAppointmentCommand.forExistingPerson(
                        AppointmentScope.onSabha(AppointableRole.SANCHALAK, SABHA),
                        personId, "anything", "anything"));

        // Only a RoleAssignment row was written — no new Person, User, or credentials.
        assertThat(f.identityProvider.createdUsername).isNull();
        assertThat(f.users.savedSinceStart()).isEmpty();
        assertThat(f.appointments.saved).hasSize(1);

        RoleAppointmentRow row = f.appointments.saved.get(0);
        assertThat(row.userId()).isEqualTo(f.userIdOf(personId));
        assertThat(row.scope().role()).isEqualTo(AppointableRole.SANCHALAK);
        assertThat(row.scope().sabhaId()).isEqualTo(SABHA);
        assertThat(row.appointedBy()).isEqualTo(NIRDESHAK);
        assertThat(row.appointedAt()).isEqualTo(NOW);

        assertThat(result.appointed()).isTrue();
        assertThat(result.personId()).isEqualTo(personId);
    }

    @Test
    void appointingAnExistingNonUserCreatesCredentialsAndTheRoleAssignment() {
        Fixture f = new Fixture();
        UUID personId = f.existingPersonWithoutUser("Plain Person", "+919820000001");

        AppointmentResult result = f.service().appoint(NIRDESHAK_SUBJECT,
                RoleAppointmentCommand.forExistingPerson(
                        AppointmentScope.onSabha(AppointableRole.SANCHALAK, SABHA),
                        personId, "new-sanchalak", "TempPass1!"));

        assertThat(f.identityProvider.createdUsername).isEqualTo("new-sanchalak");
        assertThat(f.identityProvider.createdPassword).isEqualTo("TempPass1!");
        assertThat(f.users.savedSinceStart()).hasSize(1);
        User created = f.users.savedSinceStart().get(0);
        assertThat(created.personId()).isEqualTo(personId);
        assertThat(f.appointments.saved).hasSize(1);
        assertThat(f.appointments.saved.get(0).userId()).isEqualTo(created.id());
        assertThat(result.appointed()).isTrue();
    }

    @Test
    void appointingANewPersonCreatesPersonHomeSabhaUserAndRoleAssignmentInOneFlow() {
        Fixture f = new Fixture();
        f.users.markStart();
        AddPersonCommand newPerson = new AddPersonCommand(
                "Brand New", Gender.MALE, null, "+919820000123", null, HOME_SABHA, false);

        AppointmentResult result = f.service().appoint(NIRDESHAK_SUBJECT,
                RoleAppointmentCommand.forNewPerson(
                        AppointmentScope.onSabha(AppointableRole.SANCHALAK, SABHA),
                        newPerson, "brand-new", "TempPass1!"));

        // Person created with the initial Home Sabha (OTP skipped — the appointer creates them now).
        assertThat(f.directory.added).hasSize(1);
        assertThat(f.directory.addedHomeSabha).containsExactly(HOME_SABHA);
        // Credentials created and a role assignment recorded, all referring to the new Person.
        assertThat(f.identityProvider.createdUsername).isEqualTo("brand-new");
        assertThat(f.users.savedSinceStart()).hasSize(1);
        assertThat(f.appointments.saved).hasSize(1);
        assertThat(result.appointed()).isTrue();
        assertThat(result.personId()).isEqualTo(f.directory.added.get(0).id());
    }

    @Test
    void inlineCreateWithACollidingMobileIsHardBlockedAndNothingIsCreated() {
        Fixture f = new Fixture();
        f.existingPersonWithoutUser("Mobile Owner", "+919820000999");
        f.users.markStart();
        AddPersonCommand dupe = new AddPersonCommand(
                "Imposter", Gender.MALE, null, "+919820000999", null, HOME_SABHA, false);

        assertThatThrownBy(() -> f.service().appoint(NIRDESHAK_SUBJECT,
                RoleAppointmentCommand.forNewPerson(
                        AppointmentScope.onSabha(AppointableRole.SANCHALAK, SABHA),
                        dupe, "imposter", "x")))
                .isInstanceOf(MobileAlreadyRegisteredException.class);

        assertThat(f.users.savedSinceStart()).isEmpty();
        assertThat(f.identityProvider.createdUsername).isNull();
        assertThat(f.appointments.saved).isEmpty();
    }

    @Test
    void aUsernameCollisionIsRejectedBeforeCommit() {
        Fixture f = new Fixture();
        UUID personId = f.existingPersonWithoutUser("Needs Login", "+919820000777");
        f.users.usernames.add("taken");
        f.users.markStart();

        assertThatThrownBy(() -> f.service().appoint(NIRDESHAK_SUBJECT,
                RoleAppointmentCommand.forExistingPerson(
                        AppointmentScope.onSabha(AppointableRole.SANCHALAK, SABHA),
                        personId, "taken", "x")))
                .isInstanceOf(UsernameAlreadyTakenException.class);

        assertThat(f.identityProvider.createdUsername).isNull();
        assertThat(f.users.savedSinceStart()).isEmpty();
        assertThat(f.appointments.saved).isEmpty();
    }

    @Test
    void anUnauthorizedAppointerIsDeniedAndNothingIsCreated() {
        Fixture f = new Fixture();
        UUID personId = f.existingUser("Whoever");

        assertThatThrownBy(() -> f.service().appoint(OUTSIDER_SUBJECT,
                RoleAppointmentCommand.forExistingPerson(
                        AppointmentScope.onSabha(AppointableRole.SANCHALAK, SABHA),
                        personId, "x", "x")))
                .isInstanceOf(AuthorizationDeniedException.class);

        assertThat(f.appointments.saved).isEmpty();
    }

    @Test
    void aNameSoftWarnShortCircuitsAppointmentWithoutCreatingAnything() {
        Fixture f = new Fixture();
        f.users.markStart();
        f.directory.nameCandidates.add(new NameCandidate(UUID.randomUUID(), "Brand Neue", List.of()));
        AddPersonCommand newPerson = new AddPersonCommand(
                "Brand New", Gender.MALE, null, "+919820000123", null, HOME_SABHA, false);

        AppointmentResult result = f.service().appoint(NIRDESHAK_SUBJECT,
                RoleAppointmentCommand.forNewPerson(
                        AppointmentScope.onSabha(AppointableRole.SANCHALAK, SABHA),
                        newPerson, "brand-new", "x"));

        assertThat(result.softWarned()).isTrue();
        assertThat(result.candidates()).hasSize(1);
        assertThat(f.directory.added).isEmpty();
        assertThat(f.identityProvider.createdUsername).isNull();
        assertThat(f.appointments.saved).isEmpty();
    }

    private static final class Fixture {
        final FakeCallerResolver callers = new FakeCallerResolver();
        final FakeHierarchy hierarchy = new FakeHierarchy();
        final FakeAuthority authority = new FakeAuthority();
        final FakePersonDirectory directory = new FakePersonDirectory();
        final FakeUserRepository users = new FakeUserRepository();
        final FakeIdentityProvider identityProvider = new FakeIdentityProvider();
        final FakeAppointments appointments = new FakeAppointments();
        final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        Fixture() {
            callers.map.put(NIRDESHAK_SUBJECT, NIRDESHAK);
            callers.map.put(OUTSIDER_SUBJECT, OUTSIDER);
            hierarchy.sabhaScopes.put(SABHA, new SabhaScope(KSHETRA, YUVAK, "REGULAR"));
            authority.nirdeshakScopes.add(NIRDESHAK + "|" + KSHETRA + "|" + YUVAK);
            directory.kshetraOfSabha.put(SABHA, KSHETRA);
        }

        UUID existingUser(String name) {
            UUID personId = existingPersonWithoutUser(name, "+9100" + UUID.randomUUID().toString().substring(0, 6));
            UUID userId = UUID.randomUUID();
            users.byPersonId.put(personId, new User(userId, personId, "existing-user", UUID.randomUUID()));
            users.markStart();
            return personId;
        }

        UUID existingPersonWithoutUser(String name, String mobile) {
            UUID personId = UUID.randomUUID();
            Person person = Person.create(personId, name, Gender.MALE, null, mobile, null);
            directory.persons.put(personId, person);
            directory.byMobile.put(mobile, person);
            users.markStart();
            return personId;
        }

        UUID userIdOf(UUID personId) {
            return users.byPersonId.get(personId).id();
        }

        RoleAppointmentService service() {
            AppointmentAuthorization authz = new AppointmentAuthorization(
                    hierarchy, authority, userId -> false);
            AddPersonApplicationService addPerson = new AddPersonApplicationService(
                    callers, directory, events -> { }, clock);
            return new RoleAppointmentService(
                    callers, authz, addPerson, users, identityProvider, appointments, clock);
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
            return false;
        }
    }

    private static final class FakePersonDirectory implements PersonDirectory {
        final Map<UUID, Person> persons = new HashMap<>();
        final Map<String, Person> byMobile = new HashMap<>();
        final Map<UUID, UUID> kshetraOfSabha = new HashMap<>();
        final List<Person> added = new ArrayList<>();
        final List<UUID> addedHomeSabha = new ArrayList<>();
        final List<NameCandidate> nameCandidates = new ArrayList<>();

        @Override
        public Optional<Person> findByMobile(String mobile) {
            return Optional.ofNullable(byMobile.get(mobile));
        }

        @Override
        public Optional<Person> findById(UUID personId) {
            return Optional.ofNullable(persons.get(personId));
        }

        @Override
        public List<NameCandidate> findNameCandidates(UUID kshetraId, String fullName, int limit) {
            return List.copyOf(nameCandidates);
        }

        @Override
        public Optional<WalkInCandidate> findByMobileForWalkIn(String mobile) {
            return Optional.empty();
        }

        @Override
        public Optional<UUID> kshetraIdOfSabha(UUID sabhaId) {
            return Optional.ofNullable(kshetraOfSabha.get(sabhaId));
        }

        @Override
        public void add(Person person, UUID homeSabhaId) {
            persons.put(person.id(), person);
            added.add(person);
            addedHomeSabha.add(homeSabhaId);
            if (person.mobile() != null) {
                byMobile.put(person.mobile(), person);
            }
        }
    }

    private static final class FakeUserRepository implements UserRepository {
        final Map<UUID, User> byPersonId = new HashMap<>();
        final Set<String> usernames = new HashSet<>();
        final List<User> saved = new ArrayList<>();
        int startMark;

        void markStart() {
            startMark = saved.size();
        }

        List<User> savedSinceStart() {
            return saved.subList(startMark, saved.size());
        }

        @Override
        public Optional<User> findByKeycloakUserId(UUID keycloakUserId) {
            return saved.stream().filter(u -> u.keycloakUserId().equals(keycloakUserId)).findFirst();
        }

        @Override
        public Optional<User> findByPersonId(UUID personId) {
            return Optional.ofNullable(byPersonId.get(personId));
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return byPersonId.values().stream()
                    .filter(u -> u.username().equals(username)).findFirst();
        }

        @Override
        public Optional<User> findById(UUID userId) {
            return saved.stream().filter(u -> u.id().equals(userId)).findFirst();
        }

        @Override
        public boolean existsByUsername(String username) {
            return usernames.contains(username)
                    || byPersonId.values().stream().anyMatch(u -> u.username().equals(username));
        }

        @Override
        public void save(User user) {
            saved.add(user);
            byPersonId.put(user.personId(), user);
            usernames.add(user.username());
        }
    }

    private static final class FakeIdentityProvider implements IdentityProviderGateway {
        String createdUsername;
        String createdPassword;
        final UUID newKeycloakId = UUID.randomUUID();

        @Override
        public UUID createUserRequiringPasswordChange(String username, String rawPassword) {
            this.createdUsername = username;
            this.createdPassword = rawPassword;
            return newKeycloakId;
        }

        @Override
        public void resetPassword(UUID keycloakUserId, String rawPassword, boolean requirePasswordChange) {
            throw new UnsupportedOperationException("not used by role appointment");
        }
    }

    private static final class FakeAppointments implements RoleAppointmentRepository {
        final List<RoleAppointmentRow> saved = new ArrayList<>();

        @Override
        public void save(RoleAppointmentRow row) {
            saved.add(row);
        }
    }
}
