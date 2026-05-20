// PROTOTYPE — Web · Nirikshak Sanchalak-proxy mode
// On any assigned Sabha, Nirikshak can act for Sanchalak. Banner + audit make it visible.

(function () {
  const SABHAS = [
    { id: 's1', name: 'Yuvak Sabha · Andheri-7', schedule: 'Sun 7:00–8:30pm', sanchalak: 'Pratik Patel', sanchalakStatus: 'online', lastSeen: 'last opened the app 2h ago', occState: 'Open for Marking', rosterCount: 18 },
    { id: 's2', name: 'Yuvak Sabha · Andheri-3', schedule: 'Sat 6:30–8:00pm', sanchalak: 'Hetav Suthar', sanchalakStatus: 'away', lastSeen: 'last opened the app yesterday', occState: 'Scheduled', rosterCount: 22 },
    { id: 's3', name: 'BSS Yuvak Sabha · Andheri-7', schedule: '1st Sun monthly', sanchalak: 'Ashish Mehta', sanchalakStatus: 'online', lastSeen: 'currently marking attendance', occState: 'Open for Marking', rosterCount: 8 },
    { id: 's4', name: 'Yuvak Sabha · Andheri-9', schedule: 'Sun 7:30–9:00pm', sanchalak: 'Nikhil Joshi', sanchalakStatus: 'absent', lastSeen: 'no activity in 6 days', occState: 'Scheduled', rosterCount: 14 },
  ];

  let proxyFor = null; // sabha id

  function render(root) {
    function intro() {
      return `
        <div class="proxy-intro">
          <div class="icon">⌥</div>
          <div>
            <h2>Sanchalak-proxy mode</h2>
            <p>When a Sanchalak is unavailable, you can act on their Sabha — mark attendance, manage the Occurrence, register Walk-ins, add People. <strong>All actions are recorded under your name</strong> with the proxy context, not the Sanchalak's.</p>
          </div>
        </div>
      `;
    }

    function sabhaCardsHtml() {
      return `
        <div class="sabha-grid">
          ${SABHAS.map((s) => `
            <div class="sabha-card">
              <h3>${s.name}</h3>
              <div class="schedule">${s.schedule}</div>
              <div class="meta-row"><span class="k">Sanchalak</span><span>${s.sanchalak}</span></div>
              <div class="meta-row"><span class="k">Availability</span>
                <span class="availability ${s.sanchalakStatus}">${s.sanchalakStatus === 'online' ? '● Online' : s.sanchalakStatus === 'away' ? '◐ Away' : '○ Absent'}</span>
              </div>
              <div class="meta-row"><span class="k">Last activity</span><span>${s.lastSeen}</span></div>
              <div class="meta-row"><span class="k">Today's Occurrence</span><span>${s.occState}</span></div>
              <div class="actions">
                <button class="btn ${s.sanchalakStatus === 'absent' ? 'primary' : ''}" style="width:100%;" data-act="enter-proxy" data-sid="${s.id}">
                  ${s.sanchalakStatus === 'absent' ? 'Act as Sanchalak now →' : 'Enter proxy mode'}
                </button>
              </div>
            </div>
          `).join('')}
        </div>
      `;
    }

    function activeProxyHtml() {
      const s = SABHAS.find((x) => x.id === proxyFor);
      return `
        <div class="proxy-banner">
          <div class="icon">⚠</div>
          <div class="info">
            <h2>You are acting as Sanchalak for ${s.name}</h2>
            <div class="sub">All actions on this Sabha will be audit-logged as <strong>Bhavin Mehta (Nirikshak, proxy for ${s.sanchalak})</strong>.</div>
          </div>
          <button data-act="exit-proxy">↶ Exit proxy mode</button>
        </div>

        <div class="page-head" style="margin-bottom:14px;">
          <h1 style="font-size:18px;margin:0 0 4px;">Sanchalak toolkit · ${s.name}</h1>
          <div class="sub small muted">Full Sanchalak operational authority on this Sabha (per CONTEXT.md).</div>
        </div>

        <div class="proxy-toolkit">
          <a class="toolkit-card" href="../mobile-attendance/" target="_blank">
            <div class="icon-lg">☑</div>
            <h3>Mark / edit attendance</h3>
            <p>Open the current Occurrence and toggle Present / Absent for the Roster.</p>
          </a>
          <a class="toolkit-card" href="../mobile-walk-in/" target="_blank">
            <div class="icon-lg">+</div>
            <h3>Add Walk-in</h3>
            <p>Mark a Person from elsewhere in the Directory as present at this Sabha.</p>
          </a>
          <a class="toolkit-card" href="../mobile-occurrence-control/" target="_blank">
            <div class="icon-lg">⋯</div>
            <h3>Manage Occurrence</h3>
            <p>Cancel, reschedule, or override the venue for today's Occurrence.</p>
          </a>
          <a class="toolkit-card" href="../mobile-add-person/" target="_blank">
            <div class="icon-lg">⊕</div>
            <h3>Add Person to Directory</h3>
            <p>Onboard a new Person and set their Home Sabha.</p>
          </a>
          <a class="toolkit-card" href="../mobile-home-sabha-transfer/" target="_blank">
            <div class="icon-lg">⇄</div>
            <h3>Home Sabha Transfer</h3>
            <p>Move a Person's Home Sabha here, with OTP verification.</p>
          </a>
          <a class="toolkit-card" href="../web-sabha-definition/" target="_blank">
            <div class="icon-lg">⚙</div>
            <h3>Change Sabha schedule</h3>
            <p>Move the standing weekly slot or update the venue. Sabha-shaping action.</p>
          </a>
        </div>

        <div class="audit-preview">
          <h3>What gets recorded</h3>
          <div class="entry">[24 May 7:02pm] Bhavin Mehta (Nirikshak, proxy for ${s.sanchalak}) — entered proxy mode</div>
          <div class="entry">[24 May 7:04pm] Bhavin Mehta (Nirikshak, proxy for ${s.sanchalak}) — marked Ravi Mehta · Present</div>
          <div class="entry">[24 May 7:11pm] Bhavin Mehta (Nirikshak, proxy for ${s.sanchalak}) — added Walk-in · Mihir Gajjar</div>
          <div class="entry" style="margin-top:6px;color:var(--ink-faint);">…actions persist in the Sabha's audit trail. ${s.sanchalak} sees them on next sign-in.</div>
        </div>
      `;
    }

    function mount() {
      if (proxyFor) {
        root.innerHTML = activeProxyHtml();
      } else {
        root.innerHTML = `
          ${intro()}
          <div class="page-head" style="margin-bottom:14px;">
            <h1 style="font-size:18px;margin:0 0 4px;">Sabhas in your scope</h1>
            <div class="sub small muted">Pick the Sabha you need to act for. Availability is a hint, not a gate.</div>
          </div>
          ${sabhaCardsHtml()}
        `;
      }
      attach();
    }

    function attach() {
      root.querySelectorAll('[data-act]').forEach((b) => {
        b.addEventListener('click', () => {
          const a = b.dataset.act;
          if (a === 'enter-proxy') { proxyFor = b.dataset.sid; mount(); window.scrollTo(0, 0); }
          else if (a === 'exit-proxy') { proxyFor = null; mount(); }
        });
      });
    }
    mount();
  }

  window.__VARIANTS = [{ key: 'main', name: 'Sanchalak-proxy', render }];
})();
