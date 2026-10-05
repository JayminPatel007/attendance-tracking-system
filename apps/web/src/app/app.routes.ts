import { Type } from '@angular/core';
import { Routes } from '@angular/router';
import { WebSessionResponse } from 'shared-data-access';

import { ForgotPasswordComponent } from './password-reset/forgot-password.component';
import { WhoAppointedMeComponent } from './password-reset/who-appointed-me.component';
import { AuditLogComponent } from './sections/audit-log/audit-log.component';
import { DashboardComponent } from './sections/dashboard/dashboard.component';
import { OccurrenceReopenComponent } from './sections/occurrence-reopen/occurrence-reopen.component';
import { RoleAppointmentComponent } from './sections/role-appointment/role-appointment.component';
import { SabhaDefinitionComponent } from './sections/sabha-definition/sabha-definition.component';
import { SanchalakProxyComponent } from './sections/sanchalak-proxy/sanchalak-proxy.component';
import { SelectionComponent } from './sections/selection/selection.component';
import { StructuralAdminComponent } from './sections/structural-admin/structural-admin.component';
import { SECTION_NAV, UNGATED_NAV } from './shell/section-nav';
import { sectionGuard } from './shell/section.guard';
import { ShellComponent } from './shell/shell.component';

/** The screen behind each shell section; a section the BFF adds without one does not compile. */
const SECTION_COMPONENTS = {
  DASHBOARD: DashboardComponent,
  ROLE_APPOINTMENT: RoleAppointmentComponent,
  STRUCTURAL_ADMIN: StructuralAdminComponent,
  SABHA_DEFINITION: SabhaDefinitionComponent,
  OCCURRENCE_REOPEN: OccurrenceReopenComponent,
  SANCHALAK_PROXY: SanchalakProxyComponent,
  SELECTION: SelectionComponent,
  AUDIT_LOG: AuditLogComponent,
} as const satisfies Record<WebSessionResponse.SectionsEnum, Type<unknown>>;

/** One guarded route per shell section, derived from the nav model. */
const sectionRoutes: Routes = SECTION_NAV.map((item) => ({
  path: item.path,
  component: SECTION_COMPONENTS[item.section],
  canActivate: [sectionGuard],
  data: { section: item.section, label: item.label },
}));

/**
 * The routes {@link UNGATED_NAV} lists — open to every signed-in user, so no
 * section guard. Loaded lazily: reference material everyone carries but few open
 * has no business in the initial bundle.
 */
const ungatedRoutes: Routes = [
  {
    path: 'my-authority',
    loadComponent: () =>
      import('./sections/my-authority/my-authority.component').then((m) => m.MyAuthorityComponent),
    data: { label: 'My Authority' },
  },
];

export const routes: Routes = [
  // Public, unauthenticated reset routes (ADR-0004, Slice 18B) — matched before
  // the shell so a locked-out user reaches them without an OIDC session.
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'who-appointed-me', component: WhoAppointedMeComponent },
  {
    path: '',
    component: ShellComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      ...sectionRoutes,
      ...ungatedRoutes,
      { path: '**', redirectTo: 'dashboard' },
    ],
  },
];
