// PROTOTYPE — shared floating variant switcher.
// Reads ?variant= from URL, shows current, ← → cycle, keyboard arrows.
// Each route's variants.js defines window.__VARIANTS = [{key, name, render(root)}].

(function () {
  const params = new URLSearchParams(window.location.search);
  const variants = window.__VARIANTS || [];
  if (variants.length === 0) return;
  // When only one variant remains, render it but skip the switcher UI.
  const singleVariant = variants.length === 1;

  const keys = variants.map((v) => v.key);
  let current = params.get('variant');
  if (!keys.includes(current)) current = keys[0];

  function renderCurrent() {
    const root = document.getElementById('variant-root');
    if (!root) return;
    root.innerHTML = '';
    const v = variants.find((x) => x.key === current);
    if (v) v.render(root);
    updateLabel();
  }

  function goto(key) {
    current = key;
    const url = new URL(window.location.href);
    url.searchParams.set('variant', key);
    window.history.replaceState(null, '', url.toString());
    renderCurrent();
  }

  function cycle(delta) {
    const i = keys.indexOf(current);
    const next = keys[(i + delta + keys.length) % keys.length];
    goto(next);
  }

  function updateLabel() {
    const label = document.getElementById('switcher-label');
    if (!label) return;
    const v = variants.find((x) => x.key === current);
    label.textContent = v ? `${v.key} — ${v.name}` : current;
  }

  function mountBar() {
    const bar = document.createElement('div');
    bar.id = 'prototype-switcher';
    bar.innerHTML = `
      <button id="switcher-prev" aria-label="Previous variant">←</button>
      <span id="switcher-label"></span>
      <button id="switcher-next" aria-label="Next variant">→</button>
      <a id="switcher-home" href="../index.html" title="All prototypes">⌂</a>
    `;
    document.body.appendChild(bar);
    document.getElementById('switcher-prev').addEventListener('click', () => cycle(-1));
    document.getElementById('switcher-next').addEventListener('click', () => cycle(1));
  }

  function isTextEditing() {
    const el = document.activeElement;
    if (!el) return false;
    const tag = el.tagName;
    if (tag === 'INPUT' || tag === 'TEXTAREA') return true;
    if (el.isContentEditable) return true;
    return false;
  }

  document.addEventListener('keydown', (e) => {
    if (isTextEditing()) return;
    if (e.key === 'ArrowLeft') {
      e.preventDefault();
      cycle(-1);
    } else if (e.key === 'ArrowRight') {
      e.preventDefault();
      cycle(1);
    }
  });

  document.addEventListener('DOMContentLoaded', () => {
    if (!singleVariant) mountBar();
    renderCurrent();
  });
})();
