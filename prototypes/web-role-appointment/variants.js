// PROTOTYPE — Web · Role Appointment · chosen design
// Single long form + always-visible directory-style Person picker (ADR-0011).

(function () {
  const ROLES = [
    { key: 'sanchalak', name: 'Sanchalak', sub: 'Per (Kshetra, demographic, track)', appointer: 'Nirdeshak' },
    { key: 'sah-sanchalak', name: 'Sah-Sanchalak', sub: 'Deputy to Sanchalak', appointer: 'Nirdeshak' },
    { key: 'nirikshak', name: 'Nirikshak', sub: '3–4 Regular Sabhas in a Kshetra', appointer: 'Nirdeshak' },
  ];

  // Existing People in the Directory (subset)
  const PEOPLE = [
    { id: 'p20', initials: 'PP', name: 'Pratik Patel', mobile: '98201 98201', roles: ['Yuvak Nirdeshak · Andheri-7'] },
    { id: 'p21', initials: 'JS', name: 'Jignesh Solanki', mobile: '98201 32100', roles: [] },
    { id: 'p22', initials: 'AM', name: 'Ashish Mehta', mobile: '98201 47410', roles: ['Sanchalak · BSS Baal · Andheri-7'] },
    { id: 'p23', initials: 'VP', name: 'Vivek Pandya', mobile: '98201 55432', roles: [] },
    { id: 'p24', initials: 'NJ', name: 'Nikhil Joshi', mobile: '98201 60601', roles: ['Sah-Sanchalak · Yuvak · Borivali-3'] },
  ];

  function genUsername(name) {
    return name.toLowerCase().split(' ').join('.') + '.atk';
  }
  function genPassword() {
    const a = 'kshetra,sabha,seva,arpit,nirmal,sumati,chetan'.split(',');
    return a[Math.floor(Math.random() * a.length)] + '-' + Math.floor(1000 + Math.random() * 9000);
  }

  function render(root) {
    const state = {
      role: 'sanchalak',
      sabhaKind: 'Yuvak (Regular)',
      kshetra: 'Andheri-7',
      query: '',
      person: null, // PEOPLE id or 'new'
      newPerson: { name: '', mobile: '', dob: '' },
      username: '',
      password: genPassword(),
    };

    function chosenLabel() {
      if (!state.person) return null;
      if (state.person === 'new') return state.newPerson.name || '(new Person — name pending)';
      const p = PEOPLE.find((pp) => pp.id === state.person);
      return p ? `${p.name} · ${p.mobile}` : null;
    }

    function mount() {
      const q = state.query.toLowerCase();
      const list = q
        ? PEOPLE.filter((p) => p.name.toLowerCase().includes(q) || p.mobile.includes(q))
        : PEOPLE;
      const chosen = chosenLabel();

      root.innerHTML = `
        <div class="vd">
          <h2 class="section">Appoint role</h2>
          <p class="lead">Single-screen form. Existing Users are picked from the directory list below — same affordances as the Directory section.</p>
          <div class="form-card">
            <h3>Role &amp; scope</h3>
            <div class="row">
              <div class="field">
                <label>Role</label>
                <select id="role">
                  ${ROLES.map(
                    (r) =>
                      `<option value="${r.key}" ${r.key === state.role ? 'selected' : ''}>${r.name}</option>`,
                  ).join('')}
                </select>
              </div>
              <div class="field">
                <label>Sabha kind</label>
                <select id="sk">
                  <option ${state.sabhaKind === 'Yuvak (Regular)' ? 'selected' : ''}>Yuvak (Regular)</option>
                  <option>Yuvak (BSS)</option>
                  <option>Baal (Regular)</option>
                </select>
              </div>
            </div>
            <div class="field">
              <label>Kshetra</label>
              <select id="ks">
                <option>Andheri-7</option><option>Borivali-3</option><option>Vile Parle-1</option>
              </select>
            </div>

            <h3>Person — pick from Directory or create new</h3>
            ${
              chosen
                ? `
              <div class="chosen-banner">
                <span>✓ Chosen: <strong>${chosen}</strong></span>
                <a data-act="clear">Clear</a>
              </div>
            `
                : ''
            }
            <div class="picker">
              <div class="picker-search">
                <input id="q" placeholder="Search by name or mobile" value="${state.query}" autocomplete="off" />
                <span class="count">${list.length} of ${PEOPLE.length}</span>
              </div>
              <div class="picker-list">
                ${list
                  .map(
                    (p) => `
                  <div class="picker-row ${state.person === p.id ? 'selected' : ''}" data-pid="${p.id}">
                    <div class="avatar">${p.initials}</div>
                    <div class="info">
                      <div class="name">${p.name}</div>
                      <div class="sub">${p.mobile}</div>
                      ${
                        p.roles.length
                          ? `<div class="roles-row">${p.roles.map((r) => `<span class="role-chip-sm">${r}</span>`).join('')}</div>`
                          : `<div class="roles-row"><span class="muted xs">No current roles · not yet a User</span></div>`
                      }
                    </div>
                    <span class="check">✓</span>
                  </div>
                `,
                  )
                  .join('') || '<div style="padding:20px;text-align:center;" class="muted small">No People match.</div>'}
                <div class="picker-row create" data-pid="new">
                  <div class="avatar" style="background:var(--accent-soft);color:var(--accent);">+</div>
                  <div class="info"><div class="name">Create new Person${state.query ? ` matching "${state.query}"` : ''}</div><div class="sub">Adds to Directory and continues with appointment.</div></div>
                </div>
              </div>
            </div>
            ${
              state.person === 'new'
                ? `
              <div class="inline-create">
                <h4>New Person details</h4>
                <div class="row">
                  <div class="field"><label>Full name</label><input id="np-name" value="${state.newPerson.name}" /></div>
                  <div class="field"><label>Mobile (de-dup check)</label><input id="np-mob" value="${state.newPerson.mobile}" /></div>
                </div>
                <div class="field"><label>Date of birth (optional)</label><input id="np-dob" type="date" value="${state.newPerson.dob}" /></div>
              </div>
            `
                : ''
            }

            <h3>Initial credentials</h3>
            <div class="credentials-suggest">Handed to the new User. They'll be required to change the password on first login (ADR-0004).</div>
            <div class="row">
              <div class="field"><label>Username</label><input id="un" placeholder="auto-suggested from chosen Person" value="${state.username}" /></div>
              <div class="field"><label>Password</label><input id="pw" value="${state.password}" /></div>
            </div>

            <div class="actions">
              <button class="btn" data-act="cancel" style="flex:1;">Cancel</button>
              <button class="btn primary" data-act="confirm" style="flex:2;" ${state.person ? '' : 'disabled'}>Appoint</button>
            </div>
          </div>
        </div>
      `;
      attach();
    }

    function attach() {
      const sel = (id) => root.querySelector('#' + id);
      sel('role')?.addEventListener('change', (e) => (state.role = e.target.value));
      sel('sk')?.addEventListener('change', (e) => (state.sabhaKind = e.target.value));
      sel('ks')?.addEventListener('change', (e) => (state.kshetra = e.target.value));
      const q = sel('q');
      if (q) {
        q.addEventListener('input', (e) => {
          state.query = e.target.value;
          mount();
          root.querySelector('#q').focus();
        });
      }
      root.querySelectorAll('[data-pid]').forEach((el) => {
        el.addEventListener('click', () => {
          state.person = el.dataset.pid;
          const ch =
            state.person === 'new' ? null : PEOPLE.find((p) => p.id === state.person);
          if (ch) state.username = genUsername(ch.name);
          else if (state.person === 'new' && state.newPerson.name) state.username = genUsername(state.newPerson.name);
          mount();
        });
      });
      const npn = sel('np-name');
      if (npn)
        npn.addEventListener('input', (e) => {
          state.newPerson.name = e.target.value;
          if (state.newPerson.name) state.username = genUsername(state.newPerson.name);
          const unEl = sel('un');
          if (unEl) unEl.value = state.username;
          const banner = root.querySelector('.chosen-banner strong');
          if (banner) banner.textContent = e.target.value || '(new Person — name pending)';
        });
      const npm = sel('np-mob');
      if (npm) npm.addEventListener('input', (e) => (state.newPerson.mobile = e.target.value));
      const npd = sel('np-dob');
      if (npd) npd.addEventListener('input', (e) => (state.newPerson.dob = e.target.value));
      sel('un')?.addEventListener('input', (e) => (state.username = e.target.value));
      sel('pw')?.addEventListener('input', (e) => (state.password = e.target.value));
      root.querySelectorAll('[data-act]').forEach((b) => {
        b.addEventListener('click', () => {
          const a = b.dataset.act;
          if (a === 'confirm') alert('Appointment confirmed (stub).');
          else if (a === 'clear') {
            state.person = null;
            state.username = '';
            mount();
          } else if (a === 'cancel') alert('Cancelled.');
        });
      });
    }
    mount();
  }

  window.__VARIANTS = [{ key: 'main', name: 'Role appointment', render }];
})();
