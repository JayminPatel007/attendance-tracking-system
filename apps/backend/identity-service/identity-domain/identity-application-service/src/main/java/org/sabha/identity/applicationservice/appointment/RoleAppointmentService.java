package org.sabha.identity.applicationservice.appointment;

import java.time.Clock;
import java.util.UUID;

import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.AuthorizedAction;
import org.sabha.common.CallerResolver;
import org.sabha.identity.applicationservice.IdentityProviderGateway;
import org.sabha.identity.applicationservice.UserRepository;
import org.sabha.identity.applicationservice.directory.AddPersonApplicationService;
import org.sabha.identity.applicationservice.directory.AddResult;
import org.sabha.identity.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the one-transaction role appointment of ADR-0011:
 *
 * <ol>
 *   <li>Authorize the appointer via {@link AppointmentAuthorization} — a denial
 *       becomes an {@link AuthorizationDeniedException} (HTTP 403).</li>
 *   <li>Resolve the appointee Person: an existing pick is reused as-is; a new
 *       Person is created inline through {@link AddPersonApplicationService},
 *       reusing Slice 6's de-dup (mobile hard block + name soft-warn) and the
 *       initial Home Sabha assignment — the single exception to ADR-0002's OTP
 *       requirement, since the appointer is creating the Person right now.</li>
 *   <li>Create User credentials only when the Person does not already hold a
 *       login; username uniqueness is enforced before commit
 *       ({@link UsernameAlreadyTakenException}, HTTP 409).</li>
 *   <li>Record the RoleAssignment with its {@code appointedBy} / {@code appointedAt}
 *       audit.</li>
 * </ol>
 *
 * <p>The name soft-warn short-circuits the whole act: nothing is created and the
 * candidates bubble up for the appointer to resolve.</p>
 */
@Service
public class RoleAppointmentService implements AppointRole {

    /** ADR-0025 §3: at most two active Sah-Nirdeshaks per (Kshetra, demographic). */
    static final int MAX_SAH_NIRDESHAK_PER_KSHETRA_DEMOGRAPHIC = 2;

    private final CallerResolver callerResolver;
    private final AppointmentAuthorization authz;
    private final AddPersonApplicationService addPerson;
    private final UserRepository users;
    private final IdentityProviderGateway identityProvider;
    private final RoleAppointmentRepository appointments;
    private final SahNirdeshakCountLookup sahNirdeshakCount;
    private final Clock clock;

    public RoleAppointmentService(
            CallerResolver callerResolver,
            AppointmentAuthorization authz,
            AddPersonApplicationService addPerson,
            UserRepository users,
            IdentityProviderGateway identityProvider,
            RoleAppointmentRepository appointments,
            SahNirdeshakCountLookup sahNirdeshakCount,
            Clock clock) {
        this.callerResolver = callerResolver;
        this.authz = authz;
        this.addPerson = addPerson;
        this.users = users;
        this.identityProvider = identityProvider;
        this.appointments = appointments;
        this.sahNirdeshakCount = sahNirdeshakCount;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AppointmentResult appoint(UUID keycloakSubject, RoleAppointmentCommand command) {
        UUID appointer = callerResolver.requireUserId(keycloakSubject);

        if (!authz.canAppoint(appointer, command.scope())) {
            throw new AuthorizationDeniedException(appointer, AuthorizedAction.APPOINT_ROLE);
        }

        enforceSahNirdeshakCap(command.scope());

        UUID personId;
        if (command.createsNewPerson()) {
            AddResult added = addPerson.add(keycloakSubject, command.newPerson());
            if (added.softWarned()) {
                return AppointmentResult.softWarn(added.candidates());
            }
            personId = added.personId();
        } else {
            personId = command.existingPersonId();
        }

        UUID userId = users.findByPersonId(personId)
                .map(User::id)
                .orElseGet(() -> createUser(personId, command));

        UUID assignmentId = UUID.randomUUID();
        appointments.save(new RoleAppointmentRow(
                assignmentId, userId, command.scope(), appointer, clock.instant()));

        return AppointmentResult.appointed(personId, userId, assignmentId);
    }

    /**
     * The Sah-Nirdeshak is an operational backstop, capped at two per (Kshetra,
     * demographic) (ADR-0025 §3). A capacity collision, not an authority denial,
     * so it surfaces as a 409 and is checked before anything is created.
     */
    private void enforceSahNirdeshakCap(AppointmentScope scope) {
        if (scope.role() == AppointableRole.SAH_NIRDESHAK
                && sahNirdeshakCount.activeCount(scope.kshetraId(), scope.demographic())
                        >= MAX_SAH_NIRDESHAK_PER_KSHETRA_DEMOGRAPHIC) {
            throw new SahNirdeshakCapReachedException(MAX_SAH_NIRDESHAK_PER_KSHETRA_DEMOGRAPHIC);
        }
    }

    private UUID createUser(UUID personId, RoleAppointmentCommand command) {
        if (users.existsByUsername(command.username())) {
            throw new UsernameAlreadyTakenException(command.username());
        }
        UUID keycloakUserId = identityProvider.createUserRequiringPasswordChange(
                command.username(), command.rawPassword());
        User user = new User(UUID.randomUUID(), personId, command.username(), keycloakUserId);
        users.save(user);
        return user.id();
    }
}
