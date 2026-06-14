import { HttpErrorResponse } from '@angular/common/http';

/**
 * Feature-specific message overrides for {@link errorMessageFor}. A section
 * supplies only the copy that differs from the shared default: messages keyed by
 * the ProblemDetail `code` extension (checked first) and/or by HTTP status.
 */
export interface ErrorMessageOverrides {
  byCode?: Record<string, string>;
  byStatus?: Record<number, string>;
}

/** The shared default shown when no feature override claims the failure. */
export const GENERIC_ERROR_MESSAGE = 'Something went wrong — please try again.';

/**
 * Owns the status/code → user-message logic for the BFF section flows. The
 * backend's error contract (RFC 9457 ProblemDetail, ADR per #70) is parsed here
 * once: a `code` override wins over a `status` override, and anything unmatched
 * falls back to {@link GENERIC_ERROR_MESSAGE}. Sections collapse their old
 * private `messageFor` to just the {@link ErrorMessageOverrides} that differ.
 */
export function errorMessageFor(
  err: HttpErrorResponse,
  overrides: ErrorMessageOverrides = {},
): string {
  const code = err.error?.code as string | undefined;
  if (code && overrides.byCode?.[code]) {
    return overrides.byCode[code];
  }
  const byStatus = overrides.byStatus?.[err.status];
  if (byStatus) {
    return byStatus;
  }
  return GENERIC_ERROR_MESSAGE;
}
