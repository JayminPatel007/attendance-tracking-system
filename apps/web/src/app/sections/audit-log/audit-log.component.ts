import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { errorMessageFor } from '../../shared/http-error';
import { AuditLogService } from './audit-log.service';
import { AuditEntry, AuditFilter, AuditTargetType, AUDIT_TARGET_TYPES } from './audit-log.types';

/**
 * Audit-log viewer section (ADR-0023, Slice 19). Renders the feed the BFF
 * returns — who may see it, and which entries, is entirely the backend's call
 * (Nirdeshak-and-above, geographically scoped); the shell gates the route by
 * the `AUDIT_LOG` section and a denial surfaces as a 403 message here.
 *
 * <ul>
 *   <li><b>Filter bar</b> — target type (closed enum), action, actor, date
 *       range (`to` inclusive), and the Slice 14 proxy-only toggle.</li>
 *   <li><b>Drill-down</b> — an entity detail screen deep-links here with
 *       `?targetType=…&targetId=…`; the pinned entity shows as a chip that can
 *       be cleared back to the feed.</li>
 *   <li><b>Proxy attribution</b> — an entry with `onBehalfOfUserId` carries an
 *       "on behalf of" badge (only ever Occurrence entries).</li>
 * </ul>
 */
@Component({
  selector: 'app-audit-log',
  standalone: true,
  imports: [DatePipe, FormsModule],
  templateUrl: './audit-log.component.html',
  styleUrl: './audit-log.component.scss',
})
export class AuditLogComponent implements OnInit {
  private readonly api = inject(AuditLogService);
  private readonly route = inject(ActivatedRoute);

  readonly targetTypes = AUDIT_TARGET_TYPES;

  targetType: '' | AuditTargetType = '';
  actorUserId = '';
  action = '';
  from = '';
  to = '';
  proxyOnly = false;

  readonly entries = signal<AuditEntry[]>([]);
  /** Set only via the deep link (`?targetType=…&targetId=…`): pins one entity's history. */
  readonly targetId = signal<string>('');
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const type = params.get('targetType');
      if (type && (AUDIT_TARGET_TYPES as readonly string[]).includes(type)) {
        this.targetType = type as AuditTargetType;
      }
      this.targetId.set(params.get('targetId') ?? '');
      this.load();
    });
  }

  apply(): void {
    this.load();
  }

  clearDrillDown(): void {
    this.targetId.set('');
    this.load();
  }

  private load(): void {
    this.error.set(null);
    this.api.list(this.filter()).subscribe({
      next: (items) => this.entries.set(items),
      error: (err: HttpErrorResponse) => {
        this.entries.set([]);
        this.error.set(this.messageFor(err));
      },
    });
  }

  private messageFor(err: HttpErrorResponse): string {
    return errorMessageFor(err, {
      byStatus: { 403: 'You are not authorized to view the audit log.' },
    });
  }

  private filter(): AuditFilter {
    const filter: AuditFilter = {};
    if (this.targetType) {
      filter.targetType = this.targetType;
    }
    if (this.targetId()) {
      filter.targetId = this.targetId();
    }
    if (this.actorUserId.trim()) {
      filter.actorUserId = this.actorUserId.trim();
    }
    if (this.action.trim()) {
      filter.action = this.action.trim();
    }
    if (this.from) {
      filter.from = this.from;
    }
    if (this.to) {
      filter.to = this.to;
    }
    if (this.proxyOnly) {
      filter.proxyOnly = true;
    }
    return filter;
  }
}
