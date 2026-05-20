// PROTOTYPE — Mobile · Manage current Occurrence (ADR-0001)
// Sanchalak only. Cancel (reversible), Reschedule, Venue override.

(function () {
  const occ = {
    sabha: 'Yuvak Sabha · Kshetra Andheri-7',
    standingVenue: 'Sansthan Hall, near Andheri Station',
    scheduledDate: '2026-05-24',
    scheduledTime: '19:00',
    endTime: '20:30',
    state: 'Scheduled', // 'Scheduled' | 'Rescheduled' | 'Open for Marking' | 'Cancelled'
    venueOverride: null,
    rescheduled: false,
    audit: [],
  };

  let openPanel = null; // 'reschedule' | 'venue' | 'cancel'
  let form = {
    newDate: occ.scheduledDate,
    newTime: occ.scheduledTime,
    venue: '',
    cancelReason: '',
  };

  function render(root) {
    function statePill() {
      const cls =
        occ.state === 'Cancelled' ? 'cancelled'
        : occ.state === 'Open for Marking' ? 'open'
        : occ.state === 'Rescheduled' ? 'rescheduled'
        : 'scheduled';
      return `<span class="state-pill ${cls}">${occ.state}</span>`;
    }

    function whenLabel() {
      const d = new Date(occ.scheduledDate + 'T00:00:00');
      const opts = { weekday: 'short', month: 'short', day: 'numeric' };
      return `${d.toLocaleDateString('en-IN', opts)} · ${occ.scheduledTime}–${occ.endTime}`;
    }

    function cardHtml() {
      return `
        <div class="state-card">
          ${statePill()}
          ${occ.rescheduled ? '<span class="badge">moved from 19:00</span>' : ''}
          ${occ.venueOverride ? '<span class="badge">venue overridden</span>' : ''}
          <div class="where">${occ.venueOverride || occ.standingVenue}</div>
          <div class="when">${whenLabel()}</div>
        </div>
      `;
    }

    function actionsHtml() {
      if (occ.state === 'Cancelled') {
        return `
          <div class="revert-card">
            <h3>This Occurrence is Cancelled</h3>
            <p>You can revert it back. Audit log preserves the cancellation regardless.</p>
            <div style="display:flex;gap:8px;">
              <button class="btn" style="flex:1;" data-act="revert-to-scheduled">↶ Back to Scheduled</button>
              <button class="btn primary" style="flex:1;" data-act="revert-to-open">↶ Open for Marking</button>
            </div>
          </div>
        `;
      }
      return `
        <div class="action-card ${openPanel === 'reschedule' ? 'open' : ''}" data-panel="reschedule">
          <div class="head">
            <div class="icon">⏱</div>
            <div class="body">
              <div class="title">Reschedule</div>
              <div class="sub">Move this Occurrence to a different date or time. Sabha's standing slot is unchanged.</div>
            </div>
            <div class="chev">›</div>
          </div>
          <div class="panel">
            <div class="row-2">
              <div class="field"><label>New date</label><input id="rd" type="date" value="${form.newDate}" /></div>
              <div class="field"><label>Start time</label><input id="rt" type="time" value="${form.newTime}" /></div>
            </div>
            <div class="hint" style="margin-bottom:10px;">Heads-up: Reschedule notifies the Roster.</div>
            <button class="btn primary" style="width:100%;" data-act="reschedule">Save reschedule</button>
          </div>
        </div>

        <div class="action-card ${openPanel === 'venue' ? 'open' : ''}" data-panel="venue">
          <div class="head">
            <div class="icon">⛺</div>
            <div class="body">
              <div class="title">Override venue (this Occurrence only)</div>
              <div class="sub">For one-off relocations (e.g., hall booked elsewhere). Doesn't change the Sabha's standing venue.</div>
            </div>
            <div class="chev">›</div>
          </div>
          <div class="panel">
            <div class="field">
              <label>One-off venue</label>
              <textarea id="vo" placeholder="e.g., Community Hall, Lokhandwala">${form.venue}</textarea>
              <div class="hint">Standing venue: <strong>${occ.standingVenue}</strong></div>
            </div>
            <div style="display:flex;gap:8px;">
              ${occ.venueOverride ? '<button class="btn" data-act="clear-venue" style="flex:1;">Remove override</button>' : ''}
              <button class="btn primary" style="flex:1;" data-act="save-venue" ${form.venue.trim() ? '' : 'disabled'}>Save venue</button>
            </div>
          </div>
        </div>

        <div class="action-card danger ${openPanel === 'cancel' ? 'open' : ''}" data-panel="cancel">
          <div class="head">
            <div class="icon">✕</div>
            <div class="body">
              <div class="title">Cancel this Occurrence</div>
              <div class="sub">No attendance can be recorded while Cancelled. Reversible — audit log preserves the action.</div>
            </div>
            <div class="chev">›</div>
          </div>
          <div class="panel">
            <div class="field">
              <label>Reason (required)</label>
              <textarea id="cr" placeholder="e.g., Sant arrival rescheduled, venue unavailable">${form.cancelReason}</textarea>
              <div class="hint">Saved in audit and shown to the Nirikshak.</div>
            </div>
            <button class="btn danger" style="width:100%;" data-act="cancel-occ" ${form.cancelReason.trim() ? '' : 'disabled'}>Cancel Occurrence</button>
          </div>
        </div>
      `;
    }

    function auditHtml() {
      if (!occ.audit.length) return '';
      return `
        <div class="section-heading" style="margin:20px 0 4px;font-size:11px;color:var(--ink-faint);text-transform:uppercase;letter-spacing:0.4px;">Audit log</div>
        ${occ.audit
          .map(
            (a) => `<div class="audit-strip">· ${a.at} — ${a.who}: ${a.what}</div>`,
          )
          .join('')}
      `;
    }

    function mount() {
      root.innerHTML = `
        <div class="oc-page">
          <div class="appbar">
            <h1>← Manage Occurrence</h1>
            <div class="sub">${occ.sabha}</div>
          </div>
          <div class="oc-body">
            ${cardHtml()}
            <div class="scope-note">Changes affect <strong>this Occurrence only</strong>. The Sabha's standing schedule and venue remain unchanged. <em>Sanchalak only.</em></div>
            ${actionsHtml()}
            ${auditHtml()}
          </div>
        </div>
      `;
      attach();
    }

    function attach() {
      root.querySelectorAll('.action-card .head').forEach((h) => {
        h.addEventListener('click', () => {
          const card = h.closest('.action-card');
          const p = card.dataset.panel;
          openPanel = openPanel === p ? null : p;
          mount();
        });
      });
      const rd = root.querySelector('#rd');
      if (rd) rd.addEventListener('input', (e) => (form.newDate = e.target.value));
      const rt = root.querySelector('#rt');
      if (rt) rt.addEventListener('input', (e) => (form.newTime = e.target.value));
      const vo = root.querySelector('#vo');
      if (vo) vo.addEventListener('input', (e) => {
        form.venue = e.target.value;
        const btn = root.querySelector('[data-act="save-venue"]');
        if (btn) btn.disabled = !e.target.value.trim();
      });
      const cr = root.querySelector('#cr');
      if (cr) cr.addEventListener('input', (e) => {
        form.cancelReason = e.target.value;
        const btn = root.querySelector('[data-act="cancel-occ"]');
        if (btn) btn.disabled = !e.target.value.trim();
      });

      root.querySelectorAll('[data-act]').forEach((b) => {
        b.addEventListener('click', () => {
          const a = b.dataset.act;
          const now = new Date().toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' });
          if (a === 'reschedule') {
            occ.scheduledDate = form.newDate;
            occ.scheduledTime = form.newTime;
            occ.state = 'Rescheduled';
            occ.rescheduled = true;
            occ.audit.unshift({ at: `today ${now}`, who: 'Pratik Patel (Sanchalak)', what: `Rescheduled to ${form.newDate} ${form.newTime}` });
            openPanel = null;
            mount();
          } else if (a === 'save-venue') {
            occ.venueOverride = form.venue;
            occ.audit.unshift({ at: `today ${now}`, who: 'Pratik Patel (Sanchalak)', what: `Venue override set: ${form.venue}` });
            openPanel = null;
            mount();
          } else if (a === 'clear-venue') {
            occ.audit.unshift({ at: `today ${now}`, who: 'Pratik Patel (Sanchalak)', what: 'Venue override cleared' });
            occ.venueOverride = null;
            form.venue = '';
            mount();
          } else if (a === 'cancel-occ') {
            occ.state = 'Cancelled';
            occ.audit.unshift({ at: `today ${now}`, who: 'Pratik Patel (Sanchalak)', what: `Cancelled — "${form.cancelReason}"` });
            openPanel = null;
            mount();
          } else if (a === 'revert-to-scheduled' || a === 'revert-to-open') {
            const next = a === 'revert-to-open' ? 'Open for Marking' : 'Scheduled';
            occ.audit.unshift({ at: `today ${now}`, who: 'Pratik Patel (Sanchalak)', what: `Reverted Cancellation → ${next}` });
            occ.state = next;
            mount();
          }
        });
      });
    }
    mount();
  }

  window.__VARIANTS = [{ key: 'main', name: 'Manage Occurrence', render }];
})();
