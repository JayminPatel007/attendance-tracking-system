package org.sabha.identity.application;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.sabha.identity.applicationservice.directory.AddPersonCommand;
import org.sabha.identity.applicationservice.appointment.Appointee;
import org.sabha.identity.applicationservice.directory.NameCandidate;
import org.sabha.identity.applicationservice.sabhadefinition.SabhaDefinitionCommand;
import org.sabha.identity.applicationservice.sabhadefinition.SabhaDefinitionResult;
import org.sabha.identity.applicationservice.sabhadefinition.SabhaDefinitionService;
import org.sabha.identity.domain.Gender;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sabha-definition BFF endpoint (ADR-0012, ADR-0022): the Angular web shell posts
 * a single request that creates a Sabha and appoints its Sanchalak (and optional
 * Sah-Sanchalak) in one transaction. Authenticated by the server-side OIDC
 * session, so the caller is the Keycloak subject in {@link Authentication#getName()};
 * an authenticated subject with no local User is unauthorized (403). Nirdeshak
 * authority, the schedule-shape invariants, the inline-Person dedup soft-warn, and
 * username uniqueness are arbitrated by {@link SabhaDefinitionService} and the
 * reused appointment flow, then mapped by the global exception handler.
 */
@RestController
public class SabhaDefinitionController {

    private final SabhaDefinitionService sabhaDefinition;

    public SabhaDefinitionController(SabhaDefinitionService sabhaDefinition) {
        this.sabhaDefinition = sabhaDefinition;
    }

    @PostMapping("/bff/sabhas")
    public ResponseEntity<SabhaDefinitionResponse> define(
            @RequestBody DefineSabhaRequest req, Authentication authentication) {
        UUID subject = UUID.fromString(authentication.getName());
        SabhaDefinitionResult result = sabhaDefinition.define(subject, req.toCommand());
        if (result.softWarned()) {
            return ResponseEntity.ok(SabhaDefinitionResponse.softWarn(result.candidates()));
        }
        return ResponseEntity.status(201).body(SabhaDefinitionResponse.created(
                result.sabhaId(), result.sanchalakAssignmentId(), result.sahSanchalakAssignmentId()));
    }

    public record DefineSabhaRequest(
            UUID kshetraId,
            UUID sabhaKindId,
            boolean weekly,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            String standingVenue,
            AppointeePayload sanchalak,
            AppointeePayload sahSanchalak) {

        SabhaDefinitionCommand toCommand() {
            Appointee sah = sahSanchalak == null ? null : sahSanchalak.toAppointee();
            return weekly
                    ? SabhaDefinitionCommand.weekly(kshetraId, sabhaKindId, dayOfWeek, startTime, endTime,
                            standingVenue, sanchalak.toAppointee(), sah)
                    : SabhaDefinitionCommand.monthlyAdHoc(kshetraId, sabhaKindId, standingVenue,
                            sanchalak.toAppointee(), sah);
        }
    }

    public record AppointeePayload(
            UUID existingPersonId,
            NewPersonPayload newPerson,
            String username,
            String rawPassword) {

        Appointee toAppointee() {
            return existingPersonId != null
                    ? Appointee.existing(existingPersonId, username, rawPassword)
                    : Appointee.newPerson(newPerson.toCommand(), username, rawPassword);
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

    public record SabhaDefinitionResponse(
            UUID sabhaId, UUID sanchalakAssignmentId, UUID sahSanchalakAssignmentId,
            List<NameCandidate> candidates, boolean requiresOverride) {

        static SabhaDefinitionResponse created(UUID sabhaId, UUID sanchalakAssignmentId, UUID sahSanchalakAssignmentId) {
            return new SabhaDefinitionResponse(sabhaId, sanchalakAssignmentId, sahSanchalakAssignmentId, List.of(), false);
        }

        static SabhaDefinitionResponse softWarn(List<NameCandidate> candidates) {
            return new SabhaDefinitionResponse(null, null, null, candidates, true);
        }
    }
}
