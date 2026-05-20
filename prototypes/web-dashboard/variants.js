// PROTOTYPE — Web · Per-tier Dashboard · three variants
// Question: what mental model do Nirdeshak / Sanyojak / Sant actually carry?

(function () {
  const D = window.__DATA;
  const candidates = D.roster.filter((p) => p.candidate);

  // Tiny inline SVG sparkline
  function sparkline(rates, width = 320, height = 60) {
    const max = 100;
    const min = 50;
    const step = width / (rates.length - 1);
    const pts = rates
      .map((r, i) => `${i * step},${height - ((r - min) / (max - min)) * height}`)
      .join(' ');
    return `
      <svg viewBox="0 0 ${width} ${height}" width="100%" height="${height}" preserveAspectRatio="none">
        <polyline points="${pts}" fill="none" stroke="#3b3b8a" stroke-width="2"/>
        ${rates
          .map((r, i) => `<circle cx="${i * step}" cy="${height - ((r - min) / (max - min)) * height}" r="2.5" fill="#3b3b8a" />`)
          .join('')}
      </svg>
    `;
  }

  function syncSidebar(key) {
    document.querySelectorAll('#sidenav .nav-item').forEach((el) => {
      el.classList.toggle('active', el.dataset.section === key);
    });
  }

  // ====== Section A — Dashboard overview (KPI + card grid) ======
  function renderA(root) {
    syncSidebar('A');
    root.innerHTML = `
      <div class="va">
        <div class="page-head">
          <h1>Dashboard overview</h1>
          <div class="sub">Yuvak Sabha · Kshetra Andheri-7 · last 8 weeks</div>
        </div>

        <div class="kpis">
          <div class="kpi">
            <div class="label">Attendance rate · last week</div>
            <div class="value">65%</div>
            <div class="delta bad">▼ 6 pp vs 4-week avg</div>
          </div>
          <div class="kpi">
            <div class="label">Re-engagement candidates</div>
            <div class="value">${candidates.length}</div>
            <div class="delta bad">+1 this week</div>
          </div>
          <div class="kpi">
            <div class="label">Sabhas in scope</div>
            <div class="value">4</div>
            <div class="delta">Regular + BSS</div>
          </div>
          <div class="kpi">
            <div class="label">Open Occurrences awaiting finalize</div>
            <div class="value">1</div>
            <div class="delta">past 24h grace</div>
          </div>
        </div>

        <div class="grid">
          <div class="card">
            <div class="head">
              <div class="title">Re-engagement candidates</div>
              <a class="link" href="#">View all (${candidates.length}) →</a>
            </div>
            ${candidates
              .map(
                (p) => `
              <div class="candidate-row">
                <div class="avatar">${p.initials}</div>
                <div class="meta">
                  <div>${p.name}</div>
                  <div class="sub">Home: ${D.sabha.name} · ${D.sabha.kshetra}</div>
                </div>
                <div class="missed">missed ${p.lastMissed}</div>
                <button class="btn">Follow up</button>
              </div>
            `,
              )
              .join('')}
          </div>

          <div class="stack-cards">
            <div class="card" style="padding:16px 18px;">
              <div class="head">
                <div class="title">Attendance — last 8 weeks</div>
                <a class="link" href="#">Drill →</a>
              </div>
              <div class="sparkline">${sparkline(D.weeklyRate, 320, 80)}</div>
              <div class="xs muted">${D.weeks.join(' · ')}</div>
            </div>

            <div class="card" style="padding:16px 18px;">
              <div class="head">
                <div class="title">Sabhas at risk</div>
                <a class="link" href="#">3 →</a>
              </div>
              <div class="small">
                <div style="display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid var(--line-soft);">
                  <span>Yuvak · Borivali-3</span><span class="pill bad">69%</span>
                </div>
                <div style="display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid var(--line-soft);">
                  <span>Sanyukta · Andheri-7</span><span class="pill warn">66%</span>
                </div>
                <div style="display:flex;justify-content:space-between;padding:6px 0;">
                  <span>Yuvak · Andheri-7</span><span class="pill warn">75%</span>
                </div>
              </div>
            </div>

            <div class="card" style="padding:16px 18px;">
              <div class="head">
                <div class="title">Recent Occurrences</div>
              </div>
              <div class="small">
                <div style="display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid var(--line-soft);">
                  <span>17 May · Yuvak · Andheri-7</span><span class="pill muted">Finalized</span>
                </div>
                <div style="display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid var(--line-soft);">
                  <span>17 May · Yuvak · Borivali-3</span><span class="pill good">Reopened</span>
                </div>
                <div style="display:flex;justify-content:space-between;padding:6px 0;">
                  <span>10 May · Sanyukta · Andheri-7</span><span class="pill muted">Finalized</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  // ====== Section B — People analytics (filterable Directory table) ======
  function renderB(root) {
    syncSidebar('B');
    function dotsFor(p) {
      const out = [];
      for (let i = 0; i < 8; i++) {
        const distFromRight = 7 - i;
        out.push(distFromRight < p.lastMissed ? 'absent' : 'present');
      }
      return out;
    }

    const rows = D.roster
      .slice()
      .sort((a, b) => b.lastMissed - a.lastMissed)
      .map((p) => {
        const dots = dotsFor(p);
        return `
          <tr>
            <td>
              <div style="display:flex;align-items:center;gap:8px;">
                <div class="avatar" style="width:28px;height:28px;font-size:11px;">${p.initials}</div>
                <div>
                  <div>${p.name}</div>
                  <div class="xs muted">${p.mobile}</div>
                </div>
              </div>
            </td>
            <td><span class="small">${D.sabha.name} · ${D.sabha.kshetra}</span></td>
            <td>
              <div class="week-dots">${dots.map((d) => `<span class="dot ${d}" title="${d}"></span>`).join('')}</div>
            </td>
            <td>${
              p.candidate
                ? `<span class="pill bad">candidate · ${p.lastMissed}</span>`
                : p.lastMissed > 0
                  ? `<span class="pill warn">missed ${p.lastMissed}</span>`
                  : `<span class="pill good">attending</span>`
            }</td>
            <td><a class="small" href="#" style="color:var(--accent);text-decoration:none;">Follow up</a></td>
          </tr>
        `;
      })
      .join('');

    root.innerHTML = `
      <div class="vb-wrap">
        <div class="page-head">
          <h1>People analytics</h1>
          <div class="sub">Directory-level view · sortable, filterable, exportable. Re-engagement candidates surfaced inline.</div>
        </div>
      <div class="vb">
        <aside class="filters">
          <div class="filter-group">
            <div class="filter-label">Sabha kind</div>
            <label><input type="checkbox" checked /> Yuvak (Regular)</label>
            <label><input type="checkbox" checked /> Yuvak (BSS)</label>
            <label><input type="checkbox" /> Sanyukta</label>
          </div>
          <div class="filter-group">
            <div class="filter-label">Kshetra</div>
            <label><input type="checkbox" checked /> Andheri-7</label>
            <label><input type="checkbox" /> Borivali-3</label>
            <label><input type="checkbox" /> Vile Parle-1</label>
          </div>
          <div class="filter-group">
            <div class="filter-label">Status</div>
            <label><input type="checkbox" /> Re-engagement candidates only</label>
            <label><input type="checkbox" /> Walk-ins only</label>
          </div>
          <div class="filter-group">
            <div class="filter-label">Date range</div>
            <select><option>Last 8 weeks</option><option>Last 4 weeks</option><option>This quarter</option></select>
          </div>
          <div class="filter-group">
            <div class="filter-label">Search Person</div>
            <input placeholder="name or mobile" />
          </div>
        </aside>

        <section>
          <div class="table-card">
            <div class="table-head">
              <h3>People · ${D.roster.length} matching filters · sorted by missed</h3>
              <div class="small">
                <span class="pill bad">${candidates.length} candidate</span>
                <button class="btn" style="margin-left:8px;">Export CSV</button>
              </div>
            </div>
            <table>
              <thead>
                <tr>
                  <th>Person ↓</th>
                  <th>Home Sabha</th>
                  <th>Last 8 weeks</th>
                  <th>Status ↓</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>${rows}</tbody>
            </table>
          </div>
        </section>
      </div>
      </div>
    `;
  }

  // ====== Section C — Sabha analytics (hierarchical tree explorer) ======
  function renderC(root) {
    syncSidebar('C');
    let selected = { path: ['Mumbai West', 'Andheri-7', 'Yuvak Sabha'], node: null };

    function findNode(path) {
      let nodes = D.tree;
      let node = null;
      for (const seg of path) {
        node = nodes.find((n) => n.name === seg);
        if (!node) break;
        nodes = node.children || [];
      }
      return node;
    }

    function renderTreeNode(node, path) {
      const pathHere = [...path, node.name];
      const isSel = pathHere.join('/') === selected.path.join('/');
      const hasChildren = (node.children || []).length > 0;
      return `
        <div>
          <div class="tree-node ${isSel ? 'selected' : ''}" data-path="${pathHere.join('|')}">
            <span class="caret">${hasChildren ? '▾' : ''}</span>
            <span class="name">${node.name}</span>
            <span class="type">${node.type}${node.kind ? ' · ' + node.kind : ''}</span>
            ${node.candidates > 0 ? `<span class="badge">${node.candidates}</span>` : ''}
          </div>
          ${
            hasChildren
              ? `<div class="tree-children">${node.children.map((c) => renderTreeNode(c, pathHere)).join('')}</div>`
              : ''
          }
        </div>
      `;
    }

    function renderDetail() {
      const node = findNode(selected.path);
      if (!node) return '<div class="muted">Pick something from the tree.</div>';
      const crumb = selected.path.join(' › ');
      const candList =
        node.type === 'Sabha'
          ? candidates.slice(0, node.candidates || 0)
          : candidates;
      return `
        <div class="crumb">${crumb}</div>
        <h2>${node.name} <span class="muted small">· ${node.type}${node.kind ? ' · ' + node.kind : ''}</span></h2>
        <div class="stats">
          <div class="stat"><div class="label">Attendance · last 8 wks</div><div class="value">${Math.round(node.attendance * 100)}%</div></div>
          <div class="stat"><div class="label">Re-engagement candidates</div><div class="value">${node.candidates}</div></div>
          <div class="stat"><div class="label">Occurrences this year</div><div class="value">${node.occurrences ?? '—'}</div></div>
        </div>
        <h3 class="section">Attendance trend</h3>
        ${sparkline(D.weeklyRate, 700, 100)}
        <h3 class="section">Re-engagement candidates here (${candList.length})</h3>
        <div class="candidate-list">
          ${
            candList
              .map(
                (p) => `
              <div class="row">
                <div class="avatar">${p.initials}</div>
                <div class="meta">
                  <div>${p.name}</div>
                  <div class="sub">${p.mobile} · missed ${p.lastMissed} consecutive</div>
                </div>
                <button class="btn">Follow up</button>
              </div>
            `,
              )
              .join('') ||
            '<div class="muted small">No candidates at this scope.</div>'
          }
        </div>
      `;
    }

    function mount() {
      root.innerHTML = `
        <div class="vc-wrap">
          <div class="page-head">
            <h1>Sabha analytics</h1>
            <div class="sub">Drill from Zone → Kshetra → Sabha. Re-engagement counts roll up at every level.</div>
          </div>
          <div class="vc">
            <div class="tree">${D.tree.map((n) => renderTreeNode(n, [])).join('')}</div>
            <div class="detail">${renderDetail()}</div>
          </div>
        </div>
      `;
      root.querySelectorAll('.tree-node').forEach((el) => {
        el.addEventListener('click', () => {
          selected.path = el.dataset.path.split('|');
          mount();
        });
      });
    }

    mount();
  }

  window.__VARIANTS = [
    { key: 'A', name: 'Dashboard overview', render: renderA },
    { key: 'B', name: 'People analytics', render: renderB },
    { key: 'C', name: 'Sabha analytics', render: renderC },
  ];
})();
