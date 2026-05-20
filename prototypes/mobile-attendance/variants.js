// PROTOTYPE — Mobile · Attendance Marking · chosen design
// Tap-toggle list + search field + filter chips.

(function () {
  const D = window.__DATA;

  function appbar() {
    return `
      <div class="appbar">
        <h1>${D.sabha.name} · ${D.sabha.kshetra}</h1>
        <div class="sub">${D.occurrence.date} · ${D.occurrence.state}</div>
      </div>
      <div class="sync-strip good">
        <span>● Online · synced ${D.sync.lastSyncAgo} ago</span>
        <span>Roster ${D.sync.rosterStaleDays === 0 ? 'fresh' : `${D.sync.rosterStaleDays}d old`}</span>
      </div>
    `;
  }

  function render(root) {
    const state = {};
    D.roster.slice(0, 5).forEach((p) => (state[p.id] = 'present'));
    state['p3'] = 'absent';
    let query = '';
    let filter = 'all';

    function visible() {
      let list = D.roster.slice();
      const q = query.trim().toLowerCase();
      if (q) list = list.filter((p) => p.name.toLowerCase().includes(q) || p.mobile.includes(q));
      if (filter === 'unmarked') list = list.filter((p) => !state[p.id]);
      else if (filter === 'present') list = list.filter((p) => state[p.id] === 'present');
      else if (filter === 'absent') list = list.filter((p) => state[p.id] === 'absent');
      else if (filter === 'candidates') list = list.filter((p) => p.candidate);
      return list;
    }

    function counts() {
      const present = Object.values(state).filter((x) => x === 'present').length;
      const absent = Object.values(state).filter((x) => x === 'absent').length;
      const unmarked = D.roster.length - present - absent;
      return { present, absent, unmarked };
    }

    function rowHtml(p) {
      const s = state[p.id];
      const flags = [];
      if (p.walkIn) flags.push('<span class="pill warn xs">Walk-in</span>');
      if (p.candidate) flags.push('<span class="pill bad xs">candidate</span>');
      const sub = flags.length ? flags.join(' ') + ` · missed ${p.lastMissed}` : `${p.mobile}`;
      const toggle = s === 'present' ? 'P' : s === 'absent' ? 'A' : '—';
      return `
        <div class="row" data-id="${p.id}" data-state="${s || ''}">
          <div class="avatar">${p.initials}</div>
          <div class="meta">
            <div class="name">${p.name}</div>
            <div class="sub">${sub}</div>
          </div>
          <div class="toggle">${toggle}</div>
        </div>
      `;
    }

    function chipLabels() {
      const c = counts();
      return [
        `All · ${D.roster.length}`,
        `Unmarked · ${c.unmarked}`,
        `Present · ${c.present}`,
        `Absent · ${c.absent}`,
        `Candidates · ${D.roster.filter((p) => p.candidate).length}`,
      ];
    }

    function mount() {
      const c = counts();
      const list = visible();
      const chipKeys = ['all', 'unmarked', 'present', 'absent', 'candidates'];
      const labels = chipLabels();
      root.innerHTML = `
        <div class="vd">
          ${appbar()}
          <div class="search-wrap">
            <input id="vd-q" placeholder="Search Roster by name or mobile" value="${query}" />
          </div>
          <div class="filter-chips">
            ${chipKeys
              .map(
                (k, i) =>
                  `<div class="chip ${filter === k ? 'active' : ''}" data-filter="${k}">${labels[i]}</div>`,
              )
              .join('')}
          </div>
          <div class="roster">
            ${list.length ? list.map(rowHtml).join('') : '<div class="empty-state">No People match.</div>'}
          </div>
          <button class="fab" title="Add Walk-in">+</button>
          <div class="footer">
            <button class="btn" style="flex:1;">Cancel Occurrence</button>
            <button class="btn primary" style="flex:1;">Finalize · ${c.present}/${D.roster.length}</button>
          </div>
        </div>
      `;
      const input = root.querySelector('#vd-q');
      input.addEventListener('input', (e) => {
        query = e.target.value;
        renderListOnly();
      });
      root.querySelectorAll('.chip').forEach((el) => {
        el.addEventListener('click', () => {
          filter = el.dataset.filter;
          mount();
        });
      });
      attachRowHandlers();
    }

    function renderListOnly() {
      const list = visible();
      const roster = root.querySelector('.roster');
      roster.innerHTML = list.length
        ? list.map(rowHtml).join('')
        : '<div class="empty-state">No People match.</div>';
      attachRowHandlers();
    }

    function attachRowHandlers() {
      root.querySelectorAll('.row').forEach((row) => {
        row.addEventListener('click', () => {
          const id = row.dataset.id;
          const cur = state[id];
          const next = cur === 'present' ? 'absent' : cur === 'absent' ? undefined : 'present';
          state[id] = next;
          row.dataset.state = next || '';
          row.querySelector('.toggle').textContent =
            next === 'present' ? 'P' : next === 'absent' ? 'A' : '—';
          const c = counts();
          const finalize = root.querySelector('.footer .btn.primary');
          finalize.textContent = `Finalize · ${c.present}/${D.roster.length}`;
          const labels = chipLabels();
          root.querySelectorAll('.chip').forEach((ch, i) => (ch.textContent = labels[i]));
        });
      });
    }
    mount();
  }

  window.__VARIANTS = [{ key: 'main', name: 'Attendance marking', render }];
})();
