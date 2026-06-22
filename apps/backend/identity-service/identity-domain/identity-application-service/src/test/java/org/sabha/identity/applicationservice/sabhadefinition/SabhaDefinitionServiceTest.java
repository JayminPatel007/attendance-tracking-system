package org.sabha.identity.applicationservice.sabhadefinition;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.SabhaKindRetiredException;
import org.sabha.identity.applicationservice.appointment.AppointRole;
import org.sabha.identity.applicationservice.appointment.AppointableRole;
import org.sabha.identity.applicationservice.appointment.Appointee;
import org.sabha.identity.applicationservice.appointment.AppointerAuthorityLookup;
import org.sabha.identity.applicationservice.appointment.AppointmentResult;
import org.sabha.identity.applicationservice.appointment.RoleAppointmentCommand;
import org.sabha.identity.applicationservice.directory.AddPersonCommand;
import org.sabha.identity.applicationservice.directory.NameCandidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SabhaDefinitionServiceTest {

    private static final UUID SUBJECT = UUID.fromString("00000000-0000-0000-0000-0000000000f0");
    private static final UUID NIRDESHAK = UUID.fromString("00000000-0000-0000-0000-0000000000d0");
    private static final UUID KSHETRA = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID KIND = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID RETIRED_KIND = UUID.fromString("00000000-0000-0000-0000-0000000000a3");
    private static final UUID SANCHALAK_PERSON = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID NEW_SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000e1");

    private final FakeProvisioning provisioning = new FakeProvisioning();
    private final FakeAppointRole appointments = new FakeAppointRole();
    private final SabhaDefinitionService service = new SabhaDefinitionService(
            subject -> subject.equals(SUBJECT) ? Optional.of(NIRDESHAK) : Optional.empty(),
            provisioning,
            new SabhaDefinitionAuthorization(new FakeNirdeshakAuthority()),
            appointments);

    @Test
    void nirdeshakDefinesAWeeklySabhaAndItsSanchalakIsAppointedOnTheNewSabha() {
        SabhaDefinitionResult result = service.define(SUBJECT, SabhaDefinitionCommand.weekly(
                KSHETRA, KIND, DayOfWeek.SUNDAY, LocalTime.of(9, 0), LocalTime.of(10, 30), "Goregaon Mandir",
                Appointee.existing(SANCHALAK_PERSON, "sanchalak.user", "Temp#1234"), null));

        assertThat(result.created()).isTrue();
        assertThat(result.sabhaId()).isEqualTo(NEW_SABHA);

        assertThat(provisioning.weeklyKshetra).isEqualTo(KSHETRA);
        assertThat(provisioning.weeklyKind).isEqualTo(KIND);
        assertThat(provisioning.weeklyDay).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(provisioning.weeklyStart).isEqualTo(LocalTime.of(9, 0));
        assertThat(provisioning.weeklyEnd).isEqualTo(LocalTime.of(10, 30));
        assertThat(provisioning.weeklyVenue).isEqualTo("Goregaon Mandir");
        assertThat(provisioning.weeklyCreatedBy).isEqualTo(NIRDESHAK);

        RoleAppointmentCommand appointed = appointments.commandFor(AppointableRole.SANCHALAK);
        assertThat(appointments.lastSubject).isEqualTo(SUBJECT);
        assertThat(appointed.scope().role()).isEqualTo(AppointableRole.SANCHALAK);
        assertThat(appointed.scope().sabhaId()).isEqualTo(NEW_SABHA);
        assertThat(appointed.existingPersonId()).isEqualTo(SANCHALAK_PERSON);
        assertThat(appointed.username()).isEqualTo("sanchalak.user");
    }

    @Test
    void aCallerOutsideTheirNirdeshakScopeIsDeniedAndNoSabhaIsProvisioned() {
        UUID otherSubject = UUID.fromString("00000000-0000-0000-0000-0000000000f9");
        SabhaDefinitionService denying = new SabhaDefinitionService(
                subject -> Optional.of(UUID.fromString("00000000-0000-0000-0000-0000000000d9")),
                provisioning,
                new SabhaDefinitionAuthorization(new FakeNirdeshakAuthority()),
                appointments);

        assertThatThrownBy(() -> denying.define(otherSubject, SabhaDefinitionCommand.weekly(
                KSHETRA, KIND, DayOfWeek.SUNDAY, LocalTime.of(9, 0), LocalTime.of(10, 30), "Goregaon Mandir",
                Appointee.existing(SANCHALAK_PERSON, "sanchalak.user", "Temp#1234"), null)))
                .isInstanceOf(AuthorizationDeniedException.class);

        assertThat(provisioning.created).isFalse();
        assertThat(appointments.commands).isEmpty();
    }

    @Test
    void aMonthlyAdHocSabhaIsProvisionedWithNoStandingSlot() {
        SabhaDefinitionResult result = service.define(SUBJECT, SabhaDefinitionCommand.monthlyAdHoc(
                KSHETRA, KIND, "Andheri Hall",
                Appointee.existing(SANCHALAK_PERSON, "sanchalak.user", "Temp#1234"), null));

        assertThat(result.created()).isTrue();
        assertThat(provisioning.monthlyVenue).isEqualTo("Andheri Hall");
        assertThat(provisioning.weeklyKshetra).isNull();
        assertThat(appointments.commandFor(AppointableRole.SANCHALAK).scope().sabhaId()).isEqualTo(NEW_SABHA);
    }

    @Test
    void anOptionalSahSanchalakIsAppointedOnTheSameSabha() {
        UUID sahPerson = UUID.fromString("00000000-0000-0000-0000-0000000000c2");
        SabhaDefinitionResult result = service.define(SUBJECT, SabhaDefinitionCommand.weekly(
                KSHETRA, KIND, DayOfWeek.SUNDAY, LocalTime.of(9, 0), LocalTime.of(10, 30), "Goregaon Mandir",
                Appointee.existing(SANCHALAK_PERSON, "sanchalak.user", "Temp#1234"),
                Appointee.existing(sahPerson, "sah.user", "Temp#5678")));

        assertThat(result.created()).isTrue();
        assertThat(result.sahSanchalakAssignmentId()).isNotNull();
        RoleAppointmentCommand sah = appointments.commandFor(AppointableRole.SAH_SANCHALAK);
        assertThat(sah.scope().sabhaId()).isEqualTo(NEW_SABHA);
        assertThat(sah.existingPersonId()).isEqualTo(sahPerson);
    }

    @Test
    void aSanchalakNameSoftWarnRollsBackTheActAndSurfacesCandidates() {
        appointments.softWarnSanchalak = true;

        SabhaDefinitionResult result = service.define(SUBJECT, SabhaDefinitionCommand.weekly(
                KSHETRA, KIND, DayOfWeek.SUNDAY, LocalTime.of(9, 0), LocalTime.of(10, 30), "Goregaon Mandir",
                Appointee.newPerson(new AddPersonCommand("Rakesh Patel",
                        org.sabha.identity.domain.Gender.MALE, null, "9876543210", null, null, false),
                        "sanchalak.user", "Temp#1234"),
                null));

        assertThat(result.softWarned()).isTrue();
        assertThat(result.candidates()).extracting(NameCandidate::fullName).containsExactly("Rakesh Patel");
        // The Sah-Sanchalak appointment is never attempted once the Sanchalak soft-warns.
        assertThat(appointments.commandFor(AppointableRole.SAH_SANCHALAK)).isNull();
    }

    @Test
    void definingAgainstAnUnknownSabhaKindIsRejected() {
        UUID unknownKind = UUID.fromString("00000000-0000-0000-0000-0000000000ee");

        assertThatThrownBy(() -> service.define(SUBJECT, SabhaDefinitionCommand.weekly(
                KSHETRA, unknownKind, DayOfWeek.SUNDAY, LocalTime.of(9, 0), LocalTime.of(10, 30), "Goregaon Mandir",
                Appointee.existing(SANCHALAK_PERSON, "sanchalak.user", "Temp#1234"), null)))
                .isInstanceOf(SabhaKindNotFoundException.class);
        assertThat(provisioning.created).isFalse();
    }

    @Test
    void definingAgainstARetiredSabhaKindIsRejectedAndNoSabhaIsProvisioned() {
        assertThatThrownBy(() -> service.define(SUBJECT, SabhaDefinitionCommand.weekly(
                KSHETRA, RETIRED_KIND, DayOfWeek.SUNDAY, LocalTime.of(9, 0), LocalTime.of(10, 30), "Goregaon Mandir",
                Appointee.existing(SANCHALAK_PERSON, "sanchalak.user", "Temp#1234"), null)))
                .isInstanceOf(SabhaKindRetiredException.class);
        assertThat(provisioning.created).isFalse();
    }

    /** Only NIRDESHAK holds Nirdeshak over (KSHETRA, YUVAK). */
    private static final class FakeNirdeshakAuthority implements AppointerAuthorityLookup {
        @Override
        public boolean holdsNirdeshak(UUID userId, UUID kshetraId, String demographic) {
            return userId.equals(NIRDESHAK) && kshetraId.equals(KSHETRA) && demographic.equals("YUVAK");
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

    private static final class FakeProvisioning implements org.sabha.common.SabhaProvisioning {
        boolean created;
        UUID weeklyKshetra;
        UUID weeklyKind;
        DayOfWeek weeklyDay;
        LocalTime weeklyStart;
        LocalTime weeklyEnd;
        String weeklyVenue;
        UUID weeklyCreatedBy;
        String monthlyVenue;

        @Override
        public Optional<String> demographicOfKind(UUID sabhaKindId) {
            return sabhaKindId.equals(KIND) || sabhaKindId.equals(RETIRED_KIND)
                    ? Optional.of("YUVAK") : Optional.empty();
        }

        @Override
        public boolean isKindRetired(UUID sabhaKindId) {
            return sabhaKindId.equals(RETIRED_KIND);
        }

        @Override
        public UUID createWeekly(UUID kshetraId, UUID sabhaKindId, DayOfWeek dayOfWeek,
                                 LocalTime startTime, LocalTime endTime, String venue, UUID createdBy) {
            this.created = true;
            this.weeklyKshetra = kshetraId;
            this.weeklyKind = sabhaKindId;
            this.weeklyDay = dayOfWeek;
            this.weeklyStart = startTime;
            this.weeklyEnd = endTime;
            this.weeklyVenue = venue;
            this.weeklyCreatedBy = createdBy;
            return NEW_SABHA;
        }

        @Override
        public UUID createMonthlyAdHoc(UUID kshetraId, UUID sabhaKindId, String venue, UUID createdBy) {
            this.created = true;
            this.monthlyVenue = venue;
            return NEW_SABHA;
        }
    }

    private static final class FakeAppointRole implements AppointRole {
        final java.util.List<RoleAppointmentCommand> commands = new java.util.ArrayList<>();
        UUID lastSubject;
        boolean softWarnSanchalak;

        RoleAppointmentCommand commandFor(AppointableRole role) {
            return commands.stream().filter(c -> c.scope().role() == role).findFirst().orElse(null);
        }

        @Override
        public AppointmentResult appoint(UUID keycloakSubject, RoleAppointmentCommand command) {
            this.lastSubject = keycloakSubject;
            this.commands.add(command);
            if (softWarnSanchalak && command.scope().role() == AppointableRole.SANCHALAK) {
                return AppointmentResult.softWarn(java.util.List.of(
                        new NameCandidate(SANCHALAK_PERSON, "Rakesh Patel", java.util.List.of("Yuvak Sabha"))));
            }
            return AppointmentResult.appointed(
                    SANCHALAK_PERSON,
                    UUID.fromString("00000000-0000-0000-0000-0000000000c9"),
                    UUID.randomUUID());
        }
    }
}
