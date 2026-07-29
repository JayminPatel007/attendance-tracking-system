import { AppointmentRequest } from 'shared-data-access';

/**
 * Which scope shape each appointable role needs the console to ask for
 * (ADR-0011). A Sabha-scoped role names a Sabha; the Kshetra / Zone / City tiers
 * name that id plus a demographic. This is the form's own policy, not a mirror
 * of a wire shape — the request carries all four id slots and the backend
 * arbitrates which one the role requires, so nothing on the contract says which
 * single input to render.
 */
export type AppointableRole = AppointmentRequest.RoleEnum;

export type ScopeKind = 'SABHA' | 'KSHETRA' | 'ZONE' | 'CITY';

export const ROLE_SCOPE: Record<AppointableRole, ScopeKind> = {
  SANCHALAK: 'SABHA',
  SAH_SANCHALAK: 'SABHA',
  NIRIKSHAK: 'KSHETRA',
  SAH_NIRDESHAK: 'KSHETRA',
  NIRDESHAK: 'KSHETRA',
  SANYOJAK: 'ZONE',
  REGIONAL_TEAM: 'CITY',
  SANT: 'CITY',
};
