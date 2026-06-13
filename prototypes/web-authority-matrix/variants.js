// PROTOTYPE — Web · Authority matrix · single per-actor "what can I create / delete here?" view.
// Question: is the revised scope-based authority chain legible to the person holding a role?
// Authority is by SCOPE, not by creator. Three delete kinds:
//   block-if-non-empty (geographic) · soft-retire (Sabha Kind) · revoke (role assignment).

(function () {
  const ACTORS = {
    mk:             { name: 'Madhyastha Karyalaya', chip: 'Madhyastha Karyalaya · Gujarat', user: 'Dipan Shah (MK)' },
    'regional-team':{ name: 'Regional Team',         chip: 'Regional Team · Mumbai · Yuvak', user: 'Hardik Trivedi (RT)' },
    sanyojak:       { name: 'Sanyojak',              chip: 'Yuvak Sanyojak · Mumbai West', user: 'Manan Joshi (Sanyojak)' },
    nirdeshak:      { name: 'Nirdeshak',             chip: 'Yuvak Nirdeshak · Andheri-7', user: 'Pratik Patel (Nirdeshak)' },
    'sah-nirdeshak':{ name: 'Sah-Nirdeshak',         chip: 'Yuvak Sah-Nirdeshak · Andheri-7', user: 'Rohan Desai (Sah-Nirdeshak)' },
  };

  // del kinds: 'empty' (block-if-non-empty) · 'retire' (soft-retire) · 'revoke' · false (no delete authority)
  const AUTHORITY = {
    mk: {
      structures: [
        { name: 'City', del: 'empty', delNote: 'only while it has no Zones' },
        { name: 'Sabha Kind', del: 'retire', delNote: 'soft-retire: inactive, existing drain — never hard-deleted' },
        { name: 'Zone', create: false, note: 'Zone creation moved DOWN to the Regional Team' },
      ],
      roles: [
        { name: 'Regional Team member', appoint: 'the FIRST per (City, demographic)', del: 'revoke' },
        { name: 'Sant credentials', appoint: 'admin act (sets initial login)', del: 'revoke' },
      ],
    },
    'regional-team': {
      structures: [
        { name: 'Zone', del: 'empty', delNote: 'only while it has no Kshetras' },
      ],
      roles: [
        { name: 'Regional Team member (peer)', appoint: 'self-replicating — same (City, demographic)', del: 'revoke', delNote: 'blocked only on the LAST remaining member' },
        { name: 'Sanyojak', appoint: 'per (Zone, demographic)', del: 'revoke' },
      ],
    },
    sanyojak: {
      structures: [
        { name: 'Kshetra', del: 'empty', delNote: 'only while it has no Sabhas' },
      ],
      roles: [
        { name: 'Nirdeshak', appoint: 'per (Kshetra, demographic)', del: 'revoke' },
      ],
    },
    nirdeshak: {
      structures: [
        { name: 'Sabha', del: 'empty', delNote: 'only while it has no recorded Occurrences / attendance' },
      ],
      roles: [
        { name: 'Sanchalak', appoint: 'per Sabha', del: 'revoke' },
        { name: 'Sah-Sanchalak', appoint: 'deputy', del: 'revoke' },
        { name: 'Nirikshak', appoint: 'assign its mutable set of 3–4 Sabhas', del: 'revoke' },
        { name: 'Sah-Nirdeshak', appoint: 'MAX 2 per (Kshetra, demographic)', del: 'revoke' },
      ],
    },
    'sah-nirdeshak': {
      structures: [],
      roles: [],
      proxy: [
        'Reopen finalized Occurrences',
        'Act on the Kshetra’s Sabhas during the Nirdeshak’s absence (operational proxy)',
        'Same analytics view as the Nirdeshak — read-only',
      ],
    },
  };

  const DEL_LABEL = {
    empty: { text: 'Delete: block-if-non-empty', cls: 'warn' },
    retire: { text: 'Delete: soft-retire', cls: 'warn' },
    revoke: { text: 'Delete: revoke assignment', cls: 'muted' },
  };

  function render(root) {
    const state = { actor: 'nirdeshak' };

    function setChip() {
      const c = document.getElementById('role-chip-host'); if (c) c.textContent = ACTORS[state.actor].chip;
      const u = document.getElementById('user-host'); if (u) u.textContent = ACTORS[state.actor].user;
    }

    function createBadge(item) {
      if (item.create === false) return `<span class="pill muted">✗ not your authority</span>`;
      return `<span class="pill good">✓ create</span>`;
    }
    function delBadge(item) {
      if (item.create === false || !item.del) return '';
      const d = DEL_LABEL[item.del];
      return `<span class="pill ${d.cls}">${d.text}</span>`;
    }

    function rowsFor(items, emptyMsg) {
      if (!items.length) return `<div class="am-empty">${emptyMsg}</div>`;
      return items.map((it) => `
        <div class="am-row ${it.create === false ? 'denied' : ''}">
          <div class="am-name">
            ${it.name}
            ${it.appoint ? `<span class="am-scope">appoint: ${it.appoint}</span>` : ''}
            ${it.delNote ? `<span class="am-scope">${it.delNote}</span>` : ''}
            ${it.note ? `<span class="am-scope alt">${it.note}</span>` : ''}
          </div>
          <div class="am-badges">${createBadge(it)} ${delBadge(it)}</div>
        </div>
      `).join('');
    }

    function mount() {
      setChip();
      const a = AUTHORITY[state.actor];
      root.innerHTML = `
        <div class="am-wrap">
          <div class="page-head">
            <h1>My authority</h1>
            <div class="sub">Everything this role may create or delete — gated by <strong>scope</strong>, not by who created the record. Switch role to compare.</div>
          </div>

          <div class="actor-switcher">
            ${Object.keys(ACTORS).map((k) => `<button class="${state.actor === k ? 'active' : ''}" data-actor="${k}">${ACTORS[k].name}</button>`).join('')}
          </div>

          <div class="legend">
            <span><span class="pill warn">block-if-non-empty</span> geographic entities — history never destroyed</span>
            <span><span class="pill warn">soft-retire</span> Sabha Kind — drains, never hard-deleted</span>
            <span><span class="pill muted">revoke assignment</span> role-holder — Person persists, structures inherited</span>
          </div>

          <div class="am-cols">
            <div class="am-card">
              <div class="am-head">Structures I can create / delete</div>
              ${rowsFor(a.structures, 'No structural-creation authority at this role.')}
            </div>
            <div class="am-card">
              <div class="am-head">Roles I can appoint / revoke</div>
              ${rowsFor(a.roles, 'No appointment authority at this role.')}
            </div>
          </div>

          ${a.proxy ? `
            <div class="am-card proxy">
              <div class="am-head">Operational / proxy powers (no create or delete)</div>
              ${a.proxy.map((p) => `<div class="am-row"><div class="am-name">${p}</div><div class="am-badges"><span class="pill good">allowed</span></div></div>`).join('')}
              <div class="proxy-note">Sah-Nirdeshak holds <strong>no appointment, creation, or deletion authority</strong> ("for now").</div>
            </div>
          ` : ''}
        </div>
      `;
      root.querySelectorAll('.actor-switcher button').forEach((b) => {
        b.addEventListener('click', () => { state.actor = b.dataset.actor; mount(); });
      });
    }
    mount();
  }

  window.__VARIANTS = [{ key: 'main', name: 'Authority matrix', render }];
})();
