import { demographicLabel, kindLabel } from './sabha-kind-label';

describe('sabha-kind-label', () => {
  it('decodes a TRACK_DEMOGRAPHIC token into "Demographic (Track)"', () => {
    expect(kindLabel('REGULAR_YUVAK')).toBe('Yuvak (Regular)');
    expect(kindLabel('BSS_BAAL')).toBe('Baal (Bss)');
  });

  it('passes through a token with no track separator unchanged', () => {
    expect(kindLabel('YUVAK')).toBe('YUVAK');
  });

  it('labels a bare demographic token', () => {
    expect(demographicLabel('SANYUKTA')).toBe('Sanyukta');
  });

  it('falls back to the raw token for an unknown demographic', () => {
    expect(demographicLabel('UNKNOWN')).toBe('UNKNOWN');
  });
});
