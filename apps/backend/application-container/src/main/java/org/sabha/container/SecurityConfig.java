package org.sabha.container;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * HTTP security policy for the whole deployable (ADR-0019). Two filter chains
 * (ADR-0022):
 *
 * <ul>
 *   <li><b>Web BFF chain</b> (order 1, {@code /bff/**} + the OIDC login/logout
 *       endpoints): server-side {@code oauth2Login}, tokens kept in the
 *       HttpSession, the browser holds only an HTTP-only session cookie. CSRF is
 *       enabled (cookie + header double-submit); an unauthenticated XHR gets a
 *       401 instead of a login redirect so the SPA decides when to authenticate.</li>
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
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
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
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> {}));
        return http.build();
    }
}
