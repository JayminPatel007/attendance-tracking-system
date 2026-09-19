package org.sabha.identity.applicationservice.passwordreset;

import java.util.UUID;

import org.sabha.common.CallerAuthority;
import org.sabha.common.SantLookup;
import org.springframework.stereotype.Service;

/**
 * The Authorization Engine for assigner reissue (ADR-0004): who may generate a
 * fresh password for someone else. Two arms, and they are two different rules —
 * the Karyakar who <em>appointed</em> the target may reissue for them, and
 * because a Sant has no appointer at all (ADR-0011) a Madhyastha Karyalaya member
 * stands in for one. Pure decision component: returns a boolean, never throws or
 * mutates; {@link PasswordReissueService} turns a {@code false} into an
 * {@link org.sabha.common.AuthorizationDeniedException}.
 *
 * <p><b>Why this one was promoted rather than inlined.</b> ADR-0032's subtractive
 * test — <em>a class is an engine iff something is left once the caller's own
 * rows are removed</em> — deleted two engines and inlined three predicates. This
 * predicate reads two real ports, {@link ReissueAuthorityLookup#wasAppointedBy}
 * and {@link SantLookup#isSant}, and <b>both are keyed on the target</b>, so
 * nothing about it folds into the caller parameter. It was a private method on an
 * application service; the rule promoted it. That the same test which deleted
 * {@code SabhaDefinitionAuthorization} promoted the predicate flagged as the most
 * anomalous on the map is what marks it a rule rather than a rationalisation.</p>
 *
 * <p>It is now the one engine every port of which is about somebody other than
 * the caller, and the backend's sole {@code appointed_by}-keyed authority check —
 * the one that contradicts ADR-0025's current-scope rule, under ADR-0004's
 * explicit licence. That contradiction had no documented home before; it has one
 * now.</p>
 *
 * <p>No {@code @Transactional} here, and none needed: it lands in
 * identity-application-service, which issue #69's placement rule already permits,
 * and the transactional member of {@link PasswordReissueService} is the reissue
 * <em>command</em>, which stays behind.</p>
 */
@Service
public class ReissueAuthorization {

    private final ReissueAuthorityLookup appointments;
    private final SantLookup sants;

    public ReissueAuthorization(ReissueAuthorityLookup appointments, SantLookup sants) {
        this.appointments = appointments;
        this.sants = sants;
    }

    /** Whether {@code caller} may reissue a password for {@code targetUserId}. */
    public boolean canReissue(CallerAuthority caller, UUID targetUserId) {
        if (appointments.wasAppointedBy(targetUserId, caller.userId().value())) {
            return true;
        }
        return sants.isSant(targetUserId) && caller.isMadhyasthaKaryalaya();
    }
}
