// PROTOTYPE — Mobile · Sync status & pending queue (ADR-0007)

(function () {
  const queue = [
    { id: 'q1', kind: 'attendance', title: 'Attendance · Ravi Mehta → Present', ctx: 'Yuvak Sabha · 24 May · 7:42pm', status: 'pending' },
    { id: 'q2', kind: 'attendance', title: 'Attendance · Harsh Joshi → Absent', ctx: 'Yuvak Sabha · 24 May · 7:43pm', status: 'pending' },
    { id: 'q3', kind: 'walkin', title: 'Walk-in · Mihir Gajjar', ctx: 'Yuvak Sabha · 24 May · 7:45pm · Home: Borivali-3', status: 'pending' },
    { id: 'q4', kind: 'attendance', title: 'Attendance · Devansh Desai → Present', ctx: 'Yuvak Sabha · 24 May · 7:46pm', status: 'failed', error: 'Roster stale at server — refresh required' },
    { id: 'q5', kind: 'attendance', title: 'Attendance batch (12 People)', ctx: 'Yuvak Sabha · 17 May · 8:30pm', status: 'synced', syncedAt: '17 May 8:32pm' },
  ];

  let online = true;
  let rosterStale = 0; // days
  let syncing = false;

  function iconFor(kind) {
    if (kind === 'walkin') return '+';
    if (kind === 'attendance') return '✓';
    return '·';
  }

  function pendingCount() { return queue.filter((q) => q.status === 'pending').length; }
  function failedCount() { return queue.filter((q) => q.status === 'failed').length; }

  function render(root) {
    function mount() {
      const pending = pendingCount();
      const failed = failedCount();
      const rosterTone = rosterStale === 0 ? '' : rosterStale < 7 ? 'warn' : 'bad';
      const rosterMsg = rosterStale === 0
        ? 'Roster fresh — last refreshed just now.'
        : rosterStale >= 7
          ? `Roster is ${rosterStale} days stale — marking is blocked until you refresh.`
          : `Roster is ${rosterStale} days old. Refresh recommended (block at 7 days).`;

      root.innerHTML = `
        <div class="sync-page">
          <div class="appbar">
            <h1>Sync &amp; offline</h1>
            <div class="sub">Yuvak Sabha · Kshetra Andheri-7</div>
          </div>
          <div class="sync-body">
            <div class="online-card ${online ? '' : 'offline'}">
              <span class="indicator"></span>
              <div class="info">
                <div class="state">${online ? 'Online' : 'Offline'}</div>
                <div class="meta">${online ? 'Connected. Pending actions will sync automatically.' : 'You can still mark attendance and add Walk-ins. Changes will sync when you reconnect.'}</div>
              </div>
              <button class="btn ghost" data-act="toggle-net" title="Toggle for testing">${online ? '⇄ Go offline' : '⇄ Go online'}</button>
            </div>

            <div class="sync-stat-row">
              <div class="sync-stat">
                <div class="label">Pending</div>
                <div class="value">${pending}</div>
                <div class="sub">queued for sync</div>
              </div>
              <div class="sync-stat">
                <div class="label">Last sync</div>
                <div class="value">${syncing ? '⏳' : '3 min'}</div>
                <div class="sub">${syncing ? 'syncing…' : 'ago · 24 May 7:39pm'}</div>
              </div>
            </div>

            <div class="roster-banner ${rosterTone}">${rosterMsg}</div>

            <div class="section-heading">
              <span>Pending (${pending})</span>
              ${failed ? `<span style="color:var(--bad);font-weight:600;">${failed} failed</span>` : ''}
            </div>
            ${queue.filter((q) => q.status !== 'synced').map(itemHtml).join('') || '<div class="muted small" style="padding:14px;text-align:center;">No pending actions. ✓</div>'}

            <div class="section-heading">
              <span>Recently synced</span>
            </div>
            ${queue.filter((q) => q.status === 'synced').map(itemHtml).join('')}
          </div>
          <div class="btn-row">
            <button class="btn primary" style="width:100%;" data-act="sync" ${online && pending > 0 && !syncing ? '' : 'disabled'}>
              ${syncing ? 'Syncing…' : pending > 0 ? `Sync now · ${pending} pending` : 'All up to date'}
            </button>
            <div class="xs muted" style="text-align:center;margin-top:6px;">Sync happens automatically when online. Manual sync is for nudging.</div>
          </div>
        </div>
      `;
      attach();
    }

    function itemHtml(q) {
      const status =
        q.status === 'pending' ? '<span class="qi-status pending">queued</span>'
        : q.status === 'failed' ? '<span class="qi-status failed">failed</span>'
        : '<span class="qi-status synced">synced</span>';
      return `
        <div class="queue-item ${q.status}" data-id="${q.id}">
          <div class="qi-icon">${iconFor(q.kind)}</div>
          <div class="qi-body">
            <div class="qi-title">${q.title}</div>
            <div class="qi-sub">${q.ctx}</div>
            ${q.status === 'failed' ? `<div class="qi-sub" style="color:var(--bad);">${q.error}</div>` : ''}
            ${q.status === 'synced' ? `<div class="qi-sub">Synced ${q.syncedAt}</div>` : ''}
            ${q.status === 'failed' ? `<div class="qi-actions" data-act="retry" data-qid="${q.id}">↻ Retry</div>` : ''}
          </div>
          ${status}
        </div>
      `;
    }

    function attach() {
      root.querySelectorAll('[data-act]').forEach((el) => {
        el.addEventListener('click', () => {
          const a = el.dataset.act;
          if (a === 'toggle-net') {
            online = !online;
            mount();
          } else if (a === 'sync') {
            syncing = true;
            mount();
            setTimeout(() => {
              queue.forEach((q) => {
                if (q.status === 'pending') {
                  q.status = 'synced';
                  q.syncedAt = 'just now';
                }
              });
              syncing = false;
              mount();
            }, 900);
          } else if (a === 'retry') {
            const item = queue.find((q) => q.id === el.dataset.qid);
            if (item) {
              item.status = 'pending';
              delete item.error;
              mount();
            }
          }
        });
      });
    }
    mount();
  }

  window.__VARIANTS = [{ key: 'main', name: 'Sync status', render }];
})();
