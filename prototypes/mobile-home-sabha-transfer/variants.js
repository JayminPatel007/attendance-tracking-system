// PROTOTYPE — Mobile · Verified Home Sabha Transfer (ADR-0002)
// Sanchalak-initiated, OTP-verified by Person. Always Person-initiated socially.

(function () {
  const DIRECTORY = [
    { id: 't1', initials: 'MG', name: 'Mihir Gajjar', mobile: '+91 98201 89898', currentHome: 'Yuvak Sabha · Borivali-3' },
    { id: 't2', initials: 'PV', name: 'Pranav Vyas', mobile: '+91 98202 11111', currentHome: 'Yuvak Sabha · Vile Parle-1' },
    { id: 't3', initials: 'JT', name: 'Jaymin Thakkar', mobile: '+91 98202 33333', currentHome: 'Yuvak Sabha · Kandivali-4' },
    { id: 't4', initials: 'KR', name: 'Kunal Raval', mobile: '+91 98202 44444', currentHome: 'Yuvak Sabha · Goregaon-1' },
  ];

  const DEST = 'Yuvak Sabha · Kshetra Andheri-7';
  const VALID_OTP = '4827';

  let step = 'find'; // 'find' | 'confirm' | 'otp' | 'done'
  let query = '';
  let selected = null;
  let otp = ['', '', '', ''];
  let otpError = '';
  let resendDisabled = true;
  let resendIn = 30;

  function render(root) {
    function progress() {
      const states = ['find', 'confirm', 'otp', 'done'];
      const i = states.indexOf(step);
      return `
        <div class="steps-bar">
          ${states
            .slice(0, 3)
            .map((_, idx) => `<div class="step ${idx < i ? 'done' : idx === i ? 'active' : ''}"></div>`)
            .join('')}
          <span class="step-label">${['Find Person', 'Confirm', 'OTP', 'Done'][i]}</span>
        </div>
      `;
    }

    function stepFind() {
      const q = query.trim().toLowerCase();
      const list = q
        ? DIRECTORY.filter((p) => p.name.toLowerCase().includes(q) || p.mobile.replace(/\D/g, '').includes(q.replace(/\D/g, '')))
        : DIRECTORY;
      return `
        ${progress()}
        <div class="hs-search">
          <input id="hs-q" placeholder="Search by name or mobile" value="${query}" autofocus />
          <div class="hint">Person should be standing in front of you. Their mobile must be reachable to receive the OTP.</div>
        </div>
        <div class="hs-list">
          ${list.length === 0
            ? `<div style="padding:40px 16px;text-align:center;color:var(--ink-faint);font-size:13px;">No match. <a href="../mobile-add-person/" style="color:var(--accent);display:block;margin-top:8px;">Add a new Person →</a></div>`
            : list
                .map(
                  (p) => `
                  <div class="hs-row" data-id="${p.id}">
                    <div class="avatar">${p.initials}</div>
                    <div class="meta">
                      <div class="name">${p.name}</div>
                      <div class="sub">Home: ${p.currentHome} · ${p.mobile}</div>
                    </div>
                    <span class="arrow">›</span>
                  </div>
                `,
                )
                .join('')}
        </div>
      `;
    }

    function stepConfirm() {
      const p = DIRECTORY.find((x) => x.id === selected);
      return `
        ${progress()}
        <div class="hs-body">
          <div class="transfer-summary">
            <div class="person">
              <div class="avatar lg">${p.initials}</div>
              <div>
                <div class="name-lg">${p.name}</div>
                <div class="mobile">${p.mobile}</div>
              </div>
            </div>
            <div class="movement">
              <div class="from">${p.currentHome}</div>
              <div class="arrow-dn">↓</div>
              <div class="to">${DEST}</div>
            </div>
          </div>
          <div class="legal-note">
            Sending the OTP to <strong>${p.mobile}</strong> confirms <strong>${p.name}'s</strong> consent for the Home Sabha change. Always Person-initiated — they asked you for this.
          </div>
        </div>
        <div class="btn-row" style="display:flex;gap:8px;padding:12px 16px;background:var(--surface);border-top:1px solid var(--line);">
          <button class="btn" style="flex:1;" data-act="back-find">← Different Person</button>
          <button class="btn primary" style="flex:1;" data-act="send-otp">Send OTP</button>
        </div>
      `;
    }

    function stepOtp() {
      const p = DIRECTORY.find((x) => x.id === selected);
      return `
        ${progress()}
        <div class="otp-block">
          <div class="icon-lg">⌧</div>
          <h2>Enter the OTP</h2>
          <div class="lead">4-digit code sent to <strong>${p.mobile}</strong>. Ask ${p.name.split(' ')[0]} to read it out.</div>
          <div class="otp-input">
            ${otp.map((v, i) => `<input id="otp-${i}" data-i="${i}" maxlength="1" inputmode="numeric" value="${v}" />`).join('')}
          </div>
          ${otpError ? `<div class="otp-error">${otpError}</div>` : ''}
          <div class="${resendDisabled ? 'otp-resend disabled' : 'otp-resend'}" data-act="resend">${resendDisabled ? `Resend in ${resendIn}s` : '↻ Resend OTP'}</div>
          <div class="xs faint" style="margin-top:18px;">Hint for prototyping: the valid OTP is <strong>4827</strong>.</div>
        </div>
        <div class="btn-row" style="display:flex;gap:8px;padding:12px 16px;background:var(--surface);border-top:1px solid var(--line);">
          <button class="btn" style="flex:1;" data-act="back-confirm">← Back</button>
          <button class="btn primary" style="flex:1;" data-act="verify" ${otp.every((d) => d.length === 1) ? '' : 'disabled'}>Verify &amp; Transfer</button>
        </div>
      `;
    }

    function stepDone() {
      const p = DIRECTORY.find((x) => x.id === selected);
      return `
        <div class="success-block">
          <div class="check">✓</div>
          <h2>Home Sabha updated</h2>
          <p><strong>${p.name}</strong> is now part of <br/><strong>${DEST}</strong>'s Roster.</p>
          <div style="display:grid;gap:8px;max-width:280px;margin:0 auto;">
            <button class="btn primary" data-act="reset">Done</button>
            <button class="btn" data-act="another">Transfer another Person</button>
          </div>
          <div class="xs faint" style="margin-top:24px;">Roster will refresh on next sync. Old Home Sabha audit shows the verified change.</div>
        </div>
      `;
    }

    function mount() {
      const stepHtml =
        step === 'find' ? stepFind()
        : step === 'confirm' ? stepConfirm()
        : step === 'otp' ? stepOtp()
        : stepDone();
      root.innerHTML = `
        <div class="hs-page">
          <div class="appbar">
            <h1>← Home Sabha Transfer</h1>
            <div class="sub">${DEST}</div>
          </div>
          ${stepHtml}
        </div>
      `;
      attach();
    }

    function attach() {
      const q = root.querySelector('#hs-q');
      if (q) {
        q.addEventListener('input', (e) => {
          query = e.target.value;
          mount();
          root.querySelector('#hs-q').focus();
        });
      }
      root.querySelectorAll('.hs-row').forEach((row) => {
        row.addEventListener('click', () => {
          selected = row.dataset.id;
          step = 'confirm';
          mount();
        });
      });

      otp.forEach((_, i) => {
        const inp = root.querySelector(`#otp-${i}`);
        if (!inp) return;
        if (i === 0) inp.focus();
        inp.addEventListener('input', (e) => {
          const v = e.target.value.replace(/\D/g, '').slice(0, 1);
          otp[i] = v;
          e.target.value = v;
          if (v && i < 3) root.querySelector(`#otp-${i + 1}`).focus();
          otpError = '';
          const verify = root.querySelector('[data-act="verify"]');
          if (verify) verify.disabled = !otp.every((d) => d.length === 1);
        });
        inp.addEventListener('keydown', (e) => {
          if (e.key === 'Backspace' && !otp[i] && i > 0) {
            otp[i - 1] = '';
            root.querySelector(`#otp-${i - 1}`).focus();
          }
        });
      });

      root.querySelectorAll('[data-act]').forEach((b) => {
        b.addEventListener('click', () => {
          const a = b.dataset.act;
          if (a === 'back-find') { step = 'find'; selected = null; mount(); }
          else if (a === 'send-otp') {
            step = 'otp';
            otp = ['', '', '', ''];
            otpError = '';
            resendDisabled = true;
            resendIn = 30;
            mount();
            const t = setInterval(() => {
              resendIn--;
              if (resendIn <= 0) {
                resendDisabled = false;
                clearInterval(t);
              }
              const el = root.querySelector('.otp-resend');
              if (!el) return clearInterval(t);
              el.textContent = resendDisabled ? `Resend in ${resendIn}s` : '↻ Resend OTP';
              el.classList.toggle('disabled', resendDisabled);
            }, 1000);
          }
          else if (a === 'back-confirm') { step = 'confirm'; mount(); }
          else if (a === 'verify') {
            if (otp.join('') === VALID_OTP) { step = 'done'; mount(); }
            else {
              const p = DIRECTORY.find((x) => x.id === selected);
              otpError = `Incorrect OTP. Ask ${p ? p.name.split(' ')[0] : 'them'} to re-read.`;
              otp = ['', '', '', ''];
              mount();
            }
          }
          else if (a === 'resend') {
            if (resendDisabled) return;
            resendDisabled = true; resendIn = 30; mount();
          }
          else if (a === 'reset') { step = 'find'; query = ''; selected = null; otp = ['','','',''];  mount(); }
          else if (a === 'another') { step = 'find'; query = ''; selected = null; otp = ['','','','']; mount(); }
        });
      });
    }
    mount();
  }

  window.__VARIANTS = [{ key: 'main', name: 'Home Sabha Transfer', render }];
})();
