import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DeleteButtonComponent } from './delete-button.component';

function mount(): ComponentFixture<DeleteButtonComponent> {
  TestBed.configureTestingModule({ imports: [DeleteButtonComponent] });
  return TestBed.createComponent(DeleteButtonComponent);
}

function button(fixture: ComponentFixture<DeleteButtonComponent>): HTMLButtonElement {
  return (fixture.nativeElement as HTMLElement).querySelector('button.delete')!;
}

describe('DeleteButtonComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('renders an active Delete button and emits when empty (no reason)', () => {
    const fixture = mount();
    const emitted = jasmine.createSpy('confirmed');
    fixture.componentInstance.confirmed.subscribe(emitted);
    fixture.detectChanges();

    const btn = button(fixture);
    expect(btn.disabled).toBeFalse();
    expect(btn.textContent).toContain('Delete');

    btn.click();
    expect(emitted).toHaveBeenCalledTimes(1);
  });

  it('disables the button and shows the reason when one is given', () => {
    const fixture = mount();
    fixture.componentInstance.reason = 'has 3 Zones';
    fixture.detectChanges();

    const btn = button(fixture);
    expect(btn.disabled).toBeTrue();
    expect(btn.textContent).toContain('has 3 Zones');
    expect(btn.title).toBe('has 3 Zones');
  });
});
