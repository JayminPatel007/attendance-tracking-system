import { routes } from './app.routes';
import { AuditLogComponent } from './sections/audit-log/audit-log.component';
import { MyAuthorityComponent } from './sections/my-authority/my-authority.component';
import { UNGATED_NAV } from './shell/section-nav';

describe('app routes', () => {
  it('routes audit-log to the viewer behind the section guard (Slice 19)', () => {
    const shell = routes.find((route) => route.path === '');
    const auditLog = shell?.children?.find((route) => route.path === 'audit-log');

    expect(auditLog?.component).toBe(AuditLogComponent);
    expect(auditLog?.canActivate?.length).toBe(1);
    expect(auditLog?.data?.['section']).toBe('AUDIT_LOG');
  });

  it('lazily routes my-authority unguarded — reference material, not a granted section (#90)', async () => {
    const shell = routes.find((route) => route.path === '');
    const myAuthority = shell?.children?.find((route) => route.path === 'my-authority');

    expect(myAuthority?.canActivate).toBeUndefined();
    expect(myAuthority?.component).toBeUndefined();
    await expectAsync(
      (myAuthority!.loadComponent as () => Promise<unknown>)(),
    ).toBeResolvedTo(MyAuthorityComponent);
  });

  it('keeps the catch-all last so my-authority is reachable', () => {
    const children = routes.find((route) => route.path === '')?.children ?? [];

    expect(children[children.length - 1].path).toBe('**');
  });

  it('routes every ungated sidebar entry, so no nav item dead-ends on the catch-all', () => {
    const paths = (routes.find((route) => route.path === '')?.children ?? []).map(
      (route) => route.path,
    );

    UNGATED_NAV.forEach((item) => expect(paths).withContext(item.label).toContain(item.path));
  });
});
