import { routes } from './app.routes';
import { AuditLogComponent } from './sections/audit-log/audit-log.component';

describe('app routes', () => {
  it('routes audit-log to the viewer behind the section guard (Slice 19)', () => {
    const shell = routes.find((route) => route.path === '');
    const auditLog = shell?.children?.find((route) => route.path === 'audit-log');

    expect(auditLog?.component).toBe(AuditLogComponent);
    expect(auditLog?.canActivate?.length).toBe(1);
    expect(auditLog?.data?.['section']).toBe('AUDIT_LOG');
  });
});
