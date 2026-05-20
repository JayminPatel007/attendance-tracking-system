// PROTOTYPE — Web · Structural creation (ADR-0009)
// MK creates Cities / Zones / Sabha Kinds. Sanyojak creates Kshetras.

(function () {
  const state = {
    role: 'mk', // 'mk' | 'sanyojak'
    tab: 'cities', // 'cities' | 'zones' | 'sabha-kinds' | 'kshetras'
    cities: [
      { id: 'c1', name: 'Mumbai', state: 'Maharashtra', zones: 4, sants: 2 },
      { id: 'c2', name: 'Pune', state: 'Maharashtra', zones: 3, sants: 1 },
      { id: 'c3', name: 'Ahmedabad', state: 'Gujarat', zones: 5, sants: 2 },
    ],
    zones: [
      { id: 'z1', name: 'Mumbai West', city: 'Mumbai', kshetras: 6 },
      { id: 'z2', name: 'Mumbai Central', city: 'Mumbai', kshetras: 5 },
      { id: 'z3', name: 'Mumbai East', city: 'Mumbai', kshetras: 4 },
      { id: 'z4', name: 'Pune Central', city: 'Pune', kshetras: 4 },
    ],
    sabhaKinds: [
      { id: 'sk1', demographic: 'Baal', track: 'Regular', sabhas: 42 },
      { id: 'sk2', demographic: 'Balika', track: 'Regular', sabhas: 38 },
      { id: 'sk3', demographic: 'Yuvak', track: 'Regular', sabhas: 36 },
      { id: 'sk4', demographic: 'Yuvati', track: 'Regular', sabhas: 31 },
      { id: 'sk5', demographic: 'Sanyukta', track: 'Regular', sabhas: 24 },
      { id: 'sk6', demographic: 'Baal', track: 'BSS', sabhas: 12 },
      { id: 'sk7', demographic: 'Yuvak', track: 'YSS', sabhas: 9 },
    ],
    kshetras: [
      { id: 'k1', name: 'Andheri-7', zone: 'Mumbai West', sabhas: 8 },
      { id: 'k2', name: 'Borivali-3', zone: 'Mumbai West', sabhas: 6 },
      { id: 'k3', name: 'Vile Parle-1', zone: 'Mumbai West', sabhas: 5 },
    ],
    newCity: { name: '', state: 'Gujarat' },
    newZone: { name: '', city: '' },
    newKind: { demographic: '', track: 'Regular' },
    newKshetra: { name: '', zone: 'Mumbai West' },
  };

  const ROLE_TABS = {
    mk: ['cities', 'zones', 'sabha-kinds'],
    sanyojak: ['kshetras'],
  };
  const TAB_LABELS = {
    cities: 'Cities', zones: 'Zones', 'sabha-kinds': 'Sabha Kinds', kshetras: 'Kshetras',
  };

  function render(root) {
    function setRoleChip() {
      const host = document.getElementById('role-chip-host');
      if (!host) return;
      if (state.role === 'mk') host.textContent = 'Madhyastha Karyalaya · Gujarat';
      else host.textContent = 'Yuvak Sanyojak · Mumbai West';
      const userHost = document.querySelector('.topnav .user');
      if (userHost) userHost.textContent = state.role === 'mk' ? 'Dipan Shah (MK)' : 'Manan Joshi (Sanyojak)';
    }

    function tabsHtml() {
      const allowed = ROLE_TABS[state.role];
      const all = ['cities', 'zones', 'sabha-kinds', 'kshetras'];
      return `
        <div class="tabs">
          ${all.map((t) => {
            const disabled = !allowed.includes(t);
            const count = state[t === 'sabha-kinds' ? 'sabhaKinds' : t].length;
            return `<div class="tab ${state.tab === t ? 'active' : ''} ${disabled ? 'disabled' : ''}" data-tab="${disabled ? '' : t}" ${disabled ? '' : ''}>
              ${TAB_LABELS[t]}<span class="count">${count}</span>${disabled ? '<span class="lock">🔒</span>' : ''}
            </div>`;
          }).join('')}
        </div>
      `;
    }

    function roleBanner() {
      const allowedList = ROLE_TABS[state.role].map((t) => TAB_LABELS[t]).join(', ');
      const auth = state.role === 'mk'
        ? 'As a Madhyastha Karyalaya member, you can create state-level structure. Creating an entity is attributed to you in the audit log.'
        : 'As a Sanyojak, you create Kshetras within your Zone. Cities, Zones, and Sabha Kinds are MK authority.';
      return `
        <div class="role-banner">
          <div><strong>${state.role === 'mk' ? 'Madhyastha Karyalaya' : 'Sanyojak'} authority</strong> — you can create: <strong>${allowedList}</strong>. ${auth}</div>
          <div class="role-switcher">
            <button class="${state.role === 'mk' ? 'active' : ''}" data-role="mk">MK view</button>
            <button class="${state.role === 'sanyojak' ? 'active' : ''}" data-role="sanyojak">Sanyojak view</button>
          </div>
        </div>
      `;
    }

    function citiesPanel() {
      return `
        <div class="entity-grid">
          <div class="entity-list">
            <div class="head"><h3>Cities · ${state.cities.length}</h3><span class="small">in Gujarat + Maharashtra deployment</span></div>
            ${state.cities.map((c) => `
              <div class="entity-row">
                <div class="info">
                  <div class="name">${c.name}</div>
                  <div class="sub">${c.state} · ${c.zones} Zones · ${c.sants} Sant${c.sants !== 1 ? 's' : ''} assigned</div>
                </div>
                <span class="meta">created by Dipan Shah</span>
              </div>
            `).join('')}
          </div>
          <div class="create-card">
            <h3>+ Add City</h3>
            <div class="sub">Each City supports a Sant overlay and multiple Zones beneath it.</div>
            <div class="field">
              <label>City name</label>
              <input id="city-name" placeholder="e.g., Surat" value="${state.newCity.name}" />
            </div>
            <div class="field">
              <label>State</label>
              <select id="city-state">
                <option ${state.newCity.state === 'Gujarat' ? 'selected' : ''}>Gujarat</option>
                <option ${state.newCity.state === 'Maharashtra' ? 'selected' : ''}>Maharashtra</option>
              </select>
              <div class="hint">Single deployment per ADR-0005 — state is informational.</div>
            </div>
            <button class="btn primary" style="width:100%;" data-act="create-city" ${state.newCity.name.trim() ? '' : 'disabled'}>Create City</button>
          </div>
        </div>
      `;
    }

    function zonesPanel() {
      return `
        <div class="entity-grid">
          <div class="entity-list">
            <div class="head"><h3>Zones · ${state.zones.length}</h3><span class="small">grouped by City</span></div>
            ${state.zones.map((z) => `
              <div class="entity-row">
                <div class="info">
                  <div class="name">${z.name}</div>
                  <div class="sub">in <strong>${z.city}</strong> · ${z.kshetras} Kshetras</div>
                </div>
              </div>
            `).join('')}
          </div>
          <div class="create-card">
            <h3>+ Add Zone</h3>
            <div class="sub">Sub-region within a City. Sanyojak roles attach per Zone × Sabha-kind.</div>
            <div class="field">
              <label>Zone name</label>
              <input id="zone-name" placeholder="e.g., Mumbai South" value="${state.newZone.name}" />
            </div>
            <div class="field">
              <label>Parent City</label>
              <select id="zone-city">
                <option value="">— pick City —</option>
                ${state.cities.map((c) => `<option ${c.name === state.newZone.city ? 'selected' : ''}>${c.name}</option>`).join('')}
              </select>
            </div>
            <button class="btn primary" style="width:100%;" data-act="create-zone" ${state.newZone.name.trim() && state.newZone.city ? '' : 'disabled'}>Create Zone</button>
          </div>
        </div>
      `;
    }

    function kindsPanel() {
      const dems = ['Baal', 'Balika', 'Yuvak', 'Yuvati', 'Sanyukta'];
      return `
        <div class="entity-grid">
          <div class="entity-list">
            <div class="head"><h3>Sabha Kinds · ${state.sabhaKinds.length}</h3><span class="small">demographic × track combinations</span></div>
            ${state.sabhaKinds.map((k) => `
              <div class="entity-row">
                <div class="info">
                  <div class="name">${k.demographic} (${k.track})</div>
                  <div class="sub">${k.sabhas} Sabhas of this kind in deployment</div>
                </div>
                ${k.track === 'Regular' ? '' : '<span class="pill warn xs">selective</span>'}
              </div>
            `).join('')}
          </div>
          <div class="create-card">
            <h3>+ Register Sabha Kind</h3>
            <div class="sub">A Sabha Kind is a demographic × track combination. Extensible per ADR-0009.</div>
            <div class="kind-builder">
              <h4>Kind builder</h4>
              <div class="row-2">
                <div class="field">
                  <label>Demographic</label>
                  <select id="kind-dem">
                    <option value="">—</option>
                    ${dems.map((d) => `<option ${d === state.newKind.demographic ? 'selected' : ''}>${d}</option>`).join('')}
                  </select>
                </div>
                <div class="field">
                  <label>Track</label>
                  <div class="track-radio">
                    ${['Regular', 'BSS', 'YSS'].map((t) => `
                      <label class="${state.newKind.track === t ? 'selected' : ''}" data-track="${t}">
                        <input type="radio" name="track" value="${t}" />${t}
                      </label>
                    `).join('')}
                  </div>
                </div>
              </div>
              <div class="hint">Sanyukta is Regular-track only — system disallows BSS/YSS Sanyukta.</div>
            </div>
            <button class="btn primary" style="width:100%;" data-act="create-kind" ${state.newKind.demographic && !(state.newKind.demographic === 'Sanyukta' && state.newKind.track !== 'Regular') ? '' : 'disabled'}>Register Kind</button>
          </div>
        </div>
      `;
    }

    function kshetrasPanel() {
      return `
        <div class="entity-grid">
          <div class="entity-list">
            <div class="head"><h3>Kshetras in your Zone · ${state.kshetras.length}</h3><span class="small">Mumbai West</span></div>
            ${state.kshetras.map((k) => `
              <div class="entity-row">
                <div class="info">
                  <div class="name">${k.name}</div>
                  <div class="sub">${k.sabhas} Sabhas · in ${k.zone}</div>
                </div>
              </div>
            `).join('')}
          </div>
          <div class="create-card">
            <h3>+ Add Kshetra</h3>
            <div class="sub">Sub-region of your Zone. Hosts ~10–20 Sabhas typically.</div>
            <div class="field">
              <label>Kshetra name</label>
              <input id="ksh-name" placeholder="e.g., Goregaon-2" value="${state.newKshetra.name}" />
            </div>
            <div class="field">
              <label>Parent Zone</label>
              <select disabled><option>Mumbai West (your scope)</option></select>
              <div class="hint">Locked to your Sanyojak scope.</div>
            </div>
            <button class="btn primary" style="width:100%;" data-act="create-kshetra" ${state.newKshetra.name.trim() ? '' : 'disabled'}>Create Kshetra</button>
          </div>
        </div>
      `;
    }

    function panel() {
      if (!ROLE_TABS[state.role].includes(state.tab)) {
        return `<div class="entity-list"><div class="disabled-tab-note">This tab is not within your authority. ${state.role === 'mk' ? 'Kshetras are Sanyojak authority.' : 'Switch to MK view for Cities / Zones / Sabha Kinds.'}</div></div>`;
      }
      if (state.tab === 'cities') return citiesPanel();
      if (state.tab === 'zones') return zonesPanel();
      if (state.tab === 'sabha-kinds') return kindsPanel();
      if (state.tab === 'kshetras') return kshetrasPanel();
      return '';
    }

    function mount() {
      // Sync tab if role switch invalidates current
      if (!ROLE_TABS[state.role].includes(state.tab)) {
        state.tab = ROLE_TABS[state.role][0];
      }
      setRoleChip();
      root.innerHTML = `
        <div class="page-head" style="margin-bottom:18px;">
          <h1 style="font-size:22px;margin:0 0 4px;">Structural admin</h1>
          <div class="sub small muted">Create Cities, Zones, Sabha Kinds (MK) or Kshetras (Sanyojak). Authority per ADR-0009.</div>
        </div>
        ${roleBanner()}
        ${tabsHtml()}
        ${panel()}
      `;
      attach();
    }

    function attach() {
      root.querySelectorAll('.role-switcher button').forEach((b) => {
        b.addEventListener('click', () => {
          state.role = b.dataset.role;
          mount();
        });
      });
      root.querySelectorAll('.tab').forEach((t) => {
        t.addEventListener('click', () => {
          if (t.classList.contains('disabled')) return;
          if (!t.dataset.tab) return;
          state.tab = t.dataset.tab;
          mount();
        });
      });

      const setVal = (id, key, sub) => {
        const el = root.querySelector('#' + id);
        if (!el) return;
        el.addEventListener('input', (e) => {
          if (sub) state[key][sub] = e.target.value;
          else state[key] = e.target.value;
          mount();
        });
      };
      setVal('city-name', 'newCity', 'name');
      setVal('city-state', 'newCity', 'state');
      setVal('zone-name', 'newZone', 'name');
      setVal('zone-city', 'newZone', 'city');
      setVal('kind-dem', 'newKind', 'demographic');
      setVal('ksh-name', 'newKshetra', 'name');

      root.querySelectorAll('.track-radio label').forEach((lb) => {
        lb.addEventListener('click', () => {
          state.newKind.track = lb.dataset.track;
          mount();
        });
      });

      root.querySelectorAll('[data-act]').forEach((b) => {
        b.addEventListener('click', () => {
          const a = b.dataset.act;
          if (a === 'create-city') {
            state.cities.push({ id: 'c' + Date.now(), name: state.newCity.name, state: state.newCity.state, zones: 0, sants: 0 });
            state.newCity = { name: '', state: 'Gujarat' };
            mount();
          } else if (a === 'create-zone') {
            state.zones.push({ id: 'z' + Date.now(), name: state.newZone.name, city: state.newZone.city, kshetras: 0 });
            state.newZone = { name: '', city: '' };
            mount();
          } else if (a === 'create-kind') {
            state.sabhaKinds.push({ id: 'sk' + Date.now(), demographic: state.newKind.demographic, track: state.newKind.track, sabhas: 0 });
            state.newKind = { demographic: '', track: 'Regular' };
            mount();
          } else if (a === 'create-kshetra') {
            state.kshetras.push({ id: 'k' + Date.now(), name: state.newKshetra.name, zone: 'Mumbai West', sabhas: 0 });
            state.newKshetra = { name: '', zone: 'Mumbai West' };
            mount();
          }
        });
      });
    }
    mount();
  }

  window.__VARIANTS = [{ key: 'main', name: 'Structural admin', render }];
})();
