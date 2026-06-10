import { Section } from 'identity-domain';

/** A sidebar entry: which {@link Section} it represents, its route, its label. */
export interface SectionNavItem {
  section: Section;
  path: string;
  label: string;
}

/**
 * The full set of shell sections in display order (Slice 9). Each user sees the
 * subset the BFF returns on `/bff/me`; the sidebar and the route guard both read
 * this single source of truth. Later slices fill each route with real screens.
 */
export const SECTION_NAV: readonly SectionNavItem[] = [
  { section: 'DASHBOARD', path: 'dashboard', label: 'Dashboard' },
  { section: 'ROLE_APPOINTMENT', path: 'role-appointment', label: 'Role Appointment' },
  { section: 'STRUCTURAL_ADMIN', path: 'structural-admin', label: 'Structural Admin' },
  { section: 'SABHA_DEFINITION', path: 'sabha-definition', label: 'Sabha Definition' },
  { section: 'OCCURRENCE_REOPEN', path: 'occurrence-reopen', label: 'Occurrence Reopen' },
  { section: 'SANCHALAK_PROXY', path: 'sanchalak-proxy', label: 'Sanchalak Proxy' },
  { section: 'SELECTION', path: 'selection', label: 'Selection' },
  { section: 'AUDIT_LOG', path: 'audit-log', label: 'Audit Log' },
];
