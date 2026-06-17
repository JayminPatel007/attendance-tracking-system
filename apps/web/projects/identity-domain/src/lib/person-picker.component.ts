import { Component, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { suggestPassword, suggestUsername } from './appointment-credentials';
import { DirectoryService } from './directory.service';
import { NameCandidate, PersonResponse } from './directory.types';

/**
 * The shared Directory person-picker (ADR-0011, ADR-0013): the directory-first
 * search flow every appointing screen composes. Search by mobile (one hit) or by
 * name within a Kshetra (a candidate list), pick a Person, and the initial
 * credentials are auto-suggested from their name (editable; uniqueness is the
 * backend's call). The two searches clear each other's result so only one set of
 * candidates is ever shown.
 *
 * The host supplies the Kshetra to search names within via `kshetraId` — whether
 * that is fixed by the form (Sabha-definition) or typed by the appointer
 * (Role-appointment) is the host's business, not the picker's. The host reads
 * `selectedId()`, `username`, and `rawPassword` to build its own submit payload.
 */
@Component({
  selector: 'app-person-picker',
  standalone: true,
  imports: [FormsModule],
  template: `
    @if (selectedId()) {
      <div class="chosen" role="status">
        ✓ Chosen: <strong>{{ selectedName }}</strong>
        @if (clearable()) {
          <button type="button" class="clear" (click)="clear()">Clear</button>
        }
      </div>
      <div class="credentials">
        <label>Username <input [(ngModel)]="username" /></label>
        <label>Initial password <input [(ngModel)]="rawPassword" /></label>
        <small class="hint">Handed to the appointee — they change it on first login (ADR-0011).</small>
      </div>
    } @else {
      <div class="search-row">
        <input [(ngModel)]="nameQuery" placeholder="Search by name" />
        <button type="button" (click)="searchByName()" [disabled]="!kshetraId()">Find</button>
      </div>
      <div class="search-row">
        <input [(ngModel)]="mobileQuery" placeholder="Search by mobile" />
        <button type="button" (click)="searchByMobile()">Find</button>
      </div>

      <ul class="candidates">
        @if (mobileMatch(); as m) {
          <li>
            <button type="button" (click)="pick(m.id, m.fullName)">
              {{ m.fullName }} <small>{{ m.mobile }}</small>
            </button>
          </li>
        }
        @for (c of candidates(); track c.personId) {
          <li>
            <button type="button" (click)="pick(c.personId, c.fullName)">
              {{ c.fullName }} <small>{{ c.homeSabhas.join(', ') }}</small>
            </button>
          </li>
        }
      </ul>
    }
  `,
  styles: `
    :host {
      display: block;
    }

    .search-row {
      display: flex;
      gap: 8px;
      align-items: flex-end;
      margin-bottom: 10px;
    }

    .search-row input {
      flex: 1;
      padding: 8px 10px;
      border: 1px solid #d1d5db;
      border-radius: 6px;
      box-sizing: border-box;
    }

    .search-row button {
      flex: 0 0 auto;
      padding: 8px 14px;
    }

    .candidates {
      list-style: none;
      margin: 8px 0 0;
      padding: 0;
    }

    .candidates li {
      margin: 0 0 6px;
    }

    .candidates li button {
      width: 100%;
      text-align: left;
      padding: 10px 12px;
      border: 1px solid #e5e7eb;
      border-radius: 6px;
      background: #fff;
      cursor: pointer;
    }

    .candidates li button small {
      color: #6b7280;
      margin-left: 8px;
    }

    .chosen {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 10px 12px;
      border: 1px solid #bbf7d0;
      background: #f0fdf4;
      border-radius: 6px;
      margin-bottom: 12px;
    }

    .chosen .clear {
      margin-left: auto;
      padding: 4px 10px;
    }

    .credentials {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
    }

    .credentials label {
      flex: 1 1 180px;
      display: block;
      font-size: 0.9rem;
      color: #374151;
    }

    .credentials input {
      display: block;
      width: 100%;
      margin-top: 4px;
      padding: 8px 10px;
      border: 1px solid #d1d5db;
      border-radius: 6px;
      box-sizing: border-box;
    }

    .hint {
      display: block;
      color: #6b7280;
      font-size: 0.8rem;
      margin: 4px 0 0;
      flex-basis: 100%;
    }
  `,
})
export class PersonPickerComponent {
  private readonly directory = inject(DirectoryService);

  /** Kshetra to search names within, supplied by the host. */
  readonly kshetraId = input('');
  /** Show a Clear control on the chosen card so the host can re-pick. */
  readonly clearable = input(false);

  /** Emitted when a Person is picked, so the host can clear its own error banner. */
  readonly picked = output<void>();

  nameQuery = '';
  mobileQuery = '';

  readonly candidates = signal<NameCandidate[]>([]);
  readonly mobileMatch = signal<PersonResponse | null>(null);
  readonly selectedId = signal<string | null>(null);
  selectedName = '';
  username = '';
  rawPassword = '';

  searchByName(): void {
    const name = this.nameQuery.trim();
    const kshetraId = this.kshetraId().trim();
    if (!name || !kshetraId) {
      return;
    }
    this.mobileMatch.set(null);
    this.directory.searchByName(kshetraId, name).subscribe((c) => this.candidates.set(c));
  }

  searchByMobile(): void {
    const mobile = this.mobileQuery.trim();
    if (!mobile) {
      return;
    }
    this.candidates.set([]);
    this.directory.searchByMobile(mobile).subscribe({
      next: (person) => this.mobileMatch.set(person),
      error: () => this.mobileMatch.set(null),
    });
  }

  pick(personId: string, fullName: string): void {
    this.selectedId.set(personId);
    this.selectedName = fullName;
    this.username = suggestUsername(fullName);
    this.rawPassword = suggestPassword();
    this.picked.emit();
  }

  clear(): void {
    this.selectedId.set(null);
    this.selectedName = '';
    this.username = '';
    this.rawPassword = '';
    this.candidates.set([]);
    this.mobileMatch.set(null);
    this.nameQuery = '';
    this.mobileQuery = '';
  }
}
