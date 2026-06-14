import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { provideApi } from '../../generated';
import { SelectionService } from './selection.service';

describe('SelectionService', () => {
  let service: SelectionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      // provideApi pins the generated client's base path to "" so it issues the
      // same relative URLs this spec asserts (it would otherwise default to the
      // spec server "http://localhost").
      providers: [provideHttpClient(), provideHttpClientTesting(), provideApi({ basePath: '' })],
    });
    service = TestBed.inject(SelectionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists the pending nomination queue from the BFF', () => {
    service.queue().subscribe();

    const req = http.expectOne('/bff/selection/nominations');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('lists the currently-selected People from the BFF', () => {
    service.selected().subscribe();

    const req = http.expectOne('/bff/selection/selected');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('posts an approve for a nomination to the BFF', () => {
    service.approve('nom-1').subscribe();

    const req = http.expectOne('/bff/selection/nominations/nom-1/approve');
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });

  it('posts a reject with the reason to the BFF', () => {
    service.reject('nom-1', 'Not ready yet').subscribe();

    const req = http.expectOne('/bff/selection/nominations/nom-1/reject');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ reason: 'Not ready yet' });
    req.flush(null);
  });

  it('posts a deselect with the person and selective Sabha to the BFF', () => {
    service.deselect('person-1', 'sabha-9').subscribe();

    const req = http.expectOne('/bff/selection/deselect');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ personId: 'person-1', selectiveSabhaId: 'sabha-9' });
    req.flush(null);
  });
});
