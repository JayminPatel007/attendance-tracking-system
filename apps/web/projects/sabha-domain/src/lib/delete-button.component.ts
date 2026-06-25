import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * The block-if-non-empty delete control shared by the structural-admin and
 * Sabha-definition lists (ADR-0026). Given a {@link reason} (from
 * {@link notEmptyReason}), it renders a disabled button carrying that reason;
 * given `null`, it renders an active Delete button that emits {@link confirmed}.
 * Centralizing the contract keeps every deletable list rendering it identically.
 */
@Component({
  selector: 'app-delete-button',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (reason) {
      <button type="button" class="delete" disabled [title]="reason">{{ reason }}</button>
    } @else {
      <button type="button" class="delete" (click)="confirmed.emit()">Delete</button>
    }
  `,
  styles: `
    .delete {
      flex: none;
      padding: 6px 10px;
      border: 1px solid #fca5a5;
      border-radius: 6px;
      background: #fff;
      color: #b91c1c;
      cursor: pointer;
      white-space: nowrap;
    }
    .delete:disabled {
      border-color: #e5e7eb;
      color: #9ca3af;
      cursor: not-allowed;
    }
  `,
})
export class DeleteButtonComponent {
  /** The blocking reason; when set the button is disabled and shows it, else it deletes. */
  @Input() reason: string | null = null;

  /** Emitted when an enabled (empty-entity) delete is confirmed. */
  @Output() confirmed = new EventEmitter<void>();
}
