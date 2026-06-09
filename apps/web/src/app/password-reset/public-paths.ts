/**
 * The unauthenticated password-reset routes (ADR-0004, Slice 18B). These live
 * outside the shell and must be reachable **before** any OIDC session exists, so
 * the app initializer skips resolving `/bff/me` (which would otherwise bounce a
 * session-less visitor into the Keycloak login) when the browser is on one of
 * them.
 */
export const PUBLIC_PATHS = ['/forgot-password', '/who-appointed-me'] as const;

/** Whether `path` is one of the public reset routes (exact segment match). */
export function isPublicPath(path: string): boolean {
  return PUBLIC_PATHS.some((base) => path === base || path.startsWith(`${base}/`));
}
