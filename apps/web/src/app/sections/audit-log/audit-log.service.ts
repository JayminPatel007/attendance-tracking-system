import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AuditEntry, AuditLogBffControllerService } from 'shared-data-access';

/** The target kinds the viewer filters on, in the order the dropdown lists them. */
export const AUDIT_TARGET_TYPES: readonly AuditEntry.TargetTypeEnum[] = [
  'OCCURRENCE',
  'SABHA',
  'ROLE_ASSIGNMENT',
  'STRUCTURAL',
  'PERSON',
];

/**
 * What the viewer is asking for; every field optional, and an absent one means
 * "don't narrow on this". `from` / `to` are calendar dates (`yyyy-MM-dd`, `to`
 * inclusive — the server widens it to the next day). `targetType` + `targetId`
 * together are the per-entity drill-down that the reopen and appointment screens
 * deep-link into.
 *
 * This is the viewer's own query object, not a wire shape: the endpoint takes
 * seven independent query parameters, and nothing on the contract groups them.
 */
export interface AuditFilter {
  targetType?: AuditEntry.TargetTypeEnum;
  targetId?: string;
  actorUserId?: string;
  action?: string;
  from?: string;
  to?: string;
  proxyOnly?: boolean;
}

/**
 * The audit-log viewer's read (Slice 19, ADR-0023, ADR-0022): one scoped feed,
 * newest first. Authority and geographic scoping are entirely the backend's
 * call, so a denial surfaces as a 403 here.
 *
 * What this adds over the generated client is the {@link AuditFilter} itself.
 * The generated operation takes the seven filters as seven positional optional
 * arguments, so every caller would otherwise have to spell out the full
 * argument list — and get its order right — to narrow on any one of them.
 */
@Injectable({ providedIn: 'root' })
export class AuditLogService {
  private readonly api = inject(AuditLogBffControllerService);

  list(filter: AuditFilter): Observable<AuditEntry[]> {
    return this.api.list1(
      filter.targetType,
      filter.targetId,
      filter.actorUserId,
      filter.action,
      filter.from,
      filter.to,
      filter.proxyOnly,
    );
  }
}
