// PROTOTYPE — Web · Structural creation (revised authority chain, supersedes ADR-0009)
// Authority is BY SCOPE, not by creator — any current holder of a scope can create AND delete.
//   MK             → Cities, Sabha Kinds. (Zones moved OUT to Regional Team.)
//   Regional Team  → Zones (within their City).
//   Sanyojak       → Kshetras (within their Zone).
// Delete semantics: geographic = block-if-non-empty; Sabha Kind = soft-retire (drain), never hard-delete.

(function () {
  const state = {
    role: 'mk', // 'mk' | 'regional-team' | 'sanyojak'
    tab: 'cities', // 'cities' | 'zones' | 'sabha-kinds' | 'kshetras'
    adding: false, // is the inline add row expanded for the current tab?
    cities: [
      { id: 'c1', name: 'Mumbai', state: 'Maharashtra', zones: 4, sants: 2 },
      { id: 'c2', name: 'Pune', state: 'Maharashtra', zones: 3, sants: 1 },
      { id: 'c3', name: 'Ahmedabad', state: 'Gujarat', zones: 5, sants: 2 },
      { id: 'c4', name: 'Surat', state: 'Gujarat', zones: 0, sants: 0 }, // empty → deletable
    ],
    zones: [
      { id: 'z1', name: 'Mumbai West', city: 'Mumbai', kshetras: 6 },
      { id: 'z2', name: 'Mumbai Central', city: 'Mumbai', kshetras: 5 },
      { id: 'z3', name: 'Mumbai East', city: 'Mumbai', kshetras: 4 },
      { id: 'z4', name: 'Mumbai South', city: 'Mumbai', kshetras: 0 }, // empty → deletable
    ],
    sabhaKinds: [
      { id: 'sk1', demographic: 'Baal', track: 'Regular', sabhas: 42, retired: false },
      { id: 'sk2', demographic: 'Balika', track: 'Regular', sabhas: 38, retired: false },
      { id: 'sk3', demographic: 'Yuvak', track: 'Regular', sabhas: 36, retired: false },
      { id: 'sk4', demographic: 'Yuvati', track: 'Regular', sabhas: 31, retired: false },
      { id: 'sk5', demographic: 'Sanyukta', track: 'Regular', sabhas: 24, retired: false },
      { id: 'sk6', demographic: 'Baal', track: 'BSS', sabhas: 12, retired: false },
      { id: 'sk7', demographic: 'Yuvak', track: 'YSS', sabhas: 9, retired: false },
    ],
    kshetras: [
      { id: 'k1', name: 'Andheri-7', zone: 'Mumbai West', sabhas: 8 },
      { id: 'k2', name: 'Borivali-3', zone: 'Mumbai West', sabhas: 6 },
      { id: 'k3', name: 'Vile Parle-1', zone: 'Mumbai West', sabhas: 0 }, // empty → deletable
    ],
    newCity: { name: '', state: 'Gujarat' },
    newZone: { name: '' },
    newKind: { demographic: '', track: 'Regular' },
    newKshetra: { name: '' },
    log: [],
  };

  // Which tabs each actor's SCOPE lets them act on. Tabs outside the actor's scope are HIDDEN, not locked.
  const ROLE_TABS = {
    mk: ['cities', 'sabha-kinds'],
    'regional-team': ['zones'],
    sanyojak: ['kshetras'],
  };
  const TAB_LABELS = { cities: 'Cities', zones: 'Zones', 'sabha-kinds': 'Sabha Kinds', kshetras: 'Kshetras' };
  const ROLE_META = {
    mk: { name: 'Madhyastha Karyalaya', chip: 'Madhyastha Karyalaya · Gujarat', user: 'Dipan Shah (MK)' },
    'regional-team': { name: 'Regional Team', chip: 'Regional Team · Mumbai · Yuvak', user: 'Hardik Trivedi (RT)' },
    sanyojak: { name: 'Sanyojak', chip: 'Yuvak Sanyojak · Mumbai West', user: 'Manan Joshi (Sanyojak)' },
  };

  // Validity of the inline add form for the current tab.
  function addValid() {
    if (state.tab === 'cities') return !!state.newCity.name.trim();
    if (state.tab === 'zones') return !!state.newZone.name.trim();
    if (state.tab === 'sabha-kinds') return state.newKind.demographic && !(state.newKind.demographic === 'Sanyukta' && state.newKind.track !== 'Regular');
    if (state.tab === 'kshetras') return !!state.newKshetra.name.trim();
    return false;
  }

  function logAct(msg) { state.log.unshift({ t: new Date().toLocaleTimeString(), msg }); }

  function render(root) {
    function setRoleChip() {
      const host = document.getElementById('role-chip-host');
      if (host) host.textContent = ROLE_META[state.role].chip;
      const userHost = document.querySelector('.topnav .user');
      if (userHost) userHost.textContent = ROLE_META[state.role].user;
    }

    // Only render the tabs this actor's scope grants — hidden, not locked.
    function tabsHtml() {
      const allowed = ROLE_TABS[state.role];
      if (allowed.length <= 1) return '';
      return `
        <div class="tabs">
          ${allowed.map((t) => {
            const count = state[t === 'sabha-kinds' ? 'sabhaKinds' : t].length;
            return `<div class="tab ${state.tab === t ? 'active' : ''}" data-tab="${t}">${TAB_LABELS[t]}<span class="count">${count}</span></div>`;
          }).join('')}
        </div>
      `;
    }

    function roleBanner() {
      const allowedList = ROLE_TABS[state.role].map((t) => TAB_LABELS[t]).join(', ');
      const auth = {
        mk: 'MK creates Cities and Sabha Kinds. <strong>Zones moved down to the Regional Team.</strong> Delete a City only while it has no Zones; a Sabha Kind is soft-retired (drains), never hard-deleted.',
        'regional-team': 'The Regional Team owns Zone creation within its City. Delete a Zone only while it has no Kshetras (block-if-non-empty).',
        sanyojak: 'A Sanyojak creates Kshetras within its Zone. Delete a Kshetra only while it has no Sabhas (block-if-non-empty).',
      }[state.role];
      return `
        <div class="role-banner">
          <div><strong>${ROLE_META[state.role].name} authority</strong> — scope lets you create &amp; delete: <strong>${allowedList}</strong>. ${auth}</div>
          <div class="role-switcher">
            <button class="${state.role === 'mk' ? 'active' : ''}" data-role="mk">MK</button>
            <button class="${state.role === 'regional-team' ? 'active' : ''}" data-role="regional-team">Regional Team</button>
            <button class="${state.role === 'sanyojak' ? 'active' : ''}" data-role="sanyojak">Sanyojak</button>
          </div>
        </div>
      `;
    }

    function deleteBtn(kind, id, blocked, reason) {
      if (blocked) return `<button class="btn ghost del" disabled title="${reason}">🚫 ${reason}</button>`;
      return `<button class="btn danger xs del" data-del="${kind}" data-id="${id}">Delete</button>`;
    }

    // Inline add row: collapsed = dashed "+ Add X" trigger; expanded = fields in place.
    function addRow(label, fieldsHtml) {
      if (!state.adding) {
        return `<div class="add-trigger" data-act="open-add">+ Add ${label}</div>`;
      }
      return `
        <div class="add-inline">
          <div class="add-fields">${fieldsHtml}</div>
          <div class="add-actions">
            <button class="btn ghost-btn xs" data-act="cancel-add">Cancel</button>
            <button class="btn primary xs" data-act="do-add" ${addValid() ? '' : 'disabled'}>Add</button>
          </div>
        </div>
      `;
    }

    function listShell(title, sub, addLabel, addFields, rows) {
      return `
        <div class="entity-list wide">
          <div class="head"><h3>${title}</h3><span class="small">${sub}</span></div>
          ${addRow(addLabel, addFields)}
          ${rows}
        </div>
      `;
    }

    function citiesPanel() {
      const fields = `
        <div class="add-field"><label>City name</label><input id="city-name" placeholder="e.g., Vadodara" value="${state.newCity.name}" /></div>
        <div class="add-field"><label>State</label>
          <select id="city-state">
            <option ${state.newCity.state === 'Gujarat' ? 'selected' : ''}>Gujarat</option>
            <option ${state.newCity.state === 'Maharashtra' ? 'selected' : ''}>Maharashtra</option>
          </select>
        </div>`;
      const rows = state.cities.map((c) => `
        <div class="entity-row">
          <div class="info"><div class="name">${c.name}</div><div class="sub">${c.state} · ${c.zones} Zones · ${c.sants} Sant${c.sants !== 1 ? 's' : ''} assigned</div></div>
          ${deleteBtn('city', c.id, c.zones > 0, `has ${c.zones} Zones`)}
        </div>`).join('');
      return listShell(`Cities · ${state.cities.length}`, 'delete blocked while Zones exist', 'City', fields, rows);
    }

    function zonesPanel() {
      const fields = `
        <div class="add-field"><label>Zone name</label><input id="zone-name" placeholder="e.g., Mumbai North" value="${state.newZone.name}" /></div>
        <div class="add-field"><label>City</label><input value="Mumbai (your scope)" disabled /></div>`;
      const rows = state.zones.map((z) => `
        <div class="entity-row">
          <div class="info"><div class="name">${z.name}</div><div class="sub">in <strong>${z.city}</strong> · ${z.kshetras} Kshetras</div></div>
          ${deleteBtn('zone', z.id, z.kshetras > 0, `has ${z.kshetras} Kshetras`)}
        </div>`).join('');
      return listShell(`Zones · ${state.zones.length}`, 'Regional Team scope · delete blocked while Kshetras exist', 'Zone', fields, rows);
    }

    function kindsPanel() {
      const dems = ['Baal', 'Balika', 'Yuvak', 'Yuvati', 'Sanyukta'];
      const fields = `
        <div class="add-field"><label>Demographic</label>
          <select id="kind-dem"><option value="">—</option>${dems.map((d) => `<option ${d === state.newKind.demographic ? 'selected' : ''}>${d}</option>`).join('')}</select>
        </div>
        <div class="add-field"><label>Track</label>
          <div class="track-radio">
            ${['Regular', 'BSS', 'YSS'].map((t) => `<label class="${state.newKind.track === t ? 'selected' : ''}" data-track="${t}"><input type="radio" name="track" value="${t}" />${t}</label>`).join('')}
          </div>
        </div>
        <div class="add-hint">Sanyukta is Regular-track only — system disallows BSS/YSS Sanyukta.</div>`;
      const rows = state.sabhaKinds.map((k) => `
        <div class="entity-row ${k.retired ? 'retired' : ''}">
          <div class="info">
            <div class="name">${k.demographic} (${k.track}) ${k.retired ? '<span class="pill muted xs">retired</span>' : ''}</div>
            <div class="sub">${k.sabhas} Sabhas of this kind${k.retired ? ' · draining, no new allowed' : ' in deployment'}</div>
          </div>
          ${k.track !== 'Regular' && !k.retired ? '<span class="pill warn xs">selective</span>' : ''}
          ${k.retired
            ? `<button class="btn xs del" data-act="reactivate-kind" data-id="${k.id}">Reactivate</button>`
            : `<button class="btn warn-btn xs del" data-act="retire-kind" data-id="${k.id}">Soft-retire</button>`}
        </div>`).join('');
      return listShell(`Sabha Kinds · ${state.sabhaKinds.length}`, 'soft-retire only — existing Sabhas drain', 'Sabha Kind', fields, rows);
    }

    function kshetrasPanel() {
      const fields = `
        <div class="add-field"><label>Kshetra name</label><input id="ksh-name" placeholder="e.g., Goregaon-2" value="${state.newKshetra.name}" /></div>
        <div class="add-field"><label>Zone</label><input value="Mumbai West (your scope)" disabled /></div>`;
      const rows = state.kshetras.map((k) => `
        <div class="entity-row">
          <div class="info"><div class="name">${k.name}</div><div class="sub">${k.sabhas} Sabhas · in ${k.zone}</div></div>
          ${deleteBtn('kshetra', k.id, k.sabhas > 0, `has ${k.sabhas} Sabhas`)}
        </div>`).join('');
      return listShell(`Kshetras in your Zone · ${state.kshetras.length}`, 'Mumbai West · delete blocked while Sabhas exist', 'Kshetra', fields, rows);
    }

    function logPanel() {
      if (!state.log.length) return '';
      return `
        <div class="action-log">
          <h4>Authority log — scope-based, not creator-based</h4>
          ${state.log.map((l) => `<div class="log-row"><span class="t">${l.t}</span> ${l.msg}</div>`).join('')}
        </div>
      `;
    }

    function panel() {
      if (state.tab === 'cities') return citiesPanel();
      if (state.tab === 'zones') return zonesPanel();
      if (state.tab === 'sabha-kinds') return kindsPanel();
      if (state.tab === 'kshetras') return kshetrasPanel();
      return '';
    }

    function mount() {
      if (!ROLE_TABS[state.role].includes(state.tab)) state.tab = ROLE_TABS[state.role][0];
      setRoleChip();
      root.innerHTML = `
        <div class="page-head" style="margin-bottom:18px;">
          <h1 style="font-size:22px;margin:0 0 4px;">Structural admin</h1>
          <div class="sub small muted">Create &amp; delete structure by scope. MK → Cities / Sabha Kinds · Regional Team → Zones · Sanyojak → Kshetras.</div>
        </div>
        ${roleBanner()}
        ${tabsHtml()}
        ${panel()}
        ${logPanel()}
      `;
      attach();
    }

    // Update the inline Add button's disabled state without a full re-render (keeps text focus).
    function refreshAddBtn() {
      const btn = root.querySelector('[data-act="do-add"]');
      if (btn) btn.disabled = !addValid();
    }

    function switchTab(tab) { state.tab = tab; state.adding = false; mount(); }

    function attach() {
      root.querySelectorAll('.role-switcher button').forEach((b) => {
        b.addEventListener('click', () => { state.role = b.dataset.role; state.adding = false; mount(); });
      });
      root.querySelectorAll('.tab').forEach((t) => {
        t.addEventListener('click', () => { if (t.dataset.tab) switchTab(t.dataset.tab); });
      });

      // Text inputs: update state silently + refresh the Add button only (no re-mount → focus kept).
      const txt = (id, key, sub) => {
        const el = root.querySelector('#' + id);
        if (!el) return;
        el.addEventListener('input', (e) => { if (sub) state[key][sub] = e.target.value; else state[key] = e.target.value; refreshAddBtn(); });
      };
      txt('city-name', 'newCity', 'name');
      txt('zone-name', 'newZone', 'name');
      txt('ksh-name', 'newKshetra', 'name');

      // Selects / radios: re-mount is fine (no typed focus to lose).
      root.querySelector('#city-state')?.addEventListener('change', (e) => { state.newCity.state = e.target.value; });
      root.querySelector('#kind-dem')?.addEventListener('change', (e) => { state.newKind.demographic = e.target.value; mount(); });
      root.querySelectorAll('.track-radio label').forEach((lb) => {
        lb.addEventListener('click', () => { state.newKind.track = lb.dataset.track; mount(); });
      });

      // Deletes — scope-based, with guards.
      root.querySelectorAll('[data-del]').forEach((b) => {
        b.addEventListener('click', () => {
          const kind = b.dataset.del, id = b.dataset.id;
          if (kind === 'city') { const c = state.cities.find((x) => x.id === id); state.cities = state.cities.filter((x) => x.id !== id); logAct(`Deleted City <strong>${c.name}</strong> (was empty).`); }
          else if (kind === 'zone') { const z = state.zones.find((x) => x.id === id); state.zones = state.zones.filter((x) => x.id !== id); logAct(`Deleted Zone <strong>${z.name}</strong> (was empty).`); }
          else if (kind === 'kshetra') { const k = state.kshetras.find((x) => x.id === id); state.kshetras = state.kshetras.filter((x) => x.id !== id); logAct(`Deleted Kshetra <strong>${k.name}</strong> (was empty).`); }
          mount();
        });
      });

      root.querySelectorAll('[data-act]').forEach((b) => {
        b.addEventListener('click', () => {
          const a = b.dataset.act;
          if (a === 'open-add') { state.adding = true; mount(); return; }
          if (a === 'cancel-add') {
            state.adding = false;
            state.newCity = { name: '', state: 'Gujarat' }; state.newZone = { name: '' };
            state.newKind = { demographic: '', track: 'Regular' }; state.newKshetra = { name: '' };
            mount(); return;
          }
          if (a === 'do-add') {
            if (!addValid()) return;
            if (state.tab === 'cities') { state.cities.push({ id: 'c' + Date.now(), name: state.newCity.name, state: state.newCity.state, zones: 0, sants: 0 }); logAct(`Created City <strong>${state.newCity.name}</strong>.`); state.newCity = { name: '', state: 'Gujarat' }; }
            else if (state.tab === 'zones') { state.zones.push({ id: 'z' + Date.now(), name: state.newZone.name, city: 'Mumbai', kshetras: 0 }); logAct(`Created Zone <strong>${state.newZone.name}</strong> in Mumbai.`); state.newZone = { name: '' }; }
            else if (state.tab === 'sabha-kinds') { state.sabhaKinds.push({ id: 'sk' + Date.now(), demographic: state.newKind.demographic, track: state.newKind.track, sabhas: 0, retired: false }); logAct(`Registered Sabha Kind <strong>${state.newKind.demographic} (${state.newKind.track})</strong>.`); state.newKind = { demographic: '', track: 'Regular' }; }
            else if (state.tab === 'kshetras') { state.kshetras.push({ id: 'k' + Date.now(), name: state.newKshetra.name, zone: 'Mumbai West', sabhas: 0 }); logAct(`Created Kshetra <strong>${state.newKshetra.name}</strong>.`); state.newKshetra = { name: '' }; }
            state.adding = false; mount(); return;
          }
          if (a === 'retire-kind') { const k = state.sabhaKinds.find((x) => x.id === b.dataset.id); k.retired = true; logAct(`Soft-retired <strong>${k.demographic} (${k.track})</strong> — ${k.sabhas} existing Sabhas drain, no new allowed.`); mount(); return; }
          if (a === 'reactivate-kind') { const k = state.sabhaKinds.find((x) => x.id === b.dataset.id); k.retired = false; logAct(`Reactivated <strong>${k.demographic} (${k.track})</strong>.`); mount(); return; }
        });
      });
    }
    mount();
  }

  window.__VARIANTS = [{ key: 'main', name: 'Structural admin', render }];
})();
