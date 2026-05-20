// PROTOTYPE — Mobile · Add Person · de-dup collision · chosen design
// Mobile-first entry; on match -> forced redirect to existing profile;
// on no match -> step 2 (name / DOB / Home Sabha) -> add to Directory.

(function () {
  const D = window.__DATA;
  const M = D.dedupMatch; // existing Person to collide with

  function normalize(n) {
    return (n || '').replace(/\D/g, '');
  }
  function isMatch(input) {
    return normalize(input) === normalize(M.mobile) && normalize(input).length === 10;
  }

  function appbar(title, back) {
    return `
      <div class="appbar">
        <h1>${back ? '← ' : ''}${title}</h1>
        <div class="sub">Online · Directory write requires connectivity</div>
      </div>
    `;
  }

  // Step 1: mobile entry  ->  match -> forced redirect to existing-Person profile
  //                       ->  no match -> step 2 (name / DOB / Home Sabha) -> add to Directory
  function render(root) {
    const state = {
      mobile: '',
      view: 'mobile', // 'mobile' | 'profile' | 'details'
      newPerson: { name: '', dob: '', homeSabha: 'Yuvak Sabha · Andheri-7' },
      mobileError: '',
    };

    function step1() {
      const digits = normalize(state.mobile).length;
      return `
        <div class="form-page">
          ${appbar('Add Person to Directory', true)}
          <div class="step-indicator">
            <span class="dot active"></span><span class="dot"></span>
            <span class="step-label">Step 1 of 2 · Mobile number</span>
          </div>
          <div class="form-body">
            <div class="field">
              <label>Mobile number</label>
              <input id="m" type="tel" placeholder="10-digit number" value="${state.mobile}" inputmode="numeric" maxlength="13" />
              <div class="hint">Checked against the Directory before continuing. Used as the Person's de-dup key (ADR-0013).</div>
              ${state.mobileError ? `<div class="error-hint">${state.mobileError}</div>` : ''}
            </div>
            <div class="hint-card">
              <strong>Why this first?</strong> If this number is already in the Directory, we open that Person's profile instead — preventing duplicates.
            </div>
          </div>
          <div class="btn-row">
            <button class="btn" style="flex:1;">Cancel</button>
            <button class="btn primary" style="flex:1;" data-act="next" ${digits === 10 ? '' : 'disabled'}>Continue →</button>
          </div>
        </div>
      `;
    }

    function step2Details() {
      const np = state.newPerson;
      const ready = np.name.trim().length > 0;
      return `
        <div class="form-page">
          ${appbar('Add Person to Directory', true)}
          <div class="step-indicator">
            <span class="dot done">✓</span><span class="dot active"></span>
            <span class="step-label">Step 2 of 2 · Details</span>
          </div>
          <div class="form-body">
            <div class="check-card">
              <span class="pill good">✓ No duplicate</span>
              <span>Mobile <strong>${state.mobile}</strong> is new to the Directory.</span>
            </div>
            <div class="field">
              <label>Full name <span class="required">*</span></label>
              <input id="np-name" value="${np.name}" placeholder="e.g., Jay Mehta" />
            </div>
            <div class="field">
              <label>Date of birth (optional, informational)</label>
              <input id="np-dob" type="date" value="${np.dob}" />
              <div class="hint">Stored when known. Eligibility for demographic Sabhas is the Sanchalak's call (not system-enforced).</div>
            </div>
            <div class="field">
              <label>Home Sabha <span class="required">*</span></label>
              <select id="np-hs">
                <option ${np.homeSabha.startsWith('Yuvak') ? 'selected' : ''}>Yuvak Sabha · Andheri-7</option>
                <option ${np.homeSabha.startsWith('Baal') ? 'selected' : ''}>Baal Sabha · Andheri-7</option>
                <option ${np.homeSabha.startsWith('Sanyukta') ? 'selected' : ''}>Sanyukta Sabha · Andheri-7</option>
              </select>
              <div class="hint">A Sanyukta Sabha Home is added automatically.</div>
            </div>
          </div>
          <div class="btn-row">
            <button class="btn" style="flex:1;" data-act="back">← Mobile</button>
            <button class="btn primary" style="flex:1;" data-act="add" ${ready ? '' : 'disabled'}>Add to Directory</button>
          </div>
        </div>
      `;
    }

    function profile() {
      return `
        <div class="form-page">
          ${appbar('Person already in Directory', true)}
          <div class="profile-banner">
            <span class="pill">Match</span>
            <span>This mobile is registered. Continuing means using <strong>${M.name}</strong>.</span>
          </div>
          <div class="profile-hero">
            <div class="avatar lg">${M.initials}</div>
            <div class="name">${M.name}</div>
            <div class="sub">DOB ${M.dob} · ${M.mobile}</div>
            <div class="profile-chips">
              <span class="pill muted">Active Roster member</span>
              <span class="pill good">Attended 17 May</span>
            </div>
          </div>
          <div class="profile-section">
            <h3>Home Sabhas</h3>
            <div class="kv">
              <div class="row"><span class="k">Demographic kind</span><span>${M.homeSabha}</span></div>
              <div class="row"><span class="k">Sanyukta</span><span>Sanyukta Sabha · Andheri-7</span></div>
            </div>
          </div>
          <div class="profile-section">
            <h3>Directory record</h3>
            <div class="kv">
              <div class="row"><span class="k">Added on</span><span>${M.addedOn}</span></div>
              <div class="row"><span class="k">Added by</span><span>${M.addedBy}</span></div>
              <div class="row"><span class="k">Last Sabha attended</span><span>17 May 2026</span></div>
            </div>
          </div>
          <div class="profile-section">
            <h3>Recent attendance</h3>
            <div class="attendance-dots">
              ${['W14', 'W15', 'W16', 'W17', 'W18', 'W19', 'W20', 'W21']
                .map((w, i) => `<div class="dot-week"><span class="dot ${i < 6 ? 'present' : ''}"></span><span class="wlabel">${w}</span></div>`)
                .join('')}
            </div>
          </div>
          <div class="btn-row">
            <button class="btn" style="flex:1;" data-act="back">← Different number</button>
            <button class="btn primary" style="flex:1;" data-act="use">Use this Person</button>
          </div>
        </div>
      `;
    }

    function mount() {
      root.innerHTML =
        state.view === 'profile' ? profile() : state.view === 'details' ? step2Details() : step1();
      const mob = root.querySelector('#m');
      if (mob) {
        mob.focus();
        mob.addEventListener('input', (e) => {
          state.mobile = e.target.value;
          state.mobileError = '';
          // Re-render only the button row so input keeps focus
          const continueBtn = root.querySelector('[data-act="next"]');
          if (continueBtn) {
            const digits = normalize(state.mobile).length;
            continueBtn.disabled = digits !== 10;
          }
        });
      }
      const npn = root.querySelector('#np-name');
      if (npn) npn.addEventListener('input', (e) => {
        state.newPerson.name = e.target.value;
        root.querySelector('[data-act="add"]').disabled = !e.target.value.trim();
      });
      const npd = root.querySelector('#np-dob');
      if (npd) npd.addEventListener('input', (e) => (state.newPerson.dob = e.target.value));
      const nps = root.querySelector('#np-hs');
      if (nps) nps.addEventListener('change', (e) => (state.newPerson.homeSabha = e.target.value));

      root.querySelectorAll('[data-act]').forEach((b) => {
        b.addEventListener('click', () => {
          const a = b.dataset.act;
          if (a === 'next') {
            if (normalize(state.mobile).length !== 10) {
              state.mobileError = 'Mobile must be 10 digits.';
              mount();
              return;
            }
            if (isMatch(state.mobile)) {
              state.view = 'profile';
            } else {
              state.view = 'details';
            }
            mount();
          } else if (a === 'back') {
            state.view = 'mobile';
            mount();
          } else if (a === 'use') {
            alert('Proceeding with existing Person: ' + M.name);
          } else if (a === 'add') {
            alert(
              `Added "${state.newPerson.name}" to Directory at Home: ${state.newPerson.homeSabha}.`,
            );
            state.view = 'mobile';
            state.mobile = '';
            state.newPerson = { name: '', dob: '', homeSabha: 'Yuvak Sabha · Andheri-7' };
            mount();
          }
        });
      });
    }
    mount();
  }

  window.__VARIANTS = [{ key: 'main', name: 'Add Person to Directory', render }];
})();
