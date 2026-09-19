package org.sabha.common.web;

import java.util.UUID;

import org.sabha.common.CallerAuthority;
import org.sabha.common.CallerAuthorityLookup;
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
 *
 * <p>Since ADR-0032 it resolves a {@link CallerAuthority} rather than a bare
 * {@link UserId}: the caller's {@code role_assignments} rows are loaded here too,
 * so every engine in the request is handed the same instance and a request costs
 * one authority query however many authority questions it asks. The two loads
 * stay <b>two sequential statements</b> — {@code users} by subject, then
 * assignments by {@code users.id}. The {@code LEFT JOIN} that would collapse them
 * is available and deliberately not taken: it fuses the two questions ADR-0030
 * separated, and only the first of them is a 403 when it fails.</p>
 *
 * <p>There is nothing to cache and nothing caching is built for. The resolver
 * runs once per <em>declared parameter</em>, and {@code CallerAuthority} is the
 * only caller parameter type there is, so request-scoped reuse falls out of
 * parameter passing — no {@code @RequestScope} bean, no {@code ThreadLocal}, no
 * memoization. A second caller parameter type is what would make caching a real
 * problem, and ADR-0032 rejected one.</p>
 */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final CallerResolver callers;
    private final CallerAuthorityLookup authorities;

    public CurrentUserArgumentResolver(CallerResolver callers, CallerAuthorityLookup authorities) {
        this.callers = callers;
        this.authorities = authorities;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && CallerAuthority.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        return resolve(SecurityContextHolder.getContext().getAuthentication());
    }

    /** Package-visible so the resolution rules can be tested without a servlet stack. */
    CallerAuthority resolve(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            // Defensive, and unreachable by design: both filter chains end in
            // anyRequest().authenticated(), so an unauthenticated request to a route
            // that resolves a caller is already a 401 (the BFF's HttpStatusEntryPoint,
            // or the resource server's WWW-Authenticate) and never reaches a handler.
            // The public routes that skip that are exempt from resolving a caller at
            // all, and `every_handler_resolves_its_caller` fails the build if one of
            // them gains a @CurrentUser. If this ever throws, the security config is
            // wrong, not the client — which is why it stays a 403 rather than growing
            // a 401 path of its own.
            //
            // Note anonymity does not arrive here: AnonymousAuthenticationToken is
            // isAuthenticated(), so it falls through to the getName() branch below.
            //
            // Never the Authentication itself: the global handler puts this message
            // straight into the RFC 9457 `detail`, and a token's toString carries
            // its principal and authorities.
            throw CallerUnknownException.unusableSubject("no authentication");
        }
        String name = authentication.getName();
        UUID subject;
        try {
            subject = UUID.fromString(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw CallerUnknownException.unusableSubject(name);
        }
        UserId caller = callers.resolveUserId(subject)
                .map(UserId::of)
                .orElseThrow(() -> new CallerUnknownException(subject));
        return new CallerAuthority(caller, authorities.assignmentsOf(caller.value()));
    }
}
