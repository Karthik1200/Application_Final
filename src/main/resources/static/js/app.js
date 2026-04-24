// ============================
// VRGT - Core Application JS
// ============================

const API = {
    async request(url, options = {}) {
        const defaults = {
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin'
        };
        const config = { ...defaults, ...options };
        if (options.body && typeof options.body === 'object') {
            config.body = JSON.stringify(options.body);
        }
        try {
            const response = await fetch(url, config);
            const data = await response.json();
            return data;
        } catch (error) {
            console.error('API Error:', error);
            return { success: false, message: error.message };
        }
    },
    get(url) { return this.request(url); },
    post(url, body) { return this.request(url, { method: 'POST', body }); }
};

// Toast notifications
const Toast = {
    container: null,
    init() {
        if (!this.container) {
            this.container = document.createElement('div');
            this.container.className = 'toast-container';
            document.body.appendChild(this.container);
        }
    },
    show(message, type = 'info', duration = 4000) {
        this.init();
        const icons = { success: '✓', error: '✕', warning: '⚠', info: 'ℹ' };
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerHTML = `<span>${icons[type] || 'ℹ'}</span><span>${message}</span>`;
        this.container.appendChild(toast);
        toast.addEventListener('click', () => toast.remove());
        setTimeout(() => {
            toast.style.animation = 'toast-in 0.3s ease reverse';
            setTimeout(() => toast.remove(), 300);
        }, duration);
    },
    success(msg) { this.show(msg, 'success'); },
    error(msg) { this.show(msg, 'error'); },
    warning(msg) { this.show(msg, 'warning'); },
    info(msg) { this.show(msg, 'info'); }
};

// Clock
function updateClock() {
    const el = document.getElementById('headerTime');
    if (el) {
        const now = new Date();
        el.textContent = now.toLocaleString('en-IN', {
            weekday: 'short', day: '2-digit', month: 'short',
            hour: '2-digit', minute: '2-digit', second: '2-digit'
        });
    }
}
setInterval(updateClock, 1000);
document.addEventListener('DOMContentLoaded', updateClock);

// WebSocket connection
let stompClient = null;

function connectWebSocket() {
    if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') return;
    try {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null; // Suppress debug logs
        stompClient.connect({}, function() {
            console.log('WebSocket connected');

            stompClient.subscribe('/topic/notifications', function(msg) {
                const data = JSON.parse(msg.body);
                Toast.info(data.title + ': ' + data.message);
                updateNotificationCount();
            });

            stompClient.subscribe('/topic/tracking', function(msg) {
                const data = JSON.parse(msg.body);
                if (typeof onTrackingUpdate === 'function') {
                    onTrackingUpdate(data);
                }
            });

            stompClient.subscribe('/topic/gate-events', function(msg) {
                const data = JSON.parse(msg.body);
                if (typeof onGateEvent === 'function') {
                    onGateEvent(data);
                }
            });

            stompClient.subscribe('/topic/reception-events', function(msg) {
                const data = JSON.parse(msg.body);
                if (typeof onReceptionEvent === 'function') {
                    onReceptionEvent(data);
                }
            });

            stompClient.subscribe('/topic/rooms', function(msg) {
                const data = JSON.parse(msg.body);
                if (typeof onRoomUpdate === 'function') {
                    onRoomUpdate(data);
                }
            });
        }, function(error) {
            console.log('WebSocket error, reconnecting in 5s...');
            setTimeout(connectWebSocket, 5000);
        });
    } catch(e) {
        console.log('WebSocket not available');
    }
}

function updateNotificationCount() {
    const badge = document.querySelector('.notification-bell .count');
    if (badge) {
        let count = parseInt(badge.textContent || '0');
        badge.textContent = count + 1;
        badge.style.display = 'flex';
    }
}

// Status badge helper
function getStatusBadge(status) {
    const map = {
        'PRE_REGISTERED': { class: 'pre-registered', label: 'Pre-Registered' },
        'VERIFIED': { class: 'verified', label: 'Verified' },
        'CHECKED_IN_GATE': { class: 'checked-in', label: 'At Gate' },
        'AT_RECEPTION': { class: 'at-reception', label: 'At Reception' },
        'HOST_CONFIRMED': { class: 'at-reception', label: 'Host Confirmed' },
        'EN_ROUTE': { class: 'en-route', label: 'En Route' },
        'IN_MEETING': { class: 'in-meeting', label: 'In Meeting' },
        'CHECKED_OUT': { class: 'checked-out', label: 'Checked Out' },
        'AVAILABLE': { class: 'available', label: 'Available' },
        'OCCUPIED': { class: 'occupied', label: 'Occupied' },
        'RESERVED': { class: 'reserved', label: 'Reserved' },
    };
    const info = map[status] || { class: 'pre-registered', label: status };
    return `<span class="badge-status ${info.class}">${info.label}</span>`;
}

// Format time ago
function timeAgo(dateStr) {
    if (!dateStr) return '—';
    const date = new Date(dateStr);
    const now = new Date();
    const diff = Math.floor((now - date) / 1000);
    if (diff < 60) return diff + 's ago';
    if (diff < 3600) return Math.floor(diff / 60) + 'm ago';
    if (diff < 86400) return Math.floor(diff / 3600) + 'h ago';
    return Math.floor(diff / 86400) + 'd ago';
}

// Logout
async function logout() {
    await API.post('/api/auth/logout');
    window.location.href = '/login';
}

// Init WebSocket on page load
document.addEventListener('DOMContentLoaded', () => {
    connectWebSocket();
});
