package org.sabha.identity.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.sabha.identity.applicationservice.directory.AddPersonCommand;
import org.sabha.identity.applicationservice.appointment.AppointableRole;
import org.sabha.identity.applicationservice.appointment.AppointmentResult;
import org.sabha.identity.applicationservice.appointment.AppointmentScope;
import org.sabha.identity.applicationservice.directory.NameCandidate;
import org.sabha.identity.applicationservice.appointment.RevokeRole;
import org.sabha.identity.applicationservice.appointment.RoleAppointmentCommand;
import org.sabha.identity.applicationservice.appointment.RoleAppointmentService;
import org.sabha.identity.applicationservice.appointment.SahNirdeshakCap;
import org.sabha.identity.domain.Gender;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final SahNirdeshakCap sahNirdeshakCap;
    private final RevokeRole revokeRole;

    public RoleAppointmentController(
            RoleAppointmentService appointments, SahNirdeshakCap sahNirdeshakCap, RevokeRole revokeRole) {
        this.appointments = appointments;
        this.sahNirdeshakCap = sahNirdeshakCap;
        this.revokeRole = revokeRole;
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

    /**
     * Revokes a role assignment as a state change (ADR-0026): the current holder
     * of the appointing scope — not necessarily whoever appointed it — marks the
     * row revoked, the User loses login on their last active role, and no
     * appointees or structure cascade. Authority, the Regional Team last-one-out
     * guard (409), and unknown/already-revoked ids (404) are arbitrated by
     * {@link RevokeRole} and mapped by the global exception handler. Returns 204.
     */
    @PostMapping("/bff/appointments/{id}/revoke")
    public ResponseEntity<Void> revoke(@PathVariable UUID id, Authentication authentication) {
        UUID subject = UUID.fromString(authentication.getName());
        revokeRole.revoke(subject, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Active Sah-Nirdeshak count for a (Kshetra, demographic) against the cap of
     * two (ADR-0025 §3), so the web role console can show the 2/2 indicator and
     * disable the appoint action at the limit before the appointer submits.
     */
    @GetMapping("/bff/appointments/sah-nirdeshak-cap")
    public SahNirdeshakCapResponse sahNirdeshakCap(
            @RequestParam UUID kshetraId, @RequestParam String demographic) {
        SahNirdeshakCap.CapStatus status = sahNirdeshakCap.statusFor(kshetraId, demographic);
        return new SahNirdeshakCapResponse(status.active(), status.cap(), status.reached());
    }

    public record SahNirdeshakCapResponse(int active, int cap, boolean reached) {
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
