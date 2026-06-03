/**
 * Auto-suggested credentials for the appointment form (ADR-0011): a username
 * derived from the appointee's name and a throwaway initial password the
 * appointee is forced to change on first login. Both are editable by the
 * appointer; username uniqueness is the backend's call (409 on collision).
 */

/** A lowercase dotted handle from a full name, e.g. "Fresh Sanchalak" → "fresh.sanchalak". */
export function suggestUsername(fullName: string): string {
  return fullName
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, '')
    .replace(/\s+/g, '.');
}

const PASSWORD_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789';

/** A 12-character throwaway password; `random` is injectable for deterministic tests. */
export function suggestPassword(random: () => number = Math.random): string {
  let out = '';
  for (let i = 0; i < 12; i++) {
    out += PASSWORD_ALPHABET[Math.floor(random() * PASSWORD_ALPHABET.length)];
  }
  return out;
}
