import { Routes } from '@angular/router';

import { SectionPlaceholderComponent } from './sections/section-placeholder.component';
import { SECTION_NAV } from './shell/section-nav';
import { sectionGuard } from './shell/section.guard';
import { ShellComponent } from './shell/shell.component';

/** One guarded placeholder route per shell section, derived from the nav model. */
const sectionRoutes: Routes = SECTION_NAV.map((item) => ({
  path: item.path,
  component: SectionPlaceholderComponent,
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
