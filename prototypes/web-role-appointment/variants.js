// PROTOTYPE — Web · Role appointment + revocation (revised authority chain, supersedes ADR-0011)
// Authority is BY SCOPE: any current holder of an appointing scope can appoint AND revoke peers/sub-roles,
// including assignments they did not create. Delete = REVOKE the role assignment (Person persists).
//
//   MK            → first Regional Team member per (City, demographic)
//   Regional Team → peer RT members (self-replicating, last-one-out guard) + Sanyojak (per Zone, demographic)
//   Sanyojak      → Nirdeshak (per Kshetra, demographic)
//   Nirdeshak     → Sanchalak, Sah-Sanchalak, Nirikshak, Sah-Nirdeshak (max 2 per Kshetra, demographic)
//   Sah-Nirdeshak → appoints nothing (read-only + operational proxy)

(function () {
  const ACTORS = {
    mk: {
      name: 'Madhyastha Karyalaya', chip: 'Madhyastha Karyalaya · Gujarat', user: 'Dipan Shah (MK)',
      roles: ['rt'],
      blurb: 'MK seeds the <strong>first</strong> Regional Team member per (City, demographic). Once seeded, the Regional Team replicates itself.',
    },
    'regional-team': {
      name: 'Regional Team', chip: 'Regional Team · Mumbai · Yuvak', user: 'Hardik Trivedi (RT)',
      roles: ['rt-peer', 'sanyojak'],
      blurb: 'Self-replicating: any member may appoint &amp; revoke <strong>peer</strong> RT members in the same (City, demographic) — the system only blocks revoking the <strong>last</strong> one. Also appoints the Sanyojak per (Zone, demographic).',
    },
    sanyojak: {
      name: 'Sanyojak', chip: 'Yuvak Sanyojak · Mumbai West', user: 'Manan Joshi (Sanyojak)',
      roles: ['nirdeshak'],
      blurb: 'Appoints &amp; revokes the Nirdeshak for each (Kshetra, demographic) in the Zone.',
    },
    nirdeshak: {
      name: 'Nirdeshak', chip: 'Yuvak Nirdeshak · Andheri-7', user: 'Pratik Patel (Nirdeshak)',
      roles: ['sanchalak', 'sah-sanchalak', 'nirikshak', 'sah-nirdeshak'],
      blurb: 'Appoints the operational roles in scope. <strong>Sah-Nirdeshak capped at 2</strong> per (Kshetra, demographic).',
    },
    'sah-nirdeshak': {
      name: 'Sah-Nirdeshak', chip: 'Yuvak Sah-Nirdeshak · Andheri-7', user: 'Rohan Desai (Sah-Nirdeshak)',
      roles: [],
      blurb: 'Read-only + operational proxy (reopen Occurrences, act during Nirdeshak absence) and the Nirdeshak’s analytics view. <strong>No appointment, creation, or deletion authority</strong> ("for now").',
    },
  };

  const ROLE_DEFS = {
    rt:            { name: 'Regional Team member', scope: 'Mumbai · Yuvak', cap: null, lastOneOut: false, note: 'First member is an MK admin act; thereafter the team self-replicates.' },
    'rt-peer':     { name: 'Regional Team member', scope: 'Mumbai · Yuvak', cap: null, lastOneOut: true,  note: 'Peer appointment — full create + revoke; cannot revoke the last remaining member.' },
    sanyojak:      { name: 'Sanyojak',             scope: 'Mumbai West · Yuvak', cap: null, lastOneOut: false, note: 'One per (Zone, demographic).' },
    nirdeshak:     { name: 'Nirdeshak',            scope: 'Andheri-7 · Yuvak', cap: null, lastOneOut: false, note: 'One per (Kshetra, demographic).' },
    sanchalak:     { name: 'Sanchalak',            scope: 'Andheri-7 · Yuvak Sabhas', cap: null, lastOneOut: false, note: 'Runs day-to-day attendance.' },
    'sah-sanchalak':{ name: 'Sah-Sanchalak',       scope: 'Andheri-7 · Yuvak', cap: null, lastOneOut: false, note: 'Deputy to Sanchalak.' },
    nirikshak:     { name: 'Nirikshak',            scope: 'Andheri-7 · 3–4 Sabhas', cap: null, lastOneOut: false, note: 'Oversees a mutable set of 3–4 Sabhas.' },
    'sah-nirdeshak':{ name: 'Sah-Nirdeshak',       scope: 'Andheri-7 · Yuvak', cap: 2,    lastOneOut: false, note: 'Max 2 per (Kshetra, demographic); read-only + proxy.' },
  };

  // Current role-holders in scope (in-memory). Some pre-seeded.
  let holders = [
    { id: 'h1', roleKey: 'rt-peer', name: 'Hardik Trivedi', initials: 'HT', since: 'Jan 2026' },
    { id: 'h2', roleKey: 'rt-peer', name: 'Smita Rana', initials: 'SR', since: 'Feb 2026' },
    { id: 'h3', roleKey: 'rt', name: 'Hardik Trivedi', initials: 'HT', since: 'Jan 2026' },
    { id: 'h4', roleKey: 'sanyojak', name: 'Manan Joshi', initials: 'MJ', since: 'Feb 2026' },
    { id: 'h5', roleKey: 'nirdeshak', name: 'Pratik Patel', initials: 'PP', since: 'Mar 2026' },
    { id: 'h6', roleKey: 'sanchalak', name: 'Ashish Mehta', initials: 'AM', since: 'Mar 2026' },
    { id: 'h7', roleKey: 'nirikshak', name: 'Nikhil Joshi', initials: 'NJ', since: 'Apr 2026' },
    { id: 'h8', roleKey: 'sah-nirdeshak', name: 'Rohan Desai', initials: 'RD', since: 'Apr 2026' },
  ];

  const PEOPLE = [
    { id: 'p20', initials: 'PP', name: 'Pratik Patel', mobile: '98201 98201', roles: ['Yuvak Nirdeshak · Andheri-7'] },
    { id: 'p21', initials: 'JS', name: 'Jignesh Solanki', mobile: '98201 32100', roles: [] },
    { id: 'p22', initials: 'AM', name: 'Ashish Mehta', mobile: '98201 47410', roles: ['Sanchalak · Yuvak · Andheri-7'] },
    { id: 'p23', initials: 'VP', name: 'Vivek Pandya', mobile: '98201 55432', roles: [] },
    { id: 'p24', initials: 'NJ', name: 'Nikhil Joshi', mobile: '98201 60601', roles: ['Nirikshak · Yuvak · Andheri-7'] },
  ];

  function genUsername(name) { return name.toLowerCase().split(' ').join('.') + '.atk'; }
  function genPassword() {
    const a = 'kshetra,sabha,seva,arpit,nirmal,sumati,chetan'.split(',');
    return a[Math.floor(Math.random() * a.length)] + '-' + Math.floor(1000 + Math.random() * 9000);
  }

  function render(root) {
    const state = {
      actor: 'nirdeshak',
      role: 'sanchalak',
      query: '',
      person: null,
      newPerson: { name: '', mobile: '', dob: '' },
      username: '',
      password: genPassword(),
      log: [],
      reject: null, // { holderId, code, detail } — last-one-out 409 surfaced on the offending row
    };

    function logAct(msg) { state.log.unshift({ t: new Date().toLocaleTimeString(), msg }); }
    function holdersOf(roleKey) { return holders.filter((h) => h.roleKey === roleKey); }

    function setChip() {
      const host = document.querySelector('.topnav .role-chip');
      if (host) host.textContent = ACTORS[state.actor].chip;
      const user = document.querySelector('.topnav .user');
      if (user) user.textContent = ACTORS[state.actor].user;
    }

    function actorSwitcher() {
      return `
        <div class="actor-banner">
          <div><strong>${ACTORS[state.actor].name} authority</strong> — ${ACTORS[state.actor].blurb}</div>
          <div class="actor-switcher">
            ${Object.keys(ACTORS).map((a) => `
              <button class="${state.actor === a ? 'active' : ''}" data-actor="${a}">${ACTORS[a].name}</button>
            `).join('')}
          </div>
        </div>
      `;
    }

    // Can this holder be revoked? Returns {ok, reason}.
    function revocability(h) {
      const def = ROLE_DEFS[h.roleKey];
      if (def.lastOneOut && holdersOf(h.roleKey).length <= 1) {
        return { ok: false, reason: 'last member of (Mumbai, Yuvak)' };
      }
      return { ok: true };
    }

    function holdersCard(roleKey) {
      const def = ROLE_DEFS[roleKey];
      const list = holdersOf(roleKey);
      const capLabel = def.cap ? `<span class="cap ${list.length >= def.cap ? 'full' : ''}">${list.length}/${def.cap}</span>` : `<span class="cap">${list.length}</span>`;
      return `
        <div class="holders-card">
          <div class="holders-head">
            <div>
              <span class="role-title">${def.name}</span> ${capLabel}
              <div class="scope">${def.scope} · ${def.note}</div>
            </div>
          </div>
          ${list.length ? list.map((h) => {
            const rejected = state.reject && state.reject.holderId === h.id;
            return `
            <div class="holder-row">
              <div class="avatar" style="width:30px;height:30px;font-size:11px;">${h.initials}</div>
              <div class="info">
                <div class="name">${h.name}</div>
                <div class="sub">holding since ${h.since}</div>
              </div>
              <button class="btn danger xs" data-revoke="${h.id}">Revoke role</button>
            </div>
            ${rejected ? `
            <div class="revoke-error">
              <div class="revoke-error-head">⚠ 409 Conflict · <code>${state.reject.code}</code></div>
              <div class="revoke-error-body">${state.reject.detail}</div>
            </div>` : ''}`;
          }).join('') : '<div class="empty-holders">No current holders in scope.</div>'}
        </div>
      `;
    }

    function chosenLabel() {
      if (!state.person) return null;
      if (state.person === 'new') return state.newPerson.name || '(new Person — name pending)';
      const p = PEOPLE.find((pp) => pp.id === state.person);
      return p ? `${p.name} · ${p.mobile}` : null;
    }

    function appointPanel() {
      const actorRoles = ACTORS[state.actor].roles;
      if (!actorRoles.length) {
        return `<div class="no-appoint">This role appoints nothing. ${ACTORS[state.actor].blurb}</div>`;
      }
      if (!actorRoles.includes(state.role)) state.role = actorRoles[0];

      const def = ROLE_DEFS[state.role];
      const count = holdersOf(state.role).length;
      const capReached = def.cap && count >= def.cap;

      const q = state.query.toLowerCase();
      const list = q ? PEOPLE.filter((p) => p.name.toLowerCase().includes(q) || p.mobile.includes(q)) : PEOPLE;
      const chosen = chosenLabel();

      return `
        <div class="form-card">
          <h3>Appoint a role in your scope</h3>
          <div class="row">
            <div class="field">
              <label>Role</label>
              <select id="role">
                ${actorRoles.map((rk) => `<option value="${rk}" ${rk === state.role ? 'selected' : ''}>${ROLE_DEFS[rk].name}</option>`).join('')}
              </select>
            </div>
            <div class="field">
              <label>Scope (locked to your authority)</label>
              <input value="${def.scope}" disabled />
            </div>
          </div>
          ${capReached ? `<div class="cap-warning">⚠ Cap reached — ${def.cap} ${def.name}s already in (Kshetra, demographic). Revoke one before appointing another.</div>` : ''}

          <h3>Person — pick from Directory or create new</h3>
          ${chosen ? `<div class="chosen-banner"><span>✓ Chosen: <strong>${chosen}</strong></span><a data-act="clear">Clear</a></div>` : ''}
          <div class="picker">
            <div class="picker-search">
              <input id="q" placeholder="Search by name or mobile" value="${state.query}" autocomplete="off" />
              <span class="count">${list.length} of ${PEOPLE.length}</span>
            </div>
            <div class="picker-list">
              ${list.map((p) => `
                <div class="picker-row ${state.person === p.id ? 'selected' : ''}" data-pid="${p.id}">
                  <div class="avatar">${p.initials}</div>
                  <div class="info">
                    <div class="name">${p.name}</div>
                    <div class="sub">${p.mobile}</div>
                    ${p.roles.length
                      ? `<div class="roles-row">${p.roles.map((r) => `<span class="role-chip-sm">${r}</span>`).join('')}</div>`
                      : `<div class="roles-row"><span class="muted xs">No current roles · not yet a User</span></div>`}
                  </div>
                  <span class="check">✓</span>
                </div>
              `).join('') || '<div style="padding:20px;text-align:center;" class="muted small">No People match.</div>'}
              <div class="picker-row create" data-pid="new">
                <div class="avatar" style="background:var(--accent-soft);color:var(--accent);">+</div>
                <div class="info"><div class="name">Create new Person${state.query ? ` matching "${state.query}"` : ''}</div><div class="sub">Adds to Directory and continues with appointment.</div></div>
              </div>
            </div>
          </div>
          ${state.person === 'new' ? `
            <div class="inline-create">
              <h4>New Person details</h4>
              <div class="row">
                <div class="field"><label>Full name</label><input id="np-name" value="${state.newPerson.name}" /></div>
                <div class="field"><label>Mobile (de-dup check)</label><input id="np-mob" value="${state.newPerson.mobile}" /></div>
              </div>
              <div class="field"><label>Date of birth (optional)</label><input id="np-dob" type="date" value="${state.newPerson.dob}" /></div>
            </div>
          ` : ''}

          <h3>Initial credentials</h3>
          <div class="credentials-suggest">Handed to the new User. They'll be required to change the password on first login (ADR-0004).</div>
          <div class="row">
            <div class="field"><label>Username</label><input id="un" placeholder="auto-suggested" value="${state.username}" /></div>
            <div class="field"><label>Password</label><input id="pw" value="${state.password}" /></div>
          </div>

          <div class="actions">
            <button class="btn" data-act="cancel" style="flex:1;">Cancel</button>
            <button class="btn primary" data-act="confirm" style="flex:2;" ${state.person && !capReached ? '' : 'disabled'}>Appoint ${def.name}</button>
          </div>
        </div>
      `;
    }

    function logPanel() {
      if (!state.log.length) return '';
      return `
        <div class="action-log">
          <h4>Authority log — appoint / revoke by scope</h4>
          ${state.log.map((l) => `<div class="log-row"><span class="t">${l.t}</span> ${l.msg}</div>`).join('')}
        </div>
      `;
    }

    function mount() {
      setChip();
      const actorRoles = ACTORS[state.actor].roles;
      root.innerHTML = `
        <div class="vd">
          <h2 class="section">Role appointment &amp; revocation</h2>
          <p class="lead">Authority is by scope, not by creator — you may revoke assignments you didn't create. Deleting a role-holder revokes the assignment; the Person persists (loses login only if it was their last role).</p>
          ${actorSwitcher()}
          <h3 class="holders-section">Current holders in your scope</h3>
          ${actorRoles.length ? actorRoles.map((rk) => holdersCard(rk)).join('') : holdersCard('sah-nirdeshak')}
          ${appointPanel()}
          ${logPanel()}
        </div>
      `;
      attach();
    }

    function attach() {
      const sel = (id) => root.querySelector('#' + id);

      root.querySelectorAll('.actor-switcher button').forEach((b) => {
        b.addEventListener('click', () => {
          state.actor = b.dataset.actor;
          state.person = null;
          state.username = '';
          state.reject = null;
          mount();
        });
      });

      sel('role')?.addEventListener('change', (e) => { state.role = e.target.value; state.person = null; state.username = ''; state.reject = null; mount(); });

      const q = sel('q');
      if (q) q.addEventListener('input', (e) => { state.query = e.target.value; mount(); root.querySelector('#q')?.focus(); });

      root.querySelectorAll('[data-pid]').forEach((el) => {
        el.addEventListener('click', () => {
          state.person = el.dataset.pid;
          const ch = state.person === 'new' ? null : PEOPLE.find((p) => p.id === state.person);
          if (ch) state.username = genUsername(ch.name);
          else if (state.person === 'new' && state.newPerson.name) state.username = genUsername(state.newPerson.name);
          mount();
        });
      });

      const npn = sel('np-name');
      if (npn) npn.addEventListener('input', (e) => {
        state.newPerson.name = e.target.value;
        if (state.newPerson.name) state.username = genUsername(state.newPerson.name);
        const unEl = sel('un'); if (unEl) unEl.value = state.username;
        const banner = root.querySelector('.chosen-banner strong'); if (banner) banner.textContent = e.target.value || '(new Person — name pending)';
      });
      sel('np-mob')?.addEventListener('input', (e) => (state.newPerson.mobile = e.target.value));
      sel('np-dob')?.addEventListener('input', (e) => (state.newPerson.dob = e.target.value));
      sel('un')?.addEventListener('input', (e) => (state.username = e.target.value));
      sel('pw')?.addEventListener('input', (e) => (state.password = e.target.value));

      // Revoke = scope-based delete of a role assignment. The backend authorizes by
      // scope, then enforces the Regional Team last-one-out guard — rejecting with a
      // 409 ProblemDetail (code LAST_REGIONAL_TEAM_MEMBER) that we surface as a banner.
      root.querySelectorAll('[data-revoke]').forEach((b) => {
        b.addEventListener('click', () => {
          const h = holders.find((x) => x.id === b.dataset.revoke);
          const r = revocability(h);
          if (!r.ok) {
            state.reject = {
              holderId: h.id,
              code: 'LAST_REGIONAL_TEAM_MEMBER',
              detail: `Cannot revoke the ${r.reason} — the Regional Team tier for this (City, demographic) can never be emptied. Appoint another Regional Team member first.`,
            };
            mount();
            return;
          }
          state.reject = null;
          holders = holders.filter((x) => x.id !== b.dataset.revoke);
          logAct(`Revoked the <strong>${ROLE_DEFS[h.roleKey].name}</strong> role assignment from <strong>${h.name}</strong> — Person persists; structures &amp; sub-appointees inherited by the next scope-holder.`);
          mount();
        });
      });

      root.querySelectorAll('[data-act]').forEach((b) => {
        b.addEventListener('click', () => {
          const a = b.dataset.act;
          if (a === 'confirm') {
            const def = ROLE_DEFS[state.role];
            const name = state.person === 'new' ? (state.newPerson.name || 'New Person') : PEOPLE.find((p) => p.id === state.person).name;
            const initials = name.split(' ').map((w) => w[0]).slice(0, 2).join('').toUpperCase();
            holders.push({ id: 'h' + Date.now(), roleKey: state.role, name, initials, since: 'now' });
            logAct(`Appointed <strong>${name}</strong> as <strong>${def.name}</strong> (${def.scope}).`);
            state.person = null; state.username = ''; state.query = ''; state.password = genPassword();
            mount();
          } else if (a === 'clear') {
            state.person = null; state.username = ''; mount();
          } else if (a === 'cancel') {
            state.person = null; state.username = ''; mount();
          }
        });
      });
    }
    mount();
  }

  window.__VARIANTS = [{ key: 'main', name: 'Role appointment & revocation', render }];
})();
