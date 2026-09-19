package org.sabha.common.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds the signed-in caller — their local {@code users.id} and their live
 * {@code role_assignments} rows — to a controller parameter:
 *
 * <pre>{@code
 * @GetMapping("/api/sanchalak/current-roster")
 * ResponseEntity<CurrentRoster> currentRoster(@CurrentUser CallerAuthority caller) { ... }
 * }</pre>
 *
 * <p>The annotated parameter must be a {@link org.sabha.common.CallerAuthority};
 * the local {@code users.id} comes from {@code caller.userId()}. It was {@code
 * UserId} until ADR-0032 widened the type — the annotation itself is unchanged,
 * deliberately, because {@code every_handler_resolves_its_caller} (issue #209)
 * and ADR-0032's rule R2 both key off it and a new annotation would silently void
 * both.</p>
 *
 * <p>There is exactly <b>one</b> caller parameter type. That is what makes the
 * authority query happen once per request without anything resembling a cache,
 * and a {@code UserId}-typed relapse fails the build at R2 rather than quietly
 * issuing a second query.</p>
 *
 * <p>An authenticated request whose subject maps to no local user never reaches
 * the method body — it fails with {@link org.sabha.common.CallerUnknownException}
 * (403) at the edge. See ADR-0030, ADR-0032 and {@link
 * CurrentUserArgumentResolver}.</p>
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
