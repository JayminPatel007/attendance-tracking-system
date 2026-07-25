import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SessionService, WebSession } from 'identity-domain';

import { MyAuthorityComponent } from './my-authority.component';

type Fixture = ComponentFixture<MyAuthorityComponent>;

function session(overrides: Partial<WebSession> = {}): WebSession {
  return {
    username: 'karyakar',
    madhyasthaKaryalaya: false,
    regionalTeam: false,
    sections: ['DASHBOARD'],
    ...overrides,
  };
}

function render(current: WebSession = session()): Fixture {
  TestBed.configureTestingModule({
    imports: [MyAuthorityComponent],
    providers: [
      {
        provide: SessionService,
        useValue: { session: signal<WebSession | null>(current).asReadonly() },
      },
    ],
  });
  const fixture = TestBed.createComponent(MyAuthorityComponent);
  fixture.detectChanges();
  return fixture;
}

function texts(fixture: Fixture, selector: string): string[] {
  return Array.from(fixture.nativeElement.querySelectorAll(selector)).map((el) =>
    (el as HTMLElement).textContent!.trim().replace(/\s+/g, ' '),
  );
}

function selectTier(fixture: Fixture, label: string): void {
  const button = Array.from(
    fixture.nativeElement.querySelectorAll('.actor-switcher button'),
  ).find((el) => (el as HTMLElement).textContent!.trim() === label) as HTMLElement;
  button.click();
  fixture.detectChanges();
}

function rowNames(fixture: Fixture, column: 'structures' | 'roles'): string[] {
  return texts(fixture, `.column-${column} .item-name`);
}

describe('MyAuthorityComponent', () => {
  it('offers every tier in the switcher', () => {
    const fixture = render();

    expect(texts(fixture, '.actor-switcher button')).toEqual([
      'Madhyastha Karyalaya',
      'Regional Team',
      'Sanyojak',
      'Nirdeshak',
      'Sah-Nirdeshak',
    ]);
  });

  it('explains the three delete kinds in the legend', () => {
    const fixture = render();

    const legend = texts(fixture, '.legend .legend-item');
    expect(legend.length).toBe(3);
    expect(legend.join(' ')).toContain('Block-if-non-empty');
    expect(legend.join(' ')).toContain('Soft-retire');
    expect(legend.join(' ')).toContain('Revoke assignment');
  });

  it('lists what the selected tier creates in each column, with its delete rule', () => {
    const fixture = render();

    selectTier(fixture, 'Sanyojak');

    expect(rowNames(fixture, 'structures')).toEqual(['Kshetra']);
    expect(rowNames(fixture, 'roles')).toEqual(['Nirdeshak']);
    expect(texts(fixture, '.column-structures .create-badge')).toEqual(['Create']);
    expect(texts(fixture, '.column-roles .create-badge')).toEqual(['Appoint']);
    expect(texts(fixture, '.column-structures .delete-badge')).toEqual(['Block-if-non-empty']);
    expect(texts(fixture, '.column-roles .delete-badge')).toEqual(['Revoke assignment']);
  });

  it('labels each row with its own verb — a Sant is provisioned, not appointed', () => {
    const fixture = render();

    selectTier(fixture, 'Madhyastha Karyalaya');

    expect(texts(fixture, '.column-roles .create-badge')).toEqual(['Appoint', 'Provision']);
  });

  it('swaps the whole matrix when the actor changes', () => {
    const fixture = render();

    selectTier(fixture, 'Madhyastha Karyalaya');
    expect(rowNames(fixture, 'structures')).toEqual(['City', 'Sabha Kind']);
    expect(texts(fixture, '.column-structures .delete-badge')).toContain('Soft-retire');

    selectTier(fixture, 'Nirdeshak');
    expect(rowNames(fixture, 'structures')).toEqual(['Sabha']);
    expect(rowNames(fixture, 'roles')).toEqual([
      'Sanchalak',
      'Sah-Sanchalak',
      'Nirikshak',
      'Sah-Nirdeshak',
    ]);
  });

  it('shows Sah-Nirdeshak its operational powers and an explicit no-create/delete note', () => {
    const fixture = render();

    selectTier(fixture, 'Sah-Nirdeshak');

    expect(texts(fixture, '.operational .item-name').length).toBeGreaterThan(0);
    expect(texts(fixture, '.no-authority')[0]).toContain('no structural creation');
    expect(rowNames(fixture, 'structures')).toEqual([]);
    expect(rowNames(fixture, 'roles')).toEqual([]);
  });

  it('hides the operational panel for tiers that hold real authority', () => {
    const fixture = render();

    selectTier(fixture, 'Nirdeshak');

    expect(texts(fixture, '.operational')).toEqual([]);
    expect(texts(fixture, '.no-authority')).toEqual([]);
  });

  it('opens on the signed-in member’s own tier when the session names it', () => {
    const mk = render(session({ madhyasthaKaryalaya: true }));
    expect(texts(mk, '.actor-switcher button.active')).toEqual(['Madhyastha Karyalaya']);

    TestBed.resetTestingModule();
    const regionalTeam = render(session({ regionalTeam: true }));
    expect(texts(regionalTeam, '.actor-switcher button.active')).toEqual(['Regional Team']);
  });
});
