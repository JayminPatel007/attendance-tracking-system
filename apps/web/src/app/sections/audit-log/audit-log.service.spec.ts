import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuditLogService } from './audit-log.service';

describe('AuditLogService', () => {
  let service: AuditLogService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuditLogService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('fetches the audit feed from the BFF without query params for an empty filter', () => {
    service.list({}).subscribe();

    const req = http.expectOne('/bff/audit-log');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.keys()).toEqual([]);
    req.flush([]);
  });

  it('serializes every set filter field as a query param', () => {
    service
      .list({
        targetType: 'OCCURRENCE',
        targetId: 'occ-1',
        actorUserId: 'user-7',
        action: 'REOPENED',
        from: '2026-06-01',
        to: '2026-06-09',
        proxyOnly: true,
      })
      .subscribe();

    const req = http.expectOne((r) => r.url === '/bff/audit-log');
    expect(req.request.params.get('targetType')).toBe('OCCURRENCE');
    expect(req.request.params.get('targetId')).toBe('occ-1');
    expect(req.request.params.get('actorUserId')).toBe('user-7');
    expect(req.request.params.get('action')).toBe('REOPENED');
    expect(req.request.params.get('from')).toBe('2026-06-01');
    expect(req.request.params.get('to')).toBe('2026-06-09');
    expect(req.request.params.get('proxyOnly')).toBe('true');
    req.flush([]);
  });

  it('omits the proxy toggle when it is off', () => {
    service.list({ action: 'CANCELLED', proxyOnly: false }).subscribe();

    const req = http.expectOne((r) => r.url === '/bff/audit-log');
    expect(req.request.params.keys()).toEqual(['action']);
    req.flush([]);
  });
});
