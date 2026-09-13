package org.sabha.common.web;

import java.util.UUID;

import org.sabha.common.CallerResolver;
import org.sabha.common.CallerUnknownException;
import org.sabha.common.UserId;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * The one place in the backend that knows what a credential looks like
 * (ADR-0030). Turns the authenticated request into the local {@code users.id}
 * the domain works in, so no signature below the edge names a Keycloak subject.
 *
 * <p>It reads {@link Authentication}, not {@code Jwt}, so a single resolver
 * serves both edges: the mobile Bearer resource server, where the subject is the
 * JWT's {@code sub} claim, and the web BFF's server-side OIDC session
 * (ADR-0022), where it is {@link Authentication#getName()}. Both yield the same
 * Keycloak subject.</p>
 *
 * <p>Everything that can go wrong with identifying a caller goes wrong
 * <em>here</em>, as a 403, rather than several frames into a use case: no
 * authentication, an anonymous one, a subject that is not a UUID, or a subject
 * with no local {@code users} row.</p>
 */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final CallerResolver callers;

    public CurrentUserArgumentResolver(CallerResolver callers) {
        this.callers = callers;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && UserId.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        return resolve(SecurityContextHolder.getContext().getAuthentication());
    }

    /** Package-visible so the resolution rules can be tested without a servlet stack. */
    UserId resolve(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw CallerUnknownException.unusableSubject(String.valueOf(authentication));
        }
        String name = authentication.getName();
        UUID subject;
        try {
            subject = UUID.fromString(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw CallerUnknownException.unusableSubject(name);
        }
        return callers.resolveUserId(subject)
                .map(UserId::of)
                .orElseThrow(() -> new CallerUnknownException(subject));
    }
}
