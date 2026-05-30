package org.sabha.identity.applicationservice;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.sabha.common.CallerResolver;
import org.sabha.common.DomainEventPublisher;
import org.sabha.identity.domain.MobileAlreadyRegisteredException;
import org.sabha.identity.domain.Person;
import org.sabha.identity.domain.PersonAddedOverDuplicateWarning;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adds a Person to the Directory under the two-signal de-duplication strategy of
 * ADR-0013:
 *
 * <ol>
 *   <li><b>Mobile hard block</b> — an exact match on an existing Person's
 *       own-mobile throws {@link MobileAlreadyRegisteredException}; no creation.</li>
 *   <li><b>Name soft warn</b> — a phonetic/edit-distance match within the Home
 *       Sabha's Kshetra returns up to five {@link NameCandidate}s and creates
 *       nothing, unless the adder set {@code overrideDuplicateWarning}. An
 *       override past a non-empty candidate list emits
 *       {@link PersonAddedOverDuplicateWarning} for audit.</li>
 * </ol>
 *
 * <p>A child without their own phone is added via {@code guardianPersonId}
 * instead of a mobile; the mobile-XOR-guardian invariant is enforced by
 * {@link Person#create}.
 */
@Service
public class AddPersonApplicationService {

    private static final int MAX_CANDIDATES = 5;

    private final CallerResolver callerResolver;
    private final PersonDirectory directory;
    private final DomainEventPublisher events;
    private final Clock clock;

    public AddPersonApplicationService(
            CallerResolver callerResolver,
            PersonDirectory directory,
            DomainEventPublisher events,
            Clock clock) {
        this.callerResolver = callerResolver;
        this.directory = directory;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public AddResult add(UUID keycloakSubject, AddPersonCommand command) {
        UUID adder = callerResolver.resolveUserId(keycloakSubject)
                .orElseThrow(() -> new CallerUnknownException(keycloakSubject));

        if (hasMobile(command)) {
            directory.findByMobile(command.mobile()).ifPresent(existing -> {
                throw new MobileAlreadyRegisteredException(existing);
            });
        }

        UUID kshetraId = directory.kshetraIdOfSabha(command.homeSabhaId())
                .orElseThrow(() -> new HomeSabhaNotFoundException(command.homeSabhaId()));

        List<NameCandidate> candidates =
                directory.findNameCandidates(kshetraId, command.fullName(), MAX_CANDIDATES);
        if (!candidates.isEmpty() && !command.overrideDuplicateWarning()) {
            return AddResult.softWarn(candidates);
        }

        UUID personId = UUID.randomUUID();
        Person person = Person.create(personId, command.fullName(), command.gender(),
                command.dateOfBirth(), command.mobile(), command.guardianPersonId());
        directory.add(person, command.homeSabhaId());

        if (!candidates.isEmpty()) {
            events.publishAll(List.of(new PersonAddedOverDuplicateWarning(
                    personId, adder,
                    candidates.stream().map(NameCandidate::personId).toList(),
                    clock.instant())));
        }
        return AddResult.created(personId);
    }

    private static boolean hasMobile(AddPersonCommand command) {
        return command.mobile() != null && !command.mobile().isBlank();
    }
}
