// PROTOTYPE — Web · Occurrence reopen (ADR-0001)
// Authority: Nirikshak / Nirdeshak / Sah-Nirdeshak. Captures reason; carries "reopened" badge.

(function () {
  const occs = [
    { id: 'o1', date: 'Sun, 24 May 2026', time: '7:00–8:30pm', sabha: 'Yuvak Sabha · Andheri-7', state: 'Open', present: 14, absent: 2, walkIn: 1, finalizedAt: null, finalizedBy: null, reopenedReason: null, audit: [
      { when: '24 May 7:00pm', who: 'Pratik Patel', what: 'Opened for Marking' },
    ]},
    { id: 'o2', date: 'Sun, 17 May 2026', time: '7:00–8:30pm', sabha: 'Yuvak Sabha · Andheri-7', state: 'Finalized', present: 12, absent: 6, walkIn: 0, finalizedAt: '17 May 8:42pm', finalizedBy: 'Pratik Patel (Sanchalak)', reopenedReason: null, audit: [
      { when: '17 May 7:02pm', who: 'Pratik Patel', what: 'Opened for Marking' },
      { when: '17 May 8:42pm', who: 'Pratik Patel', what: 'Finalized (12 P / 6 A / 0 W)' },
    ]},
    { id: 'o3', date: 'Sun, 10 May 2026', time: '7:00–8:30pm', sabha: 'Yuvak Sabha · Andheri-7', state: 'Reopened', present: 15, absent: 3, walkIn: 2, finalizedAt: '10 May 8:38pm', finalizedBy: 'Pratik Patel (Sanchalak)', reopenedReason: 'Two Walk-ins recorded on paper but not in app', audit: [
      { when: '10 May 7:00pm', who: 'Pratik Patel', what: 'Opened for Marking' },
      { when: '10 May 8:38pm', who: 'Pratik Patel', what: 'Finalized (13 P / 5 A / 0 W)' },
      { when: '11 May 9:15am', who: 'Bhavin Mehta (Nirikshak)', what: 'Reopened — "Two Walk-ins recorded on paper but not in app"' },
      { when: '11 May 9:22am', who: 'Bhavin Mehta (Nirikshak, proxy for Sanchalak)', what: 'Added Walk-ins · Mihir Gajjar, Aakash Sheth' },
      { when: '11 May 9:24am', who: 'Bhavin Mehta (Nirikshak)', what: 'Refinalized (15 P / 3 A / 2 W)' },
    ]},
    { id: 'o4', date: 'Sun, 3 May 2026', time: '7:00–8:30pm', sabha: 'Yuvak Sabha · Andheri-7', state: 'Finalized', present: 13, absent: 5, walkIn: 0, finalizedAt: '3 May 8:36pm', finalizedBy: 'Pratik Patel (Sanchalak)', reopenedReason: null, audit: [
      { when: '3 May 7:00pm', who: 'Pratik Patel', what: 'Opened for Marking' },
      { when: '3 May 8:36pm', who: 'Pratik Patel', what: 'Finalized (13 P / 5 A / 0 W)' },
    ]},
    { id: 'o5', date: 'Sun, 26 Apr 2026', time: '7:00–8:30pm', sabha: 'Yuvak Sabha · Andheri-7', state: 'Cancelled', present: 0, absent: 0, walkIn: 0, finalizedAt: null, finalizedBy: null, reopenedReason: null, audit: [
      { when: '24 Apr 6:12pm', who: 'Pratik Patel', what: 'Cancelled — "Power outage at venue"' },
    ]},
  ];

  let selectedId = 'o2';
  let reason = '';

  function render(root) {
    function listHtml() {
      return occs
        .map(
          (o) => `
        <div class="occ-row ${o.id === selectedId ? 'selected' : ''}" data-id="${o.id}">
          <div class="col">
            <div class="date">${o.date}</div>
            <div class="sabha">${o.sabha}</div>
          </div>
          <span class="state ${o.state.toLowerCase()}">${o.state}</span>
        </div>
      `,
        )
        .join('');
    }

    function detailHtml() {
      const o = occs.find((x) => x.id === selectedId);
      if (!o) return '';

      const reopenSection = o.state === 'Finalized' ? `
        <div class="reopen-card">
          <h3>Reopen this Occurrence</h3>
          <p>Brings the Occurrence back to <strong>Open for Marking</strong> so attendance can be edited. A "reopened" badge will persist on the Occurrence; this action and the reason are audit-logged.</p>
        </div>
        <div class="reopen-form">
          <span class="auth-pill">Nirikshak authority · ADR-0001</span>
          <div class="field">
            <label>Reason for reopening (required)</label>
            <textarea id="rr" placeholder="e.g., Sanchalak missed marking three Walk-ins on paper">${reason}</textarea>
          </div>
          <div style="display:flex;gap:8px;">
            <button class="btn" data-act="cancel">Cancel</button>
            <button class="btn primary" data-act="reopen" ${reason.trim() ? '' : 'disabled'}>Reopen Occurrence</button>
          </div>
        </div>
      ` : o.state === 'Reopened' ? `
        <div class="reopened-banner">
          <span class="pill" style="padding:2px 8px;border-radius:999px;font-size:10px;font-weight:600;text-transform:uppercase;">Reopened</span>
          <span><strong>Why:</strong> ${o.reopenedReason}</span>
        </div>
      ` : o.state === 'Open' ? `
        <div class="locked-banner">This Occurrence is still Open for Marking. No reopen needed.</div>
      ` : `
        <div class="locked-banner" style="background:var(--bad-soft);color:var(--bad);">Cancelled Occurrences cannot be reopened — only reverted (by the Sanchalak).</div>
      `;

      return `
        <div class="crumb">Occurrences › ${o.sabha} ›</div>
        <h2>${o.date}</h2>
        <div class="state-row">
          <span class="state ${o.state.toLowerCase()}" style="font-size:10px;padding:2px 10px;border-radius:999px;text-transform:uppercase;letter-spacing:0.4px;font-weight:600;background:var(--line-soft);color:var(--ink-dim);">${o.state}</span>
          <span class="small muted">${o.time}</span>
        </div>

        ${o.state === 'Finalized' || o.state === 'Reopened' ? `
          <div class="attendance-grid">
            <div class="stat"><div class="label">Present</div><div class="value">${o.present}</div></div>
            <div class="stat"><div class="label">Absent</div><div class="value">${o.absent}</div></div>
            <div class="stat"><div class="label">Walk-ins</div><div class="value">${o.walkIn}</div></div>
            <div class="stat"><div class="label">Rate</div><div class="value">${Math.round((o.present / (o.present + o.absent)) * 100)}%</div></div>
          </div>
        ` : ''}

        ${reopenSection}

        <div class="audit-trail">
          <h3>Audit trail</h3>
          ${o.audit
            .map(
              (a) => `<div class="audit-entry"><span class="when">${a.when}</span><span class="who">${a.who}</span><span class="what">${a.what}</span></div>`,
            )
            .join('')}
        </div>
      `;
    }

    function mount() {
      root.innerHTML = `
        <div class="page-head" style="margin-bottom:20px;">
          <h1 style="font-size:22px;margin:0 0 4px;">Occurrences in scope</h1>
          <div class="sub small muted">Sabhas assigned to you. Reopen Finalized Occurrences when the Sanchalak needs an edit.</div>
        </div>
        <div class="reopen-grid">
          <div class="occ-list-card">
            <div class="head">Last 8 weeks</div>
            ${listHtml()}
          </div>
          <section class="occ-detail">${detailHtml()}</section>
        </div>
      `;
      attach();
    }

    function attach() {
      root.querySelectorAll('.occ-row').forEach((row) => {
        row.addEventListener('click', () => {
          selectedId = row.dataset.id;
          reason = '';
          mount();
        });
      });
      const rr = root.querySelector('#rr');
      if (rr) rr.addEventListener('input', (e) => {
        reason = e.target.value;
        const btn = root.querySelector('[data-act="reopen"]');
        if (btn) btn.disabled = !e.target.value.trim();
      });
      root.querySelectorAll('[data-act]').forEach((b) => {
        b.addEventListener('click', () => {
          if (b.dataset.act === 'reopen') {
            const o = occs.find((x) => x.id === selectedId);
            o.state = 'Reopened';
            o.reopenedReason = reason;
            const now = '11 May 9:15am'; // canned for prototype clarity
            o.audit.push({ when: now, who: 'Bhavin Mehta (Nirikshak)', what: `Reopened — "${reason}"` });
            reason = '';
            mount();
          } else if (b.dataset.act === 'cancel') {
            reason = '';
            mount();
          }
        });
      });
    }
    mount();
  }

  window.__VARIANTS = [{ key: 'main', name: 'Occurrence reopen', render }];
})();
