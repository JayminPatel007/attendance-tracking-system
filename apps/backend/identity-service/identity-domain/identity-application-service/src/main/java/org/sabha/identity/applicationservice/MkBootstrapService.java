package org.sabha.identity.applicationservice;

import java.util.UUID;

import org.sabha.identity.domain.Person;
import org.sabha.identity.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Install-time bootstrap of the first Madhyastha Karyalaya member (ADR-0011's
 * one-off seed, outside the normal appointment flow). Provisions the user in
 * Keycloak (forced first-login password change) and creates the linked local
 * Person + User + MK membership, all in one transaction.
 *
 * <p>Idempotent: if any MK member already exists, this is a no-op, so it is safe
 * to run on every boot.</p>
 */
@Service
public class MkBootstrapService {

    private final IdentityProviderGateway identityProvider;
    private final PersonRepository persons;
    private final UserRepository users;
    private final MadhyasthaKaryalayaMembership membership;

    public MkBootstrapService(
            IdentityProviderGateway identityProvider,
            PersonRepository persons,
            UserRepository users,
            MadhyasthaKaryalayaMembership membership) {
        this.identityProvider = identityProvider;
        this.persons = persons;
        this.users = users;
        this.membership = membership;
    }

    @Transactional
    public void ensureBootstrapMember(BootstrapMkCommand command) {
        if (membership.anyMemberExists()) {
            return;
        }

        UUID keycloakUserId =
                identityProvider.createUserRequiringPasswordChange(command.username(), command.rawPassword());

        Person person = Person.create(
                UUID.randomUUID(),
                command.fullName(),
                command.gender(),
                null,
                command.mobile(),
                null);
        persons.save(person);

        User user = new User(UUID.randomUUID(), person.id(), command.username(), keycloakUserId);
        users.save(user);

        membership.grantTo(user.id());
    }
}
