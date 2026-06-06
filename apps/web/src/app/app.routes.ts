import { Routes } from '@angular/router';

import { DashboardComponent } from './sections/dashboard/dashboard.component';
import { OccurrenceReopenComponent } from './sections/occurrence-reopen/occurrence-reopen.component';
import { RoleAppointmentComponent } from './sections/role-appointment/role-appointment.component';
import { SabhaDefinitionComponent } from './sections/sabha-definition/sabha-definition.component';
import { SanchalakProxyComponent } from './sections/sanchalak-proxy/sanchalak-proxy.component';
import { SectionPlaceholderComponent } from './sections/section-placeholder.component';
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
