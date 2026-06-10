import { Routes } from '@angular/router';

import { ForgotPasswordComponent } from './password-reset/forgot-password.component';
import { WhoAppointedMeComponent } from './password-reset/who-appointed-me.component';
import { AuditLogComponent } from './sections/audit-log/audit-log.component';
import { DashboardComponent } from './sections/dashboard/dashboard.component';
import { OccurrenceReopenComponent } from './sections/occurrence-reopen/occurrence-reopen.component';
import { RoleAppointmentComponent } from './sections/role-appointment/role-appointment.component';
import { SabhaDefinitionComponent } from './sections/sabha-definition/sabha-definition.component';
import { SanchalakProxyComponent } from './sections/sanchalak-proxy/sanchalak-proxy.component';
import { SectionPlaceholderComponent } from './sections/section-placeholder.component';
import { SelectionComponent } from './sections/selection/selection.component';
import { StructuralAdminComponent } from './sections/structural-admin/structural-admin.component';
import { SECTION_NAV } from './shell/section-nav';
import { sectionGuard } from './shell/section.guard';
import { ShellComponent } from './shell/shell.component';

/** Sections with a real screen; the rest fall back to the placeholder (later slices). */
const SECTION_COMPONENTS = {
  DASHBOARD: DashboardComponent,
  ROLE_APPOINTMENT: RoleAppointmentComponent,
  STRUCTURAL_ADMIN: StructuralAdminComponent,
  SABHA_DEFINITION: SabhaDefinitionComponent,
  OCCURRENCE_REOPEN: OccurrenceReopenComponent,
  SANCHALAK_PROXY: SanchalakProxyComponent,
  SELECTION: SelectionComponent,
  AUDIT_LOG: AuditLogComponent,
} as const;

/** One guarded route per shell section, derived from the nav model. */
const sectionRoutes: Routes = SECTION_NAV.map((item) => ({
  path: item.path,
  component:
    SECTION_COMPONENTS[item.section as keyof typeof SECTION_COMPONENTS] ?? SectionPlaceholderComponent,
  canActivate: [sectionGuard],
  data: { section: item.section, label: item.label },
}));

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
      { path: '**', redirectTo: 'dashboard' },
    ],
  },
];
