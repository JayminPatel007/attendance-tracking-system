package org.sabha.identity.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.sabha.identity.applicationservice.directory.AddPersonCommand;
import org.sabha.identity.applicationservice.appointment.AppointableRole;
import org.sabha.identity.applicationservice.appointment.AppointmentResult;
import org.sabha.identity.applicationservice.appointment.AppointmentScope;
import org.sabha.identity.applicationservice.directory.NameCandidate;
import org.sabha.identity.applicationservice.appointment.RoleAppointmentCommand;
import org.sabha.identity.applicationservice.appointment.RoleAppointmentService;
import org.sabha.identity.domain.Gender;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Role-appointment BFF endpoint (ADR-0011, ADR-0022): the Angular web shell posts
 * a single appointment that resolves-or-creates the Person, mints credentials
 * when needed, and records the RoleAssignment in one transaction. The
 * directory-first candidate search the form uses lives in
 * {@link DirectoryBffController}. Authenticated by the server-side OIDC session,
 * so the caller is the Keycloak subject in {@link Authentication#getName()}; an
 * authenticated subject with no local User is unauthorized (403). Authority, the
 * mobile hard block, the name soft-warn, and username uniqueness are arbitrated
 * by {@link RoleAppointmentService} and mapped by the global exception handler.
 */
@RestController
public class RoleAppointmentController {

    private final RoleAppointmentService appointments;

    public RoleAppointmentController(RoleAppointmentService appointments) {
        this.appointments = appointments;
    }

    @PostMapping("/bff/appointments")
    public ResponseEntity<AppointmentResponse> appoint(
            @RequestBody AppointmentRequest req, Authentication authentication) {
        UUID subject = UUID.fromString(authentication.getName());
        AppointmentResult result = appointments.appoint(subject, req.toCommand());
        if (result.softWarned()) {
            return ResponseEntity.ok(AppointmentResponse.softWarn(result.candidates()));
        }
        return ResponseEntity.status(201).body(
                AppointmentResponse.appointed(result.personId(), result.userId(), result.assignmentId()));
    }

    public record AppointmentRequest(
            AppointableRole role,
            UUID sabhaId,
            UUID kshetraId,
            UUID zoneId,
            UUID cityId,
            String demographic,
            UUID existingPersonId,
            NewPersonPayload newPerson,
            String username,
            String rawPassword) {

        RoleAppointmentCommand toCommand() {
            AppointmentScope scope = new AppointmentScope(
                    role, sabhaId, kshetraId, zoneId, cityId, demographic);
            if (existingPersonId != null) {
                return RoleAppointmentCommand.forExistingPerson(scope, existingPersonId, username, rawPassword);
            }
            return RoleAppointmentCommand.forNewPerson(scope, newPerson.toCommand(), username, rawPassword);
        }
    }

    public record NewPersonPayload(
            String fullName,
            Gender gender,
            LocalDate dateOfBirth,
            String mobile,
            UUID guardianPersonId,
            UUID homeSabhaId,
            boolean overrideDuplicateWarning) {

        AddPersonCommand toCommand() {
            return new AddPersonCommand(
                    fullName, gender, dateOfBirth, mobile, guardianPersonId, homeSabhaId, overrideDuplicateWarning);
        }
    }

    public record AppointmentResponse(
            UUID personId, UUID userId, UUID assignmentId,
            List<NameCandidate> candidates, boolean requiresOverride) {

        static AppointmentResponse appointed(UUID personId, UUID userId, UUID assignmentId) {
            return new AppointmentResponse(personId, userId, assignmentId, List.of(), false);
        }

        static AppointmentResponse softWarn(List<NameCandidate> candidates) {
            return new AppointmentResponse(null, null, null, candidates, true);
        }
    }
}
