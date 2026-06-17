import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { DirectoryService } from './directory.service';
import { PersonPickerComponent } from './person-picker.component';

function dirSpy(): jasmine.SpyObj<DirectoryService> {
  const spy = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['searchByName', 'searchByMobile']);
  spy.searchByName.and.returnValue(of([{ personId: 'cand-1', fullName: 'Pratik Patel', homeSabhas: [] }]));
  spy.searchByMobile.and.returnValue(
    of({ id: 'm-1', fullName: 'Mobile Match', gender: 'MALE', dateOfBirth: null, mobile: '+919820000001', guardianPersonId: null }),
  );
  return spy;
}

/** Host that fixes the Kshetra from the form (the Sabha-definition shape). */
@Component({
  standalone: true,
  imports: [PersonPickerComponent],
  template: `<app-person-picker [kshetraId]="kshetraId" (picked)="pickedCount = pickedCount + 1" />`,
})
class FixedKshetraHost {
  kshetraId = 'ksh1';
  pickedCount = 0;
}

function mount(): { fixture: ComponentFixture<FixedKshetraHost>; picker: PersonPickerComponent; api: jasmine.SpyObj<DirectoryService> } {
  const api = dirSpy();
  TestBed.configureTestingModule({
    imports: [FixedKshetraHost],
    providers: [{ provide: DirectoryService, useValue: api }],
  });
  const fixture = TestBed.createComponent(FixedKshetraHost);
  fixture.detectChanges();
  const picker = fixture.debugElement.children[0].componentInstance as PersonPickerComponent;
  return { fixture, picker, api };
}

describe('PersonPickerComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('searches by name within the bound Kshetra and lists candidates', () => {
    const { picker, api } = mount();
    picker.nameQuery = 'Pratik';

    picker.searchByName();

    expect(api.searchByName).toHaveBeenCalledWith('ksh1', 'Pratik');
    expect(picker.candidates().length).toBe(1);
  });

  it('does not search by name when the name or Kshetra is blank', () => {
    const { picker, api } = mount();
    picker.nameQuery = '   ';

    picker.searchByName();

    expect(api.searchByName).not.toHaveBeenCalled();
  });

  it('searches by mobile and clears any name candidates', () => {
    const { picker } = mount();
    picker.nameQuery = 'Pratik';
    picker.searchByName();
    expect(picker.candidates().length).toBe(1);

    picker.mobileQuery = '+919820000001';
    picker.searchByMobile();

    expect(picker.mobileMatch()?.id).toBe('m-1');
    expect(picker.candidates().length).toBe(0);
  });

  it('clears the mobile match when a name search runs', () => {
    const { picker } = mount();
    picker.mobileQuery = '+919820000001';
    picker.searchByMobile();
    expect(picker.mobileMatch()).not.toBeNull();

    picker.nameQuery = 'Pratik';
    picker.searchByName();

    expect(picker.mobileMatch()).toBeNull();
  });

  it('clears the mobile match when the mobile lookup errors', () => {
    const { picker, api } = mount();
    api.searchByMobile.and.returnValue(throwError(() => new Error('not found')));
    picker.mobileQuery = '+910000000000';

    picker.searchByMobile();

    expect(picker.mobileMatch()).toBeNull();
  });

  it('records the pick, suggests credentials, and emits picked', () => {
    const { fixture, picker } = mount();

    picker.pick('cand-1', 'Pratik Patel');

    expect(picker.selectedId()).toBe('cand-1');
    expect(picker.selectedName).toBe('Pratik Patel');
    expect(picker.username).toBe('pratik.patel');
    expect(picker.rawPassword.length).toBe(12);
    expect(fixture.componentInstance.pickedCount).toBe(1);
  });

  it('does not search by name when the host has supplied no Kshetra', () => {
    const api = dirSpy();
    TestBed.configureTestingModule({
      imports: [FixedKshetraHost],
      providers: [{ provide: DirectoryService, useValue: api }],
    });
    const fixture = TestBed.createComponent(FixedKshetraHost);
    fixture.componentInstance.kshetraId = '';
    fixture.detectChanges();
    const picker = fixture.debugElement.children[0].componentInstance as PersonPickerComponent;
    picker.nameQuery = 'Pratik';

    picker.searchByName();

    expect(api.searchByName).not.toHaveBeenCalled();
  });

  it('clear() resets selection, credentials, and search state', () => {
    const { picker } = mount();
    picker.pick('cand-1', 'Pratik Patel');
    picker.mobileQuery = 'x';
    picker.nameQuery = 'y';

    picker.clear();

    expect(picker.selectedId()).toBeNull();
    expect(picker.selectedName).toBe('');
    expect(picker.username).toBe('');
    expect(picker.rawPassword).toBe('');
    expect(picker.candidates().length).toBe(0);
    expect(picker.mobileMatch()).toBeNull();
  });
});
