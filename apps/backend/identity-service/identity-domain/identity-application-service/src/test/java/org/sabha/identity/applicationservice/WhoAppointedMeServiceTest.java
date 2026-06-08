package org.sabha.identity.applicationservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.identity.domain.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WhoAppointedMeServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final UUID PERSON_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e2");
    private static final UUID KEYCLOAK_USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e3");
    private static final String USERNAME = "lost.user";

    @Test
    void lookupReturnsTheContactDetailsOfTheUsersAppointer() {
        Fixture f = new Fixture();
        f.users.seed(new User(USER_ID, PERSON_ID, USERNAME, KEYCLOAK_USER_ID));
        f.contacts.seedAppointers(USER_ID, List.of(
                new AppointerContact("Suresh Patel", "+919820111222")));

        List<AppointerContact> result = f.service().lookup(USERNAME);

        assertThat(result).containsExactly(new AppointerContact("Suresh Patel", "+919820111222"));
    }

    @Test
    void lookupForASantWithNoAppointerReturnsMadhyasthaKaryalayaContacts() {
        Fixture f = new Fixture();
        f.users.seed(new User(USER_ID, PERSON_ID, USERNAME, KEYCLOAK_USER_ID));
        // No appointer recorded (Sants have none per ADR-0011).
        f.contacts.seedMadhyasthaKaryalaya(List.of(
                new AppointerContact("MK Coordinator", "+919820999000")));

        List<AppointerContact> result = f.service().lookup(USERNAME);

        assertThat(result).containsExactly(new AppointerContact("MK Coordinator", "+919820999000"));
    }

    @Test
    void lookupForAnUnknownUsernameIsRejected() {
        Fixture f = new Fixture();

        assertThatThrownBy(() -> f.service().lookup("nobody"))
                .isInstanceOf(UnknownUsernameException.class);
    }

    // ---- test fixtures -------------------------------------------------------

    static final class Fixture {
        final InMemoryUserRepository users = new InMemoryUserRepository();
        final InMemoryAppointerContactLookup contacts = new InMemoryAppointerContactLookup();

        WhoAppointedMeService service() {
            return new WhoAppointedMeService(users, contacts);
        }
    }

    static final class InMemoryUserRepository implements UserRepository {
        private final Map<UUID, User> byId = new HashMap<>();

        void seed(User user) {
            byId.put(user.id(), user);
        }

        @Override
        public Optional<User> findByKeycloakUserId(UUID keycloakUserId) {
            return byId.values().stream().filter(u -> keycloakUserId.equals(u.keycloakUserId())).findFirst();
        }

        @Override
        public Optional<User> findByPersonId(UUID personId) {
            return byId.values().stream().filter(u -> personId.equals(u.personId())).findFirst();
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return byId.values().stream().filter(u -> username.equals(u.username())).findFirst();
        }

        @Override
        public Optional<User> findById(UUID userId) {
            return Optional.ofNullable(byId.get(userId));
        }

        @Override
        public boolean existsByUsername(String username) {
            return findByUsername(username).isPresent();
        }

        @Override
        public void save(User user) {
            byId.put(user.id(), user);
        }
    }

    static final class InMemoryAppointerContactLookup implements AppointerContactLookup {
        private final Map<UUID, List<AppointerContact>> appointers = new HashMap<>();
        private List<AppointerContact> mkContacts = List.of();

        void seedAppointers(UUID targetUserId, List<AppointerContact> contacts) {
            appointers.put(targetUserId, contacts);
        }

        void seedMadhyasthaKaryalaya(List<AppointerContact> contacts) {
            this.mkContacts = contacts;
        }

        @Override
        public List<AppointerContact> appointersOf(UUID targetUserId) {
            return appointers.getOrDefault(targetUserId, List.of());
        }

        @Override
        public List<AppointerContact> madhyasthaKaryalayaContacts() {
            return mkContacts;
        }
    }
}
