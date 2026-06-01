package org.sabha.identity.applicationservice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.identity.domain.Gender;
import org.sabha.identity.domain.Person;
import org.sabha.identity.domain.User;

import static org.assertj.core.api.Assertions.assertThat;

class MkBootstrapServiceTest {

    private static final UUID KEYCLOAK_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final String FULL_NAME = "Bootstrap MK Member";
    private static final String MOBILE = "+919820111222";
    private static final String USERNAME = "mk-admin";
    private static final String PASSWORD = "changeme123!";

    @Test
    void seedsTheFirstMkMemberInKeycloakAndLocallyWhenNoMemberExists() {
        Fixture f = new Fixture();

        f.service().ensureBootstrapMember(command());

        // Keycloak user created with a forced first-login password change.
        assertThat(f.keycloak.createdUsername).isEqualTo(USERNAME);
        assertThat(f.keycloak.createdPassword).isEqualTo(PASSWORD);

        // A local Person was created carrying the supplied details.
        assertThat(f.persons.saved).hasSize(1);
        Person person = f.persons.saved.get(0);
        assertThat(person.fullName()).isEqualTo(FULL_NAME);
        assertThat(person.gender()).isEqualTo(Gender.MALE);
        assertThat(person.mobile()).isEqualTo(MOBILE);

        // A local User links that Person to the Keycloak identity.
        assertThat(f.users.saved).hasSize(1);
        User user = f.users.saved.get(0);
        assertThat(user.username()).isEqualTo(USERNAME);
        assertThat(user.personId()).isEqualTo(person.id());
        assertThat(user.keycloakUserId()).isEqualTo(KEYCLOAK_ID);

        // The User holds Madhyastha Karyalaya membership.
        assertThat(f.membership.granted).containsExactly(user.id());
    }

    @Test
    void doesNothingWhenAnMkMemberAlreadyExists() {
        Fixture f = new Fixture();
        f.membership.exists = true;

        f.service().ensureBootstrapMember(command());

        assertThat(f.keycloak.createdUsername).isNull();
        assertThat(f.persons.saved).isEmpty();
        assertThat(f.users.saved).isEmpty();
        assertThat(f.membership.granted).isEmpty();
    }

    private static BootstrapMkCommand command() {
        return new BootstrapMkCommand(FULL_NAME, Gender.MALE, MOBILE, USERNAME, PASSWORD);
    }

    private static final class Fixture {
        final FakeKeycloakAdminClient keycloak = new FakeKeycloakAdminClient();
        final FakePersonRepository persons = new FakePersonRepository();
        final FakeUserRepository users = new FakeUserRepository();
        final FakeMembership membership = new FakeMembership();

        MkBootstrapService service() {
            return new MkBootstrapService(keycloak, persons, users, membership);
        }
    }

    private static final class FakeKeycloakAdminClient implements KeycloakAdminClient {
        String createdUsername;
        String createdPassword;

        @Override
        public UUID createUserRequiringPasswordChange(String username, String rawPassword) {
            this.createdUsername = username;
            this.createdPassword = rawPassword;
            return KEYCLOAK_ID;
        }
    }

    private static final class FakePersonRepository implements PersonRepository {
        final List<Person> saved = new ArrayList<>();

        @Override
        public void save(Person person) {
            saved.add(person);
        }
    }

    private static final class FakeUserRepository implements UserRepository {
        final List<User> saved = new ArrayList<>();

        @Override
        public Optional<User> findByKeycloakUserId(UUID keycloakUserId) {
            return saved.stream().filter(u -> u.keycloakUserId().equals(keycloakUserId)).findFirst();
        }

        @Override
        public void save(User user) {
            saved.add(user);
        }
    }

    private static final class FakeMembership implements MadhyasthaKaryalayaMembership {
        final Set<UUID> granted = new HashSet<>();
        boolean exists = false;

        @Override
        public boolean anyMemberExists() {
            return exists;
        }

        @Override
        public void grantTo(UUID userId) {
            granted.add(userId);
        }

        @Override
        public boolean isMember(UUID userId) {
            return granted.contains(userId);
        }
    }
}
