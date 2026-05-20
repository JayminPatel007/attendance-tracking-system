// PROTOTYPE — Mobile · Add Walk-in (ADR-0007)

(function () {
  // Wider Directory beyond the Sabha's Roster.
  const DIRECTORY = [
    { id: 'd1', initials: 'MG', name: 'Mihir Gajjar', mobile: '98201 89898', home: 'Yuvak Sabha · Borivali-3' },
    { id: 'd2', initials: 'PV', name: 'Pranav Vyas', mobile: '98202 11111', home: 'Yuvak Sabha · Vile Parle-1' },
    { id: 'd3', initials: 'AS', name: 'Aakash Sheth', mobile: '98202 22222', home: 'Yuvak Sabha · Dadar-2' },
    { id: 'd4', initials: 'RM', name: 'Ravi Mehta', mobile: '98201 11122', home: 'Yuvak Sabha · Andheri-7', rosterMember: true },
    { id: 'd5', initials: 'JT', name: 'Jaymin Thakkar', mobile: '98202 33333', home: 'Yuvak Sabha · Kandivali-4' },
    { id: 'd6', initials: 'KR', name: 'Kunal Raval', mobile: '98202 44444', home: 'Yuvak Sabha · Goregaon-1' },
    { id: 'd7', initials: 'HP', name: 'Hetav Parikh', mobile: '98202 55555', home: 'Yuvak Sabha · Malad-3' },
    { id: 'd8', initials: 'TS', name: 'Tirth Solanki', mobile: '98202 66666', home: 'Yuvak Sabha · Borivali-3' },
  ];

  // Cached Roster (subset accessible offline)
  const ROSTER_IDS = new Set(['d4']);

  let online = true;
  let query = '';
  let confirmFor = null; // person id pending confirmation

  function render(root) {
    function visible() {
      let list = online ? DIRECTORY : DIRECTORY.filter((p) => ROSTER_IDS.has(p.id));
      const q = query.trim().toLowerCase();
      if (q) list = list.filter((p) => p.name.toLowerCase().includes(q) || p.mobile.includes(q));
      return list;
    }

    function rowHtml(p) {
      const isRoster = p.rosterMember || ROSTER_IDS.has(p.id);
      return `
        <div class="wi-row ${isRoster ? 'roster-member' : 'away'}" data-id="${p.id}">
          <div class="avatar">${p.initials}</div>
          <div class="meta">
            <div class="name">${p.name}</div>
            <div class="home"><strong>${p.home}</strong> · ${p.mobile}</div>
          </div>
          <span class="arrow">›</span>
        </div>
      `;
    }

    function confirmHtml() {
      const p = DIRECTORY.find((x) => x.id === confirmFor);
      if (!p) return '';
      const isRoster = p.rosterMember || ROSTER_IDS.has(p.id);
      const lead = isRoster
        ? 'This Person\'s Home Sabha is the current Sabha. Marking as Present (not Walk-in).'
        : `This Person's Home Sabha is <strong>${p.home}</strong>. Marking as a Walk-in here.`;
      return `
        <div class="wi-confirm-backdrop">
          <div class="wi-confirm">
            <h2>Confirm ${isRoster ? 'attendance' : 'Walk-in'}</h2>
            <div class="person">
              <div class="avatar lg">${p.initials}</div>
              <div>
                <div class="name-lg">${p.name}</div>
                <div class="small muted">${p.mobile}</div>
              </div>
            </div>
            <div class="meta-row"><span class="k">Home Sabha</span><span>${p.home}</span></div>
            <div class="meta-row"><span class="k">Marking at</span><span>Yuvak Sabha · Andheri-7</span></div>
            <div class="meta-row"><span class="k">Occurrence</span><span>Sun, 24 May 7:00pm</span></div>
            <div class="note">${lead}</div>
            <div class="actions">
              <button class="btn" style="flex:1;" data-act="cancel">Cancel</button>
              <button class="btn primary" style="flex:1;" data-act="confirm">Mark ${isRoster ? 'Present' : 'Walk-in'}</button>
            </div>
          </div>
        </div>
      `;
    }

    function mount() {
      const list = visible();
      const scopeMsg = online
        ? 'Searching the full Directory.'
        : 'Offline — searching cached Roster only. Add Walk-ins from outside the Roster after reconnecting.';
      const showRosterFirst = !query && online;
      const roster = showRosterFirst ? list.filter((p) => p.rosterMember || ROSTER_IDS.has(p.id)) : [];
      const away = showRosterFirst ? list.filter((p) => !(p.rosterMember || ROSTER_IDS.has(p.id))) : list;

      root.innerHTML = `
        <div class="wi-page" style="position:relative;">
          <div class="appbar">
            <h1>← Add Walk-in</h1>
            <div class="sub">Yuvak Sabha · Kshetra Andheri-7 · 24 May</div>
          </div>
          <div class="wi-scope-strip ${online ? '' : 'warn'}">${scopeMsg}</div>
          <div class="wi-search-wrap">
            <input id="wi-q" placeholder="Search by name or mobile" value="${query}" />
            <div class="wi-hint">Walk-ins are People already in the Directory. <a href="../mobile-add-person/" style="color:var(--accent);">Add new Person →</a></div>
          </div>
          <div class="wi-list">
            ${list.length === 0
              ? `<div class="wi-empty">No People match "${query}".<br /><a class="add-link" href="../mobile-add-person/">+ Add a new Person to the Directory</a></div>`
              : showRosterFirst
                ? `${roster.length ? `<div class="wi-section">From this Sabha's Roster (${roster.length})</div>${roster.map(rowHtml).join('')}` : ''}
                   ${away.length ? `<div class="wi-section">From elsewhere in the Directory (${away.length})</div>${away.map(rowHtml).join('')}` : ''}`
                : list.map(rowHtml).join('')}
          </div>
          ${confirmFor ? confirmHtml() : ''}
        </div>
      `;
      attach();
    }

    function attach() {
      const q = root.querySelector('#wi-q');
      if (q) {
        q.addEventListener('input', (e) => {
          query = e.target.value;
          mount();
          root.querySelector('#wi-q').focus();
        });
      }
      root.querySelectorAll('.wi-row').forEach((row) => {
        row.addEventListener('click', () => {
          confirmFor = row.dataset.id;
          mount();
        });
      });
      root.querySelectorAll('[data-act]').forEach((b) => {
        b.addEventListener('click', () => {
          const a = b.dataset.act;
          if (a === 'cancel') {
            confirmFor = null;
            mount();
          } else if (a === 'confirm') {
            const p = DIRECTORY.find((x) => x.id === confirmFor);
            alert(`Queued: ${p.rosterMember || ROSTER_IDS.has(p.id) ? 'Present' : 'Walk-in'} for ${p.name}.`);
            confirmFor = null;
            mount();
          }
        });
      });
    }
    mount();
  }

  window.__VARIANTS = [{ key: 'main', name: 'Add Walk-in', render }];
})();
