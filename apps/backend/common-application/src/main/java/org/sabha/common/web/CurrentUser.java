package org.sabha.common.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds the signed-in caller's local {@code users.id} to a controller parameter:
 *
 * <pre>{@code
 * @GetMapping("/api/sanchalak/current-roster")
 * ResponseEntity<CurrentRoster> currentRoster(@CurrentUser UserId caller) { ... }
 * }</pre>
 *
 * <p>The annotated parameter must be a {@link org.sabha.common.UserId}. An
 * authenticated request whose subject maps to no local user never reaches the
 * method body — it fails with {@link org.sabha.common.CallerUnknownException}
 * (403) at the edge. See ADR-0030 and {@link CurrentUserArgumentResolver}.</p>
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
