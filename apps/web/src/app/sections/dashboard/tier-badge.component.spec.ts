import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CandidateRow } from 'shared-data-access';

import { TierBadgeComponent } from './tier-badge.component';

function mount(tier: CandidateRow.TierEnum): ComponentFixture<TierBadgeComponent> {
  TestBed.configureTestingModule({ imports: [TierBadgeComponent] });
  const fixture = TestBed.createComponent(TierBadgeComponent);
  fixture.componentRef.setInput('tier', tier);
  fixture.detectChanges();
  return fixture;
}

describe('TierBadgeComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('labels a CANDIDATE tier', () => {
    const fixture = mount('CANDIDATE');
    expect((fixture.nativeElement as HTMLElement).textContent?.trim()).toBe('Candidate');
  });

  it('labels and flags a PRIORITY tier', () => {
    const fixture = mount('PRIORITY');
    const badge = (fixture.nativeElement as HTMLElement).querySelector('.tier-badge');

    expect(badge?.textContent?.trim()).toBe('Priority');
    expect(badge?.classList.contains('tier-badge--priority')).toBeTrue();
  });
});
