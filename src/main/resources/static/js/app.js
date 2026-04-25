// ============================================================
//  VRGT Core Application JS v2.0
// ============================================================

/* ── API Client ─────────────────────────────────────────── */
const API = {
  async request(url, options = {}) {
    const cfg = {
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      ...options,
    };
    if (options.body && typeof options.body === 'object') {
      cfg.body = JSON.stringify(options.body);
    }
    try {
      const res = await fetch(url, cfg);
      const data = await res.json();
      return data;
    } catch (e) {
      console.error('API Error:', url, e);
      return { success: false, message: e.message };
    }
  },
  get(url)        { return this.request(url); },
  post(url, body) { return this.request(url, { method: 'POST', body }); },
  put(url, body)  { return this.request(url, { method: 'PUT',  body }); },
  del(url)        { return this.request(url, { method: 'DELETE' }); },

  /* Trigger a file download from a GET endpoint */
  download(url, filename) {
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
  }
};

/* ── Toast ──────────────────────────────────────────────── */
const Toast = {
  _container: null,
  _get() {
    if (!this._container) {
      this._container = document.createElement('div');
      this._container.className = 'toast-container';
      document.body.appendChild(this._container);
    }
    return this._container;
  },
  show(msg, type = 'info', ms = 4000) {
    const icons = { success: '✓', error: '✕', warning: '⚠', info: 'ℹ' };
    const t = document.createElement('div');
    t.className = `toast ${type}`;
    t.innerHTML = `<span style="font-size:16px;flex-shrink:0">${icons[type]||'ℹ'}</span><span style="flex:1">${msg}</span>`;
    t.addEventListener('click', () => t.remove());
    this._get().appendChild(t);
    setTimeout(() => {
      t.style.opacity = '0';
      t.style.transform = 'translateX(100%)';
      t.style.transition = '0.3s ease';
      setTimeout(() => t.remove(), 300);
    }, ms);
  },
  success(m, ms) { this.show(m, 'success', ms); },
  error(m, ms)   { this.show(m, 'error',   ms); },
  warning(m, ms) { this.show(m, 'warning', ms); },
  info(m, ms)    { this.show(m, 'info',    ms); },
};

/* ── Clock ──────────────────────────────────────────────── */
function _updateClock() {
  const el = document.getElementById('headerTime');
  if (el) {
    el.textContent = new Date().toLocaleString('en-IN', {
      weekday: 'short', day: '2-digit', month: 'short',
      hour: '2-digit', minute: '2-digit', second: '2-digit',
    });
  }
}
setInterval(_updateClock, 1000);
document.addEventListener('DOMContentLoaded', _updateClock);

/* ── WebSocket ──────────────────────────────────────────── */
let _stompClient = null;
let _wsRetries   = 0;

function connectWebSocket() {
  if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') return;
  try {
    const sock = new SockJS('/ws');
    _stompClient = Stomp.over(sock);
    _stompClient.debug = null;
    _stompClient.connect({}, () => {
      _wsRetries = 0;
      _stompClient.subscribe('/topic/notifications', m => {
        const d = JSON.parse(m.body);
        Toast.info(`${d.title}: ${d.message}`);
        _incNotifBadge();
      });
      _stompClient.subscribe('/topic/tracking',       m => { if (typeof onTrackingUpdate === 'function')  onTrackingUpdate(JSON.parse(m.body)); });
      _stompClient.subscribe('/topic/gate-events',    m => { if (typeof onGateEvent === 'function')       onGateEvent(JSON.parse(m.body)); });
      _stompClient.subscribe('/topic/reception-queue',m => { if (typeof onReceptionEvent === 'function')  onReceptionEvent(JSON.parse(m.body)); });
      _stompClient.subscribe('/topic/rooms',          m => { if (typeof onRoomUpdate === 'function')      onRoomUpdate(JSON.parse(m.body)); });
    }, () => {
      const delay = Math.min(5000 * 2 ** _wsRetries++, 30000);
      setTimeout(connectWebSocket, delay);
    });
  } catch (e) { /* WebSocket unavailable */ }
}

function _incNotifBadge() {
  document.querySelectorAll('.notification-bell .count, .icon-btn .badge').forEach(el => {
    el.style.display = 'flex';
    el.textContent   = (parseInt(el.textContent || '0') + 1).toString();
  });
}

/* ── Tabs ───────────────────────────────────────────────── */
function initTabs(navSelector, panelSelector) {
  document.querySelectorAll(navSelector).forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll(navSelector).forEach(b => b.classList.remove('active'));
      document.querySelectorAll(panelSelector).forEach(p => p.classList.remove('active'));
      btn.classList.add('active');
      const panel = document.getElementById(btn.dataset.tab);
      if (panel) panel.classList.add('active');
    });
  });
}

/* ── Badge helper ───────────────────────────────────────── */
function getStatusBadge(status) {
  const map = {
    PRE_REGISTERED:  { cls: 'pre-registered',  label: 'Pre-Registered' },
    VERIFIED:        { cls: 'verified',         label: 'Verified' },
    CHECKED_IN_GATE: { cls: 'at-gate',          label: 'At Gate' },
    AT_RECEPTION:    { cls: 'at-reception',     label: 'At Reception' },
    HOST_CONFIRMED:  { cls: 'confirmed',        label: 'Host Confirmed' },
    EN_ROUTE:        { cls: 'en-route',         label: 'En Route' },
    IN_MEETING:      { cls: 'in-meeting',       label: 'In Meeting' },
    CHECKED_OUT:     { cls: 'checked-out',      label: 'Checked Out' },
    BLACKLISTED:     { cls: 'alert',            label: 'Blacklisted' },
    EXPIRED:         { cls: 'checked-out',      label: 'Expired' },
    AVAILABLE:       { cls: 'available',        label: 'Available' },
    OCCUPIED:        { cls: 'occupied',         label: 'Occupied' },
    RESERVED:        { cls: 'reserved',         label: 'Reserved' },
    MAINTENANCE:     { cls: 'maintenance',      label: 'Maintenance' },
  };
  const { cls, label } = map[status] || { cls: 'pre-registered', label: status };
  return `<span class="badge ${cls}">${label}</span>`;
}

/* ── Time formatting ────────────────────────────────────── */
function timeAgo(dateStr) {
  if (!dateStr) return '—';
  const diff = Math.floor((Date.now() - new Date(dateStr)) / 1000);
  if (diff < 60)    return diff + 's ago';
  if (diff < 3600)  return Math.floor(diff / 60) + 'm ago';
  if (diff < 86400) return Math.floor(diff / 3600) + 'h ago';
  return new Date(dateStr).toLocaleDateString('en-IN', { day: '2-digit', month: 'short' });
}

function fmtTime(dateStr) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleString('en-IN', {
    day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit',
  });
}

/* ── Initials avatar ────────────────────────────────────── */
function initials(name) {
  if (!name) return '?';
  return name.trim().split(/\s+/).slice(0, 2).map(w => w[0]).join('').toUpperCase();
}

/* ── Avatar color from name ─────────────────────────────── */
const _avatarColors = ['#6366f1','#06b6d4','#10b981','#f59e0b','#a855f7','#ef4444','#3b82f6'];
function avatarColor(name) {
  let h = 0;
  for (let i = 0; i < (name||'').length; i++) h = (h * 31 + name.charCodeAt(i)) | 0;
  return _avatarColors[Math.abs(h) % _avatarColors.length];
}

/* ── Loading state helpers ──────────────────────────────── */
function setLoading(el, text = 'Loading…') {
  if (!el) return;
  el._orig = el.innerHTML;
  el.disabled = true;
  el.innerHTML = `<span class="spinner spinner-sm"></span> ${text}`;
}
function clearLoading(el) {
  if (!el || !el._orig) return;
  el.innerHTML = el._orig;
  el.disabled = false;
  el._orig = null;
}

/* ── Confirm dialog ─────────────────────────────────────── */
function confirm2(msg) {
  return new Promise(resolve => {
    const overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.innerHTML = `
      <div class="modal" style="max-width:380px">
        <div class="modal-header"><h3>Confirm</h3></div>
        <p style="font-size:14px;color:var(--text-secondary);margin-bottom:24px;">${msg}</p>
        <div class="modal-footer">
          <button class="btn btn-outline" id="_confirmNo">Cancel</button>
          <button class="btn btn-danger"  id="_confirmYes">Confirm</button>
        </div>
      </div>`;
    document.body.appendChild(overlay);
    overlay.querySelector('#_confirmYes').addEventListener('click', () => { overlay.remove(); resolve(true); });
    overlay.querySelector('#_confirmNo').addEventListener('click',  () => { overlay.remove(); resolve(false); });
  });
}

/* ── Search filter on table ─────────────────────────────── */
function liveSearch(inputId, tableBodyId) {
  const input = document.getElementById(inputId);
  const tbody = document.getElementById(tableBodyId);
  if (!input || !tbody) return;
  input.addEventListener('input', () => {
    const q = input.value.toLowerCase();
    tbody.querySelectorAll('tr').forEach(row => {
      row.style.display = row.textContent.toLowerCase().includes(q) ? '' : 'none';
    });
  });
}

/* ── Logout ─────────────────────────────────────────────── */
async function logout() {
  await API.post('/api/auth/logout');
  localStorage.removeItem('user');
  window.location.href = '/login';
}

/* ── User info from localStorage ────────────────────────── */
function currentUser() {
  try { return JSON.parse(localStorage.getItem('user') || '{}'); } catch { return {}; }
}

/* ── Set user info in header ────────────────────────────── */
function initUserHeader() {
  const u = currentUser();
  if (!u.username) return;
  document.querySelectorAll('.user-name').forEach(el => el.textContent = u.fullName || u.username);
  document.querySelectorAll('.user-avatar').forEach(el => el.textContent = initials(u.fullName || u.username));
}

/* ── Init ───────────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {
  connectWebSocket();
  initUserHeader();
});
