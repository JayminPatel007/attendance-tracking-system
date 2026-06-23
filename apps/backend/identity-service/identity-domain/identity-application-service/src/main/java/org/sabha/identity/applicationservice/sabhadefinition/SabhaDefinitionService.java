package org.sabha.identity.applicationservice.sabhadefinition;

import java.util.List;
import java.util.UUID;

import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.AuthorizedAction;
import org.sabha.common.CallerResolver;
import org.sabha.common.SabhaKindRetiredException;
import org.sabha.common.SabhaProvisioning;
import org.sabha.identity.applicationservice.appointment.AppointRole;
import org.sabha.identity.applicationservice.appointment.AppointableRole;
import org.sabha.identity.applicationservice.appointment.AppointmentResult;
import org.sabha.identity.applicationservice.appointment.AppointmentScope;
import org.sabha.identity.applicationservice.directory.NameCandidate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Orchestrates the one-transaction Sabha definition of ADR-0012: a Nirdeshak
 * creates a Sabha and appoints its Sanchalak (and optional Sah-Sanchalak) as a
 * single atomic act.
 *
 * <ol>
 *   <li>Resolve the calling Nirdeshak via {@link CallerResolver}.</li>
 *   <li>Resolve the chosen kind's demographic and authorize the caller as
 *       Nirdeshak over (Kshetra, demographic) — a denial becomes an
 *       {@link AuthorizationDeniedException} (HTTP 403) before anything is created.</li>
 *   <li>Provision the Sabha across the seam via {@link SabhaProvisioning} (the
 *       sabha context owns the aggregate and its schedule-shape invariants).</li>
 *   <li>Appoint the Sanchalak — and any Sah-Sanchalak — on the new Sabha by
 *       reusing Slice 11's {@link AppointRole} flow (inline Person create, dedup
 *       soft-warn, credentials). A name soft-warn rolls the whole act back so no
 *       Sabha is left behind, and surfaces the candidates for the Nirdeshak.</li>
 * </ol>
 */
@Service
public class SabhaDefinitionService {

    private final CallerResolver callerResolver;
    private final SabhaProvisioning provisioning;
    private final SabhaDefinitionAuthorization authz;
    private final AppointRole appointments;

    public SabhaDefinitionService(
            CallerResolver callerResolver,
            SabhaProvisioning provisioning,
            SabhaDefinitionAuthorization authz,
            AppointRole appointments) {
        this.callerResolver = callerResolver;
        this.provisioning = provisioning;
        this.authz = authz;
        this.appointments = appointments;
    }

    @Transactional
    public SabhaDefinitionResult define(UUID keycloakSubject, SabhaDefinitionCommand command) {
        UUID nirdeshak = callerResolver.requireUserId(keycloakSubject);

        String demographic = provisioning.demographicOfKind(command.sabhaKindId())
                .orElseThrow(() -> new SabhaKindNotFoundException(command.sabhaKindId()));

        if (!authz.canDefineSabha(nirdeshak, command.kshetraId(), demographic)) {
            throw new AuthorizationDeniedException(nirdeshak, AuthorizedAction.CREATE_SABHA);
        }

        if (provisioning.isKindRetired(command.sabhaKindId())) {
            throw new SabhaKindRetiredException(command.sabhaKindId());
        }

        UUID sabhaId = command.weekly()
                ? provisioning.createWeekly(command.kshetraId(), command.sabhaKindId(), command.dayOfWeek(),
                        command.startTime(), command.endTime(), command.standingVenue(), nirdeshak)
                : provisioning.createMonthlyAdHoc(command.kshetraId(), command.sabhaKindId(),
                        command.standingVenue(), nirdeshak);

        AppointmentResult sanchalak = appointments.appoint(keycloakSubject,
                command.sanchalak().toCommand(AppointmentScope.onSabha(AppointableRole.SANCHALAK, sabhaId)));
        if (sanchalak.softWarned()) {
            return rolledBackSoftWarn(sanchalak.candidates());
        }

        UUID sahSanchalakAssignmentId = null;
        if (command.hasSahSanchalak()) {
            AppointmentResult sah = appointments.appoint(keycloakSubject,
                    command.sahSanchalak().toCommand(AppointmentScope.onSabha(AppointableRole.SAH_SANCHALAK, sabhaId)));
            if (sah.softWarned()) {
                return rolledBackSoftWarn(sah.candidates());
            }
            sahSanchalakAssignmentId = sah.assignmentId();
        }

        return SabhaDefinitionResult.created(sabhaId, sanchalak.assignmentId(), sahSanchalakAssignmentId);
    }

    /**
     * A soft-warn surfaces after the Sabha has been provisioned, so the
     * transaction must roll back to honour the single-atomic-act guarantee. The
     * candidates still bubble up for the Nirdeshak to resolve.
     */
    private SabhaDefinitionResult rolledBackSoftWarn(List<NameCandidate> candidates) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
        return SabhaDefinitionResult.softWarn(candidates);
    }
}
