// PROTOTYPE — Web · Sabha definition (ADR-0012, ADR-0011)

(function () {
  const KINDS = [
    { id: 'yuvak-reg', label: 'Yuvak Sabha (Regular)' },
    { id: 'yuvak-yss', label: 'YSS Yuvak Sabha' },
    { id: 'baal-reg', label: 'Baal Sabha (Regular)' },
    { id: 'baal-bss', label: 'BSS Baal Sabha' },
    { id: 'sanyukta', label: 'Sanyukta Sabha' },
  ];
  const KSHETRAS = ['Andheri-7', 'Andheri-3', 'Andheri-9'];
  const DAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

  const PEOPLE = [
    { id: 'p1', initials: 'PP', name: 'Pratik Patel', mobile: '98201 98201', roles: ['Yuvak Nirdeshak · Andheri-7'] },
    { id: 'p2', initials: 'JS', name: 'Jignesh Solanki', mobile: '98201 32100', roles: [] },
    { id: 'p3', initials: 'VP', name: 'Vivek Pandya', mobile: '98201 55432', roles: [] },
    { id: 'p4', initials: 'AM', name: 'Ashish Mehta', mobile: '98201 47410', roles: ['Sanchalak · BSS Baal · Andheri-7'] },
    { id: 'p5', initials: 'HT', name: 'Harshad Trivedi', mobile: '98201 76543', roles: [] },
  ];

  const state = {
    kind: 'yuvak-reg',
    kshetra: 'Andheri-7',
    venue: '',
    shape: 'weekly', // 'weekly' | 'monthly'
    day: 'Sun',
    startTime: '19:00',
    endTime: '20:30',
    sanchalakQuery: '',
    sanchalakId: null,
    sahQuery: '',
    sahId: null,
    username: '',
    password: '',
  };

  function genPw() {
    const w = ['arpan', 'seva', 'sabha', 'kshetra', 'chetan'];
    return w[Math.floor(Math.random() * w.length)] + '-' + Math.floor(1000 + Math.random() * 9000);
  }
  function genUsername(name) {
    return name.toLowerCase().split(' ').join('.') + '.atk';
  }
  if (!state.password) state.password = genPw();

  function render(root) {
    function pickerHtml(idPrefix, currentId, query, label, hintNew) {
      const q = query.toLowerCase();
      const list = q ? PEOPLE.filter((p) => p.name.toLowerCase().includes(q) || p.mobile.includes(q)) : PEOPLE;
      const chosen = PEOPLE.find((p) => p.id === currentId);
      return `
        ${chosen ? `<div class="chosen-banner"><span>✓ Chosen: <strong>${chosen.name}</strong> · ${chosen.mobile}</span><a data-act="clear-${idPrefix}">Clear</a></div>` : ''}
        <div class="picker">
          <div class="picker-search">
            <input id="${idPrefix}-q" placeholder="Search ${label} by name or mobile" value="${query}" autocomplete="off" />
            <span class="count">${list.length} of ${PEOPLE.length}</span>
          </div>
          <div class="picker-list">
            ${list.map((p) => `
              <div class="picker-row ${currentId === p.id ? 'selected' : ''}" data-pid="${p.id}" data-pkr="${idPrefix}">
                <div class="avatar">${p.initials}</div>
                <div class="info">
                  <div class="name">${p.name}</div>
                  <div class="sub">${p.mobile}${p.roles.length ? ' · ' + p.roles.join(', ') : ' · not yet a User'}</div>
                </div>
                <span class="check">✓</span>
              </div>
            `).join('')}
            ${hintNew ? `<div class="picker-row create"><div class="avatar" style="background:var(--accent-soft);color:var(--accent);">+</div><div class="info"><div class="name">+ Add new Person${query ? ` matching "${query}"` : ''} to Directory</div><div class="sub">Inline create (de-dup checked).</div></div></div>` : ''}
          </div>
        </div>
      `;
    }

    function previewStripHtml() {
      const kind = KINDS.find((k) => k.id === state.kind);
      const schedule = state.shape === 'weekly'
        ? `${state.day} ${state.startTime}–${state.endTime}`
        : 'Monthly ad-hoc · Sanchalak materializes each Occurrence';
      const sanchalak = state.sanchalakId ? PEOPLE.find((p) => p.id === state.sanchalakId).name : '—';
      return `
        <div class="preview-strip">
          <strong>Preview:</strong> ${kind.label} at Kshetra <strong>${state.kshetra}</strong> · ${schedule} · Sanchalak <strong>${sanchalak}</strong>${state.venue ? ` · Venue: ${state.venue}` : ''}
        </div>
      `;
    }

    function mount() {
      root.innerHTML = `
        <div class="def-page">
          <div class="page-head" style="margin-bottom:18px;">
            <h1 style="font-size:22px;margin:0 0 4px;">Define a Sabha</h1>
            <div class="sub small muted">Creates the standing Sabha. Occurrences materialize from the schedule (weekly) or are added manually (monthly ad-hoc).</div>
          </div>

          <div class="form-card">

            <h3>Identification</h3>
            <div class="row-2">
              <div class="field">
                <label>Sabha kind</label>
                <select id="kind">
                  ${KINDS.map((k) => `<option value="${k.id}" ${k.id === state.kind ? 'selected' : ''}>${k.label}</option>`).join('')}
                </select>
                <div class="hint">Demographic + track. Extensible by MK (ADR-0009).</div>
              </div>
              <div class="field">
                <label>Kshetra</label>
                <select id="kshetra">
                  ${KSHETRAS.map((k) => `<option ${k === state.kshetra ? 'selected' : ''}>${k}</option>`).join('')}
                </select>
                <div class="hint">Locked to your Nirdeshak scope.</div>
              </div>
            </div>

            <h3>Schedule shape</h3>
            <div class="shape-toggle">
              <div class="shape-option ${state.shape === 'weekly' ? 'selected' : ''}" data-shape="weekly">
                <h4>Weekly-recurring (Regular)</h4>
                <p>Fixed slot. System auto-materializes each week's Occurrence ahead of time.</p>
              </div>
              <div class="shape-option ${state.shape === 'monthly' ? 'selected' : ''}" data-shape="monthly">
                <h4>Monthly ad-hoc (BSS / YSS)</h4>
                <p>No standing day/time. Sanchalak creates each Occurrence manually before its date.</p>
              </div>
            </div>
            ${state.shape === 'weekly' ? `
              <div class="row-3">
                <div class="field">
                  <label>Day of week</label>
                  <select id="day">${DAYS.map((d) => `<option ${d === state.day ? 'selected' : ''}>${d}</option>`).join('')}</select>
                </div>
                <div class="field"><label>Start time</label><input id="start" type="time" value="${state.startTime}" /></div>
                <div class="field"><label>End time</label><input id="end" type="time" value="${state.endTime}" /></div>
              </div>
              <div class="hint">First Occurrence will materialize for next ${state.day}, ${state.startTime}.</div>
            ` : ''}

            <h3>Standing venue</h3>
            <div class="field">
              <label>Address (free-text)</label>
              <textarea id="venue" placeholder="e.g., Sansthan Hall, near Andheri Station">${state.venue}</textarea>
              <div class="hint">Sanchalak can override venue on a per-Occurrence basis without affecting this standing address.</div>
            </div>

            <h3>Sanchalak</h3>
            ${pickerHtml('san', state.sanchalakId, state.sanchalakQuery, 'Sanchalak', true)}
            ${state.sanchalakId ? `
              <div class="creds-hint" style="margin-top:14px;">Initial credentials — handed to the new Sanchalak (ADR-0011). They must change the password on first login.</div>
              <div class="row-2">
                <div class="field"><label>Username</label><input id="un" value="${state.username || genUsername(PEOPLE.find(p => p.id === state.sanchalakId).name)}" /></div>
                <div class="field"><label>Initial password</label><input id="pw" value="${state.password}" /></div>
              </div>
            ` : ''}

            <h3>Sah-Sanchalak (optional)</h3>
            ${pickerHtml('sah', state.sahId, state.sahQuery, 'Sah-Sanchalak', false)}

            ${previewStripHtml()}

            <div class="actions">
              <button class="btn" style="flex:1;" data-act="cancel">Cancel</button>
              <button class="btn primary" style="flex:2;" data-act="save" ${state.sanchalakId && state.venue.trim() ? '' : 'disabled'}>Create Sabha</button>
            </div>
          </div>
        </div>
      `;
      attach();
    }

    function attach() {
      const sel = (id) => root.querySelector('#' + id);
      sel('kind')?.addEventListener('change', (e) => { state.kind = e.target.value; mount(); });
      sel('kshetra')?.addEventListener('change', (e) => { state.kshetra = e.target.value; mount(); });
      sel('venue')?.addEventListener('input', (e) => {
        state.venue = e.target.value;
        const btn = root.querySelector('[data-act="save"]');
        if (btn) btn.disabled = !(state.sanchalakId && state.venue.trim());
      });
      sel('day')?.addEventListener('change', (e) => { state.day = e.target.value; mount(); });
      sel('start')?.addEventListener('input', (e) => { state.startTime = e.target.value; });
      sel('end')?.addEventListener('input', (e) => { state.endTime = e.target.value; });
      sel('un')?.addEventListener('input', (e) => { state.username = e.target.value; });
      sel('pw')?.addEventListener('input', (e) => { state.password = e.target.value; });

      ['san', 'sah'].forEach((p) => {
        const qIn = sel(`${p}-q`);
        if (qIn) qIn.addEventListener('input', (e) => {
          if (p === 'san') state.sanchalakQuery = e.target.value;
          else state.sahQuery = e.target.value;
          mount();
          root.querySelector(`#${p}-q`).focus();
        });
      });

      root.querySelectorAll('.shape-option').forEach((opt) => {
        opt.addEventListener('click', () => {
          state.shape = opt.dataset.shape;
          mount();
        });
      });

      root.querySelectorAll('[data-pid]').forEach((row) => {
        row.addEventListener('click', () => {
          const which = row.dataset.pkr;
          const pid = row.dataset.pid;
          if (which === 'san') {
            state.sanchalakId = pid;
            const p = PEOPLE.find((x) => x.id === pid);
            state.username = genUsername(p.name);
          }
          else if (which === 'sah') state.sahId = pid;
          mount();
        });
      });

      root.querySelectorAll('[data-act]').forEach((b) => {
        b.addEventListener('click', () => {
          const a = b.dataset.act;
          if (a === 'clear-san') { state.sanchalakId = null; state.username = ''; mount(); }
          else if (a === 'clear-sah') { state.sahId = null; mount(); }
          else if (a === 'cancel') alert('Cancelled.');
          else if (a === 'save') {
            const kind = KINDS.find((k) => k.id === state.kind);
            alert(`Created: ${kind.label} at ${state.kshetra}.\nFirst Occurrence will auto-materialize for the next ${state.day} at ${state.startTime}.`);
          }
        });
      });
    }
    mount();
  }

  window.__VARIANTS = [{ key: 'main', name: 'Define a Sabha', render }];
})();
