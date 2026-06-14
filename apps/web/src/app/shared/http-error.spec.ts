import { HttpErrorResponse } from '@angular/common/http';

import { errorMessageFor } from './http-error';

function errorAt(status: number, error?: unknown): HttpErrorResponse {
  return new HttpErrorResponse({ status, error });
}

describe('errorMessageFor', () => {
  it('returns the byStatus override for a matched status', () => {
    const msg = errorMessageFor(errorAt(403), {
      byStatus: { 403: 'not authorized here' },
    });

    expect(msg).toBe('not authorized here');
  });

  it('prefers a byCode override over byStatus for the same response', () => {
    const msg = errorMessageFor(errorAt(409, { code: 'USERNAME_TAKEN' }), {
      byCode: { USERNAME_TAKEN: 'username taken' },
      byStatus: { 409: 'mobile already used' },
    });

    expect(msg).toBe('username taken');
  });

  it('falls back to the generic message when nothing matches', () => {
    expect(errorMessageFor(errorAt(500), { byStatus: { 403: 'x' } })).toBe(
      'Something went wrong — please try again.',
    );
  });

  it('falls back to the generic message when called with no overrides', () => {
    expect(errorMessageFor(errorAt(403))).toBe('Something went wrong — please try again.');
  });
});
