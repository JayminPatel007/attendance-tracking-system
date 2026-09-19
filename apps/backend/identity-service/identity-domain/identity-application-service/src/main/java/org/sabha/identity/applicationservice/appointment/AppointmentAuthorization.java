package org.sabha.identity.applicationservice.appointment;

import java.util.UUID;

import org.sabha.common.CallerAuthority;
import org.sabha.common.SabhaFact;
import org.sabha.common.SabhaFacts;
import org.sabha.common.StructuralParentage;
import org.springframework.stereotype.Service;

/**
 * The Authorization Engine for role appointment (ADR-0011): given an appointer
 * and the scope of the role being filled, it resolves the geographic containment
 * (Sabha → Kshetra → Zone → City) and checks the appointer holds the tier above
 * at the parent scope. Pure decision component — returns a boolean, never throws
 * or mutates; {@link RoleAppointmentService} turns a {@code false} into an
 * {@link org.sabha.common.AuthorizationDeniedException}.
 *
 * <p>Its two authority ports folded into the caller parameter under ADR-0032,
 * leaving only the two that resolve <em>containment</em> — which are keyed by the
 * target scope, not the appointer. What survives here is the whole of the
 * policy: the six-arm switch, the inverted rank (you appoint the tier beneath
 * you), and the Regional Team's self-replication disjunction.</p>
 */
@Service
public class AppointmentAuthorization {

    private final SabhaFacts sabhas;
    private final StructuralParentage parentage;

    public AppointmentAuthorization(SabhaFacts sabhas, StructuralParentage parentage) {
        this.sabhas = sabhas;
        this.parentage = parentage;
    }

    public boolean canAppoint(CallerAuthority appointer, AppointmentScope scope) {
        return switch (scope.role()) {
            case SANCHALAK, SAH_SANCHALAK -> nirdeshakOverSabha(appointer, scope.sabhaId());
            case NIRIKSHAK, SAH_NIRDESHAK ->
                    appointer.holdsNirdeshakIn(scope.kshetraId(), scope.demographic());
            case NIRDESHAK -> sanyojakOverKshetra(appointer, scope.kshetraId(), scope.demographic());
            case SANYOJAK -> regionalTeamOverZone(appointer, scope.zoneId(), scope.demographic());
            case REGIONAL_TEAM -> regionalTeamPeerOrMk(appointer, scope.cityId(), scope.demographic());
            case SANT -> appointer.isMadhyasthaKaryalaya();
        };
    }

    /**
     * The Regional Team is self-replicating (ADR-0025 §2): a peer already holding
     * a Regional Team role for the same (City, demographic) may appoint another,
     * in addition to the Madhyastha Karyalaya's bootstrap path (ADR-0011).
     */
    private boolean regionalTeamPeerOrMk(CallerAuthority appointer, UUID cityId, String demographic) {
        return appointer.isMadhyasthaKaryalaya()
                || appointer.holdsRegionalTeamIn(cityId, demographic);
    }

    private boolean nirdeshakOverSabha(CallerAuthority appointer, UUID sabhaId) {
        return sabhas.of(sabhaId)
                .map(SabhaFact::scope)
                .map(s -> appointer.holdsNirdeshakIn(s.kshetraId(), s.demographic()))
                .orElse(false);
    }

    private boolean sanyojakOverKshetra(CallerAuthority appointer, UUID kshetraId, String demographic) {
        return parentage.zoneOfKshetra(kshetraId)
                .map(zoneId -> appointer.holdsSanyojakIn(zoneId, demographic))
                .orElse(false);
    }

    private boolean regionalTeamOverZone(CallerAuthority appointer, UUID zoneId, String demographic) {
        return parentage.cityOfZone(zoneId)
                .map(cityId -> appointer.holdsRegionalTeamIn(cityId, demographic))
                .orElse(false);
    }
}
