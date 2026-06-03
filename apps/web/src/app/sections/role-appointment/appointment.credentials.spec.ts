import { suggestPassword, suggestUsername } from './appointment.credentials';

describe('appointment credential suggestions', () => {
  it('derives a lowercase dotted username from a full name', () => {
    expect(suggestUsername('Fresh Sanchalak')).toBe('fresh.sanchalak');
  });

  it('strips punctuation and collapses whitespace', () => {
    expect(suggestUsername("  Jaymin   D'Souza  ")).toBe('jaymin.dsouza');
  });

  it('produces a 12-character password from the allowed alphabet', () => {
    const password = suggestPassword(() => 0.5);
    expect(password.length).toBe(12);
    expect(password).toMatch(/^[A-Za-z2-9]+$/);
  });
});
