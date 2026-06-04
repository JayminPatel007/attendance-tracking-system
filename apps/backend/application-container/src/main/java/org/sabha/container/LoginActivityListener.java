package org.sabha.container;

import java.time.Clock;
import java.util.UUID;

import org.sabha.common.CallerResolver;
import org.sabha.common.UserActivityRecorder;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Records a User's last login so the proxy picker's "last seen" hint reflects web
 * sign-ins (Slice 14). Listens for {@link InteractiveAuthenticationSuccessEvent},
 * which Spring Security fires once per interactive (web OIDC) login success — not
 * on the stateless per-request JWT authentication the mobile API chain uses, so a
 * Bearer call never counts as a "login" here.
 *
 * <p>The authenticated principal's name is the Keycloak subject (the {@code sub}
 * claim, per ADR-0022); it is resolved to the local User before recording. A
 * subject with no local User (should not happen post-login) is simply ignored.</p>
 */
@Component
public class LoginActivityListener {

    private final CallerResolver callers;
    private final UserActivityRecorder activity;
    private final Clock clock;

    public LoginActivityListener(CallerResolver callers, UserActivityRecorder activity, Clock clock) {
        this.callers = callers;
        this.activity = activity;
        this.clock = clock;
    }

    @EventListener
    public void onLogin(InteractiveAuthenticationSuccessEvent event) {
        String name = event.getAuthentication().getName();
        UUID subject;
        try {
            subject = UUID.fromString(name);
        } catch (IllegalArgumentException notASubjectUuid) {
            return;
        }
        callers.resolveUserId(subject)
                .ifPresent(userId -> activity.recordLogin(userId, clock.instant()));
    }
}
