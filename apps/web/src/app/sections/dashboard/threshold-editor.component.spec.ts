import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { DashboardBffControllerService, Thresholds, WebSessionResponse } from 'shared-data-access';
import { SessionService } from 'identity-domain';

import { ThresholdEditorComponent } from './threshold-editor.component';

import { ApiStub, apiStub } from '../../shared/api-stub.testing';

function sessionStub(madhyasthaKaryalaya: boolean): Partial<SessionService> {
  return {
    session: signal<WebSessionResponse | null>({ username: 'u', madhyasthaKaryalaya, regionalTeam: false, sections: ['DASHBOARD'] }),
  } as Partial<SessionService>;
}

function apiSpy(current: Thresholds): ApiStub<DashboardBffControllerService> {
  const spy = apiStub<DashboardBffControllerService>('DashboardBffControllerService', ['thresholds', 'updateThresholds']);
  spy.thresholds.and.returnValue(of(current));
  spy.updateThresholds.and.returnValue(of(undefined));
  return spy;
}

function mount(
  madhyasthaKaryalaya: boolean,
  current: Thresholds = { candidate: 3, priority: 6 },
): {
  fixture: ComponentFixture<ThresholdEditorComponent>;
  api: ApiStub<DashboardBffControllerService>;
} {
  const api = apiSpy(current);
  TestBed.configureTestingModule({
    imports: [ThresholdEditorComponent],
    providers: [
      { provide: DashboardBffControllerService, useValue: api },
      { provide: SessionService, useValue: sessionStub(madhyasthaKaryalaya) },
    ],
  });
  const fixture = TestBed.createComponent(ThresholdEditorComponent);
  fixture.detectChanges();
  return { fixture, api };
}

describe('ThresholdEditorComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('renders nothing and reads no thresholds for a non-MK caller', () => {
    const { fixture, api } = mount(false);

    expect(api.thresholds).not.toHaveBeenCalled();
    expect((fixture.nativeElement as HTMLElement).textContent?.trim()).toBe('');
  });

  it('loads the current thresholds for an MK caller', () => {
    const { fixture, api } = mount(true, { candidate: 3, priority: 6 });

    expect(api.thresholds).toHaveBeenCalled();
    expect(fixture.componentInstance.candidate).toBe(3);
    expect(fixture.componentInstance.priority).toBe(6);
  });

  it('saves edited thresholds via PUT', () => {
    const { fixture, api } = mount(true);
    const c = fixture.componentInstance;
    c.candidate = 2;
    c.priority = 5;

    c.save();

    expect(api.updateThresholds).toHaveBeenCalledWith({ candidate: 2, priority: 5 });
  });

  it('rejects an invalid pair client-side without calling the BFF', () => {
    const { fixture, api } = mount(true);
    const c = fixture.componentInstance;
    c.candidate = 5;
    c.priority = 2;

    expect(c.valid()).toBeFalse();
    c.save();

    expect(api.updateThresholds).not.toHaveBeenCalled();
    expect(c.error()).not.toBeNull();
  });

  it('surfaces a 422 from the BFF as a validation message', () => {
    const { fixture, api } = mount(true);
    api.updateThresholds.and.returnValue(throwError(() => new HttpErrorResponse({ status: 422 })));
    const c = fixture.componentInstance;
    c.candidate = 1;
    c.priority = 2;

    c.save();

    expect(c.error()).not.toBeNull();
  });

  it('confirms a successful save', () => {
    const { fixture } = mount(true);
    const c = fixture.componentInstance;
    c.candidate = 4;
    c.priority = 8;

    c.save();

    expect(c.saved()).toBeTrue();
  });
});
