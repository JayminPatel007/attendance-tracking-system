import { notEmptyReason } from './structural';

describe('notEmptyReason', () => {
  it('returns null when the entity is empty and thus deletable', () => {
    expect(notEmptyReason(0, 'Zone')).toBeNull();
    expect(notEmptyReason(-1, 'Zone')).toBeNull();
  });

  it('keeps the singular wording byte-identical to the backend', () => {
    expect(notEmptyReason(1, 'Zone')).toBe('has 1 Zone');
    expect(notEmptyReason(1, 'Occurrence')).toBe('has 1 Occurrence');
  });

  it('pluralizes with a trailing s for counts above one', () => {
    expect(notEmptyReason(6, 'Kshetra')).toBe('has 6 Kshetras');
    expect(notEmptyReason(12, 'Occurrence')).toBe('has 12 Occurrences');
  });
});
