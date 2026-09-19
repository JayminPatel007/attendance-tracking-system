package org.sabha.identity.applicationservice.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.AuditReadAccess;
import org.sabha.common.CallerAuthority;
import org.sabha.identity.applicationservice.Callers;
import org.sabha.identity.applicationservice.UserRepository;
import org.sabha.identity.domain.Section;
import org.sabha.identity.domain.User;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The web shell's session view-model (ADR-0022, Slice 9).
 *
 * <p>This class had <b>no unit test at all</b> before ADR-0032 — which is the
 * mundane reason {@code AuditReadAccess} appeared on issue #224's zero-fake port
 * list. The remediation for that finding was always "write the test", not "delete
 * the port", and the fold is what made writing one cheap: three of the four ports
 * this service held were asking the same question of the same relation about the
 * same caller, and the one that stayed fakes as a lambda.</p>
 *
 * <p>It is also where the fold's best number is asserted. {@code describe} issued
 * <b>eight</b> SQL statements; it issues <b>one</b> here, plus the two the edge
 * already ran — and that one is the {@code users} re-fetch the stopping rule
 * refuses to remove. {@link #describeReadsTheUsersRowExactlyOnce()} pins it, so a
 * future change that quietly reintroduces a per-flag lookup fails.</p>
 */
class WebSessionServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID KSHETRA = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID CITY = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000b3");
    private static final String YUVAK = "YUVAK";

    private final CountingUsers users = new CountingUsers();

    @Test
    void describeReturnsTheUsernameAndTheSectionsTheCallersRolesUnlock() {
        WebSession session = describe(Callers.of(USER_ID).nirdeshakOf(KSHETRA, YUVAK).build(), admitted(false));

        assertThat(session.username()).isEqualTo("nirdeshak.user");
        assertThat(session.madhyasthaKaryalaya()).isFalse();
        assertThat(session.regionalTeam()).isFalse();
        assertThat(session.sections()).containsExactlyInAnyOrder(
                Section.DASHBOARD, Section.SELECTION, Section.OCCURRENCE_REOPEN);
    }

    @Test
    void anMkMemberUnlocksTheStateLevelSections() {
        WebSession session = describe(Callers.madhyasthaKaryalaya(USER_ID), admitted(false));

        assertThat(session.madhyasthaKaryalaya()).isTrue();
        assertThat(session.sections()).contains(
                Section.ROLE_APPOINTMENT, Section.STRUCTURAL_ADMIN, Section.SABHA_DEFINITION);
    }

    @Test
    void aRegionalTeamMemberReachesStructuralAdminAndNothingElseOfMks() {
        // Zone creation moved MK -> Regional Team (ADR-0024, issue #84); the RT gets
        // that one section and none of the MK-only ones.
        WebSession session = describe(Callers.of(USER_ID).regionalTeamIn(CITY, YUVAK).build(), admitted(false));

        assertThat(session.regionalTeam()).isTrue();
        assertThat(session.sections())
                .contains(Section.STRUCTURAL_ADMIN)
                .doesNotContain(Section.ROLE_APPOINTMENT, Section.SABHA_DEFINITION);
    }

    @Test
    void theAuditSectionIsWhateverTheEngineAdmitsRatherThanATierListRestatedHere() {
        // Issue #80: the sidebar and the BFF read one rule, so they cannot drift.
        // The port is the whole seam, and post-fold it fakes as a lambda.
        WebSession admittedSession = describe(Callers.of(USER_ID).sanchalakOf(SABHA).build(), admitted(true));
        WebSession deniedSession = describe(Callers.of(USER_ID).sanchalakOf(SABHA).build(), admitted(false));

        assertThat(admittedSession.sections()).contains(Section.AUDIT_LOG);
        assertThat(deniedSession.sections()).doesNotContain(Section.AUDIT_LOG);
    }

    @Test
    void theAuditGateIsAskedAboutTheCallerItWasGiven() {
        List<CallerAuthority> asked = new ArrayList<>();
        CallerAuthority caller = Callers.of(USER_ID).sanchalakOf(SABHA).build();

        new WebSessionService(users, seen -> {
            asked.add(seen);
            return false;
        }).describe(caller);

        assertThat(asked).containsExactly(caller);
    }

    @Test
    void describeReadsTheUsersRowExactlyOnce() {
        // The fold's arithmetic, pinned. Eight statements became three: the edge's
        // two, plus this one. It survives only because putting `username` on
        // CallerAuthority is what the stopping rule forbids — so if this count ever
        // rises, a port has crept back in; if the read disappears, the rule was
        // quietly stretched.
        describe(Callers.madhyasthaKaryalaya(USER_ID), admitted(true));

        assertThat(users.reads).isEqualTo(1);
    }

    @Test
    void aCallerWithNoUsersRowDescribesNothing() {
        // Unreachable in production — the edge 403s first — but the Optional is the
        // shape, and this is what it means.
        CountingUsers empty = new CountingUsers();
        empty.user = null;

        assertThat(new WebSessionService(empty, admitted(true))
                .describe(Callers.noRoles(USER_ID))).isEmpty();
    }

    private WebSession describe(CallerAuthority caller, AuditReadAccess audit) {
        return new WebSessionService(users, audit).describe(caller).orElseThrow();
    }

    /** The last surviving port, and it fakes as a lambda — ADR-0032 said it would. */
    private static AuditReadAccess admitted(boolean admitted) {
        return caller -> admitted;
    }

    /** Counts reads, because the statement count is the thing under test. */
    private static final class CountingUsers implements UserRepository {
        private User user = new User(USER_ID, UUID.randomUUID(), "nirdeshak.user", UUID.randomUUID());
        private int reads;

        @Override
        public Optional<User> findById(UUID userId) {
            reads++;
            return Optional.ofNullable(user).filter(u -> u.id().equals(userId));
        }

        @Override
        public Optional<User> findByPersonId(UUID personId) {
            return Optional.empty();
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return Optional.empty();
        }

        @Override
        public boolean existsByUsername(String username) {
            return false;
        }

        @Override
        public void save(User user) {
            throw new UnsupportedOperationException("describe() is a read");
        }
    }
}
