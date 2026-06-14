package org.sabha.container;

import java.io.IOException;
import java.util.function.Supplier;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * HTTP security policy for the whole deployable (ADR-0019). Two filter chains
 * (ADR-0022):
 *
 * <ul>
 *   <li><b>Web BFF chain</b> (order 1, {@code /bff/**} + the OIDC login/logout
 *       endpoints): server-side {@code oauth2Login}, tokens kept in the
 *       HttpSession, the browser holds only an HTTP-only session cookie. CSRF is
 *       enabled (cookie + header double-submit, SPA-style per the Spring
 *       Security single-page-app guidance); an unauthenticated XHR gets a 401
 *       instead of a login redirect so the SPA decides when to authenticate.</li>
 *   <li><b>API chain</b> (order 2, everything else): the unchanged OAuth2
 *       resource server for the mobile app's Bearer JWTs.</li>
 * </ul>
 *
 * Lives in application-container because the chains compose routes from every
 * bounded context — putting them in any one context would make it know the
 * others' routes.
 */
@Configuration
public class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain webBffFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/bff/**", "/oauth2/**", "/login/**", "/logout")
                .authorizeHttpRequests(authz -> authz.anyRequest().authenticated())
                .oauth2Login(Customizer.withDefaults())
                .logout(logout -> logout
                        .logoutUrl("/bff/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value())))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        // The OpenAPI document (issue #73) describes the contract every
                        // client already calls — no data, no secrets — so it is public,
                        // letting the spec be fetched as a build artifact for client codegen.
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        // Self-service password reset and the "who appointed me?" lookup are
                        // reached from the login screen before the User can authenticate
                        // (ADR-0004), so they are the one set of public business endpoints.
                        .requestMatchers("/api/password-reset/**", "/api/who-appointed-me").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> {}));
        return http.build();
    }

    /**
     * Forces the deferred {@link CsrfToken} to be loaded on every BFF request so
     * {@link CookieCsrfTokenRepository} writes the {@code XSRF-TOKEN} cookie even
     * for safe requests — otherwise the SPA has no token to echo on its first
     * mutating call. (Spring Security single-page-application guidance.)
     */
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }

    /**
     * CSRF handler tuned for a JavaScript SPA (Spring Security single-page-app
     * guidance): renders the token XOR-masked into request attributes (BREACH
     * protection), but resolves the value the client sends in the
     * {@code X-XSRF-TOKEN} header as the raw cookie value — which is what Angular
     * echoes from the {@code XSRF-TOKEN} cookie.
     */
    static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                Supplier<CsrfToken> csrfToken) {
            xor.handle(request, response, csrfToken);
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            boolean fromHeader = StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()));
            return (fromHeader ? plain : xor).resolveCsrfTokenValue(request, csrfToken);
        }
    }
}
