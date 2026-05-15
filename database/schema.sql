-- ============================================================
--  Smart Hotel Visitor Route Guidance & Real-Time Tracking
--  MySQL 8+ Schema — Production Ready
--  Database: vrgt_db
-- ============================================================

CREATE DATABASE IF NOT EXISTS vrgt_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE vrgt_db;

-- ── Roles & Users ────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS app_users (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    full_name    VARCHAR(120) NOT NULL,
    email        VARCHAR(120) NOT NULL UNIQUE,
    phone        VARCHAR(20),
    department   VARCHAR(80),
    role         ENUM('ADMIN','SECURITY_GUARD','RECEPTIONIST','HOST','MANAGER') NOT NULL DEFAULT 'HOST',
    active       TINYINT(1)   NOT NULL DEFAULT 1,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login   DATETIME,
    INDEX idx_users_role  (role),
    INDEX idx_users_email (email)
) ENGINE=InnoDB;

-- ── Visitors ─────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS visitors (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name             VARCHAR(120) NOT NULL,
    email                 VARCHAR(120) NOT NULL,
    phone                 VARCHAR(20)  NOT NULL,
    company               VARCHAR(120),
    id_type               VARCHAR(40),
    id_number             VARCHAR(60),
    photo_url             VARCHAR(500),
    purpose               VARCHAR(255),
    host_name             VARCHAR(120) NOT NULL,
    host_email            VARCHAR(120),
    host_phone            VARCHAR(20),
    host_user_id          BIGINT,
    status                ENUM('PRE_REGISTERED','VERIFIED','CHECKED_IN_GATE','AT_RECEPTION',
                               'HOST_CONFIRMED','EN_ROUTE','IN_MEETING','CHECKED_OUT',
                               'BLACKLISTED','EXPIRED') NOT NULL DEFAULT 'PRE_REGISTERED',
    qr_code               TEXT,
    qr_image_base64       MEDIUMTEXT,
    qr_expiry             DATETIME,
    otp                   VARCHAR(10),
    otp_expiry            DATETIME,
    otp_verified          TINYINT(1) DEFAULT 0,
    email_verified        TINYINT(1) DEFAULT 0,
    assigned_room_id      VARCHAR(20),
    nfc_tag_id            VARCHAR(60),
    pre_registered_at     DATETIME,
    gate_checked_in_at    DATETIME,
    reception_checked_in_at DATETIME,
    meeting_started_at    DATETIME,
    checked_out_at        DATETIME,
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (host_user_id) REFERENCES app_users(id) ON DELETE SET NULL,
    INDEX idx_visitor_status    (status),
    INDEX idx_visitor_email     (email),
    INDEX idx_visitor_phone     (phone),
    INDEX idx_visitor_id_number (id_number),
    INDEX idx_visitor_created   (created_at)
) ENGINE=InnoDB;

-- ── Blacklist ─────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS blacklist_entries (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name   VARCHAR(120),
    email       VARCHAR(120),
    phone       VARCHAR(20),
    id_number   VARCHAR(60),
    reason      TEXT NOT NULL,
    added_by    VARCHAR(80),
    active      TINYINT(1) DEFAULT 1,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_bl_email     (email),
    INDEX idx_bl_phone     (phone),
    INDEX idx_bl_id_number (id_number)
) ENGINE=InnoDB;

-- ── Gate Logs ─────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS gate_logs (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    visitor_id   BIGINT NOT NULL DEFAULT 0,
    visitor_name VARCHAR(120),
    gate_id      VARCHAR(30),
    action       VARCHAR(30) NOT NULL,   -- ENTRY | EXIT | ENTRY_FAILED
    qr_scanned   TEXT,
    qr_valid     TINYINT(1) DEFAULT 0,
    id_verified  TINYINT(1) DEFAULT 0,
    verified_by  VARCHAR(80),
    remarks      VARCHAR(255),
    timestamp    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_gate_visitor   (visitor_id),
    INDEX idx_gate_timestamp (timestamp),
    INDEX idx_gate_action    (action)
) ENGINE=InnoDB;

-- ── Meeting Rooms ─────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS meeting_rooms (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_code   VARCHAR(20) NOT NULL UNIQUE,
    room_name   VARCHAR(120) NOT NULL,
    floor       VARCHAR(10) NOT NULL,
    capacity    INT NOT NULL DEFAULT 6,
    facilities  VARCHAR(255),
    status      ENUM('AVAILABLE','OCCUPIED','RESERVED','MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_room_status (status),
    INDEX idx_room_floor  (floor)
) ENGINE=InnoDB;

-- ── Meetings ──────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS meetings (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    visitor_id        BIGINT,
    visitor_name      VARCHAR(120),
    room_id           BIGINT,
    room_code         VARCHAR(20),
    host_user_id      BIGINT,
    host_name         VARCHAR(120),
    title             VARCHAR(255),
    status            VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    scheduled_start   DATETIME,
    scheduled_end     DATETIME,
    actual_start      DATETIME,
    actual_end        DATETIME,
    extended_minutes  INT DEFAULT 0,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (visitor_id)   REFERENCES visitors(id)      ON DELETE SET NULL,
    FOREIGN KEY (room_id)      REFERENCES meeting_rooms(id) ON DELETE SET NULL,
    FOREIGN KEY (host_user_id) REFERENCES app_users(id)     ON DELETE SET NULL,
    INDEX idx_meeting_visitor (visitor_id),
    INDEX idx_meeting_room    (room_id),
    INDEX idx_meeting_status  (status),
    INDEX idx_meeting_start   (scheduled_start)
) ENGINE=InnoDB;

-- ── Location Events (Real-Time Tracking) ─────────────────────

CREATE TABLE IF NOT EXISTS location_events (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    visitor_id        BIGINT NOT NULL,
    visitor_name      VARCHAR(120),
    location          VARCHAR(60) NOT NULL,
    zone_id           VARCHAR(40),
    floor             VARCHAR(10),
    position_x        DOUBLE DEFAULT 0,
    position_y        DOUBLE DEFAULT 0,
    restricted_zone   TINYINT(1) DEFAULT 0,
    alert_triggered   TINYINT(1) DEFAULT 0,
    dwell_time_seconds BIGINT,
    timestamp         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (visitor_id) REFERENCES visitors(id) ON DELETE CASCADE,
    INDEX idx_loc_visitor   (visitor_id),
    INDEX idx_loc_timestamp (timestamp),
    INDEX idx_loc_location  (location)
) ENGINE=InnoDB;

-- ── Emergency Logs ────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS emergency_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    emergency_type  VARCHAR(60) NOT NULL,   -- FIRE, SECURITY, MEDICAL, EVACUATION, DRILL
    severity        VARCHAR(20) NOT NULL DEFAULT 'HIGH',
    triggered_by    VARCHAR(80),
    description     TEXT,
    affected_zones  VARCHAR(255),
    total_visitors  INT DEFAULT 0,
    evacuated_count INT DEFAULT 0,
    missing_count   INT DEFAULT 0,
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | RESOLVED | DRILL
    resolved_by     VARCHAR(80),
    triggered_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at     DATETIME,
    INDEX idx_emergency_status (status),
    INDEX idx_emergency_type   (emergency_type),
    INDEX idx_emergency_time   (triggered_at)
) ENGINE=InnoDB;

-- ── Notifications ─────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS notifications (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    type         VARCHAR(60) NOT NULL,
    title        VARCHAR(255) NOT NULL,
    message      TEXT,
    recipient_id BIGINT,
    visitor_id   BIGINT,
    channel      VARCHAR(30) DEFAULT 'IN_APP',
    is_read      TINYINT(1) DEFAULT 0,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notif_recipient (recipient_id),
    INDEX idx_notif_visitor   (visitor_id),
    INDEX idx_notif_read      (is_read),
    INDEX idx_notif_created   (created_at)
) ENGINE=InnoDB;

-- ── Audit Logs ────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS audit_logs (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type        VARCHAR(60) NOT NULL,
    entity_type       VARCHAR(40),
    entity_id         BIGINT,
    performed_by      VARCHAR(80),
    performed_by_role VARCHAR(40),
    description       TEXT,
    old_value         TEXT,
    new_value         TEXT,
    ip_address        VARCHAR(45),
    timestamp         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_event     (event_type),
    INDEX idx_audit_entity    (entity_type, entity_id),
    INDEX idx_audit_performer (performed_by),
    INDEX idx_audit_timestamp (timestamp)
) ENGINE=InnoDB;

-- ── QR Tokens ─────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS qr_tokens (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    visitor_id   BIGINT NOT NULL,
    token_hash   VARCHAR(512) NOT NULL,
    encrypted_qr TEXT,
    issued_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at   DATETIME NOT NULL,
    used         TINYINT(1) DEFAULT 0,
    used_at      DATETIME,
    revoked      TINYINT(1) DEFAULT 0,
    FOREIGN KEY (visitor_id) REFERENCES visitors(id) ON DELETE CASCADE,
    INDEX idx_qrt_visitor (visitor_id),
    INDEX idx_qrt_expires (expires_at)
) ENGINE=InnoDB;

-- ── Restricted Zones ──────────────────────────────────────────

CREATE TABLE IF NOT EXISTS restricted_zones (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    zone_id     VARCHAR(40) NOT NULL UNIQUE,
    zone_name   VARCHAR(120) NOT NULL,
    floor       VARCHAR(10),
    description VARCHAR(255),
    active      TINYINT(1) DEFAULT 1,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ── Alerts ────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS alerts (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_type   VARCHAR(60) NOT NULL,
    visitor_id   BIGINT,
    visitor_name VARCHAR(120),
    zone_id      VARCHAR(40),
    floor        VARCHAR(10),
    message      TEXT,
    severity     VARCHAR(20) DEFAULT 'MEDIUM',
    acknowledged TINYINT(1) DEFAULT 0,
    ack_by       VARCHAR(80),
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ack_at       DATETIME,
    FOREIGN KEY (visitor_id) REFERENCES visitors(id) ON DELETE SET NULL,
    INDEX idx_alert_type      (alert_type),
    INDEX idx_alert_visitor   (visitor_id),
    INDEX idx_alert_ack       (acknowledged),
    INDEX idx_alert_created   (created_at)
) ENGINE=InnoDB;

-- ── Room Bookings ─────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS room_bookings (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id     BIGINT NOT NULL,
    visitor_id  BIGINT,
    host_id     BIGINT,
    booked_from DATETIME NOT NULL,
    booked_to   DATETIME NOT NULL,
    status      VARCHAR(30) DEFAULT 'CONFIRMED',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id)    REFERENCES meeting_rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (visitor_id) REFERENCES visitors(id)      ON DELETE SET NULL,
    FOREIGN KEY (host_id)    REFERENCES app_users(id)     ON DELETE SET NULL,
    INDEX idx_booking_room   (room_id),
    INDEX idx_booking_period (booked_from, booked_to)
) ENGINE=InnoDB;

-- ════════════════════════════════════════════════════════════
--  SEED DATA
-- ════════════════════════════════════════════════════════════

-- Default staff accounts (passwords are BCrypt-hashed)
-- admin123  → $2a$10$...  (run DataInitializer or insert via app startup)

INSERT IGNORE INTO meeting_rooms (room_code, room_name, floor, capacity, facilities, status) VALUES
  ('MR-101', 'Sapphire Boardroom',    '1', 12, 'Projector, Video Conferencing, Whiteboard', 'AVAILABLE'),
  ('MR-102', 'Emerald Conference',    '1',  8, 'Projector, Whiteboard', 'AVAILABLE'),
  ('MR-201', 'Diamond Executive',     '2',  6, 'Video Conferencing, Smart TV', 'AVAILABLE'),
  ('MR-202', 'Ruby Meeting Pod',      '2',  4, 'Whiteboard', 'AVAILABLE'),
  ('MR-301', 'Topaz Strategy Room',   '3', 10, 'Projector, Video Conferencing, Whiteboard, Smart Board', 'AVAILABLE'),
  ('MR-302', 'Opal Innovation Lab',   '3', 20, 'Multiple Screens, Video Wall, Recording', 'AVAILABLE'),
  ('LOBBY-1','Main Lobby Lounge',     'G',  6, 'TV, Coffee Station', 'AVAILABLE'),
  ('VIP-1',  'VIP Suite',             '4',  4, 'Private Bath, Premium AV, Concierge', 'AVAILABLE');

INSERT IGNORE INTO restricted_zones (zone_id, zone_name, floor, description) VALUES
  ('RZ-SERVER',  'Server Room',       'B1', 'IT Infrastructure — Authorized Personnel Only'),
  ('RZ-SECURITY','Security Control',  'G',  'CCTV and Access Control Hub'),
  ('RZ-FINANCE', 'Finance Vault',     '3',  'Finance Documents and Safe — Restricted'),
  ('RZ-EXEC',    'Executive Floor',   '5',  'C-Suite — Appointment Required');
