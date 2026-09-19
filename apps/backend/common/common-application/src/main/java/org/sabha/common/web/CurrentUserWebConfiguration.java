package org.sabha.common.web;

import java.util.List;

import org.sabha.common.CallerAuthorityLookup;
import org.sabha.common.CallerResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers {@link CurrentUserArgumentResolver} with Spring MVC. The module
 * wires itself rather than relying on {@code application-container} to remember
 * (ADR-0030): an unregistered resolver fails as "this parameter cannot be
 * resolved" on every annotated endpoint, which is not a failure anyone should
 * have to diagnose.
 */
@Configuration
public class CurrentUserWebConfiguration implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUser;

    public CurrentUserWebConfiguration(CallerResolver callers, CallerAuthorityLookup authorities) {
        this.currentUser = new CurrentUserArgumentResolver(callers, authorities);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUser);
    }
}
