package org.sabha.identity.applicationservice.session;

import java.util.Optional;

import org.sabha.common.AuditReadAccess;
import org.sabha.common.CallerAuthority;
import org.sabha.identity.applicationservice.UserRepository;
import org.sabha.identity.domain.MemberAuthority;
import org.sabha.identity.domain.VisibleSections;
import org.springframework.stereotype.Service;

/**
 * Resolves an authenticated web (BFF) session to the shell view-model: the local
 * User behind the Keycloak subject, their MK and Regional Team membership, and
 * their visible {@link org.sabha.identity.domain.Section}s (ADR-0022, Slice 9
 * role-based nav).
 *
 * <p>Regional Team membership unlocks Structural Admin for Zone creation
 * (ADR-0024, issue #84), so the session carries it alongside MK; it is read as a
 * non-empty City scope. The Audit-log section is gated by {@link
 * AuditReadAccess}, the same scope-resolution the audit BFF uses, rather than a
 * tier rule restated here — so the sidebar admits exactly the set the engine
 * admits, the Regional Team included (issue #80).</p>
 *
 * <p><b>This path is the fold's best number.</b> It issued <em>eight</em> SQL
 * statements before ADR-0032 — the {@code users} row, MK membership, the Regional
 * Team cities, the operational roles, and {@code canRead} fanning out to three
 * more through the deleted {@code JdbcAuditScopeLookup}. Three of its four ports
 * turned out to ask the same question of the same relation about the same
 * caller. It now issues <b>three</b>: the edge's two, plus the one below.</p>
 *
 * <p><b>Why three and not two: the stopping rule costs something here.</b> The
 * re-fetch of the caller's {@code users} row survives, and reduces to a single
 * column — {@code username()} — because the edge's resolver projects {@code id}
 * alone. The tempting fix is to put {@code username} on {@link CallerAuthority},
 * and that is precisely what its stopping rule forbids: a username is not a
 * projection of {@code role_assignments}. <b>Leave the re-fetch.</b> It is the
 * first time that rule has refused anything, and a rule that has never refused
 * anything has not been tested.</p>
 */
@Service
public class WebSessionService {

    private final UserRepository users;
    private final AuditReadAccess auditReadAccess;

    public WebSessionService(UserRepository users, AuditReadAccess auditReadAccess) {
        this.users = users;
        this.auditReadAccess = auditReadAccess;
    }

    public Optional<WebSession> describe(CallerAuthority caller) {
        // Optional, though the empty branch is unreachable: the edge 403s when the
        // authenticated subject has no `users` row, so a CallerAuthority never
        // exists for a user this find could miss. Documented rather than deleted,
        // following CurrentUserArgumentResolver's no-authentication branch one file
        // away (commit 34043e4) — the shape is load-bearing if the edge ever gains
        // a second, laxer resolution path.
        return users.findById(caller.userId().value()).map(user -> {
            MemberAuthority authority = new MemberAuthority(
                    caller.isMadhyasthaKaryalaya(),
                    !caller.regionalTeamCities().isEmpty(),
                    auditReadAccess.canRead(caller),
                    caller.operationalRoles());
            return new WebSession(
                    user.username(),
                    authority.madhyasthaKaryalaya(),
                    authority.regionalTeam(),
                    VisibleSections.forMember(authority));
        });
    }
}
