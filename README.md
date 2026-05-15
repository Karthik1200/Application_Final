# Smart Hotel Visitor Route Guidance & Real-Time Tracking System

> Enterprise-grade visitor intelligence platform — QR entry, live indoor tracking, emergency evacuation, and analytics.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Technology Stack](#technology-stack)
3. [Roles & Access](#roles--access)
4. [Module Walkthrough](#module-walkthrough)
5. [Quick Start (H2 Dev)](#quick-start-h2-dev)
6. [Docker Deployment (MySQL)](#docker-deployment-mysql)
7. [Environment Variables](#environment-variables)
8. [API Reference](#api-reference)
9. [WebSocket Topics](#websocket-topics)
10. [Database Schema](#database-schema)
11. [Project Structure](#project-structure)
12. [Default Credentials](#default-credentials)
13. [Testing Strategy](#testing-strategy)

---

## Architecture Overview

```
Browser / Mobile
      │  HTTP + WebSocket (STOMP/SockJS)
      ▼
┌──────────────────────────────────────────────┐
│           Spring Boot Monolith               │
│  ┌──────────┐  ┌──────────┐  ┌───────────┐  │
│  │  Auth /  │  │ Business │  │  WebSocket│  │
│  │  JWT     │  │  Layer   │  │  Broker   │  │
│  └──────────┘  └──────────┘  └───────────┘  │
│  ┌──────────────────────────────────────────┐│
│  │ Spring Data JPA  ·  Spring Security      ││
│  └──────────────────────────────────────────┘│
└──────────────────────┬───────────────────────┘
                       │
          ┌────────────┴─────────────┐
          ▼                          ▼
    MySQL 8 (prod)            H2 In-Memory (dev)
```

The system is a **single Spring Boot application** with a Thymeleaf + vanilla-JS frontend. All real-time events are pushed via WebSocket (STOMP). For true microservice deployment the modules are cleanly separated into independent packages and can be extracted individually.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT (jjwt 0.12) |
| Persistence | Spring Data JPA + Hibernate |
| Database (dev) | H2 (in-memory) |
| Database (prod) | MySQL 8+ |
| Real-Time | Spring WebSocket + STOMP |
| QR Code | ZXing (AES-256 encrypted payload) |
| OTP Delivery | MSG91 (SMS / Email / WhatsApp) |
| PDF Export | iText 7 |
| Excel Export | Apache POI |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| UI | Thymeleaf + Vanilla JS + Chart.js |
| Containerisation | Docker + Docker Compose |

---

## Roles & Access

| Role | Username (dev) | Password | Access |
|---|---|---|---|
| Admin | `admin` | `admin123` | Full access + analytics + user management |
| Security Guard | `guard1` | `guard123` | Gate kiosk, QR scanning |
| Receptionist | `reception1` | `reception123` | Reception queue, room assignment |
| Host | `host1` | `host123` | Visitor tracking, meeting management |

---

## Module Walkthrough

### 1 · Visitor Pre-Registration
- Visitor opens `/register` (public link, no login required)
- Fills in personal details + purpose + host name
- Selects OTP delivery channel: SMS / Email / WhatsApp
- **MSG91** delivers OTP → visitor verifies on Step 2
- On success: AES-256 encrypted **QR pass** generated (24-hour expiry) + displayed as Base64 image

### 2 · Gate Entry
- Security Guard at `/gate` enters QR string manually or via webcam
- Backend decrypts + validates: expiry, duplicate usage, blacklist
- On pass: visitor status → `CHECKED_IN_GATE`, gate log saved, WebSocket event broadcast to reception
- Guard can separately confirm physical ID verification

### 3 · Reception Check-In
- `/reception` shows live visitor queue (WebSocket-updated)
- Receptionist selects visitor → assigns meeting room (auto-suggest or manual)
- Host notification sent → host can approve or note
- Visitor status → `AT_RECEPTION` → `EN_ROUTE`

### 4 · Indoor Route Guidance
- `/indoor-map` renders an SVG floor plan with animated visitor dots
- Simulated BLE beacon positions update via WebSocket `/topic/tracking`
- Path line animates from current position to assigned room

### 5 · Real-Time Tracking
- `/tracking` shows all active visitors on the live floor map
- Restricted zone entry triggers automatic alerts
- Dwell-time threshold (30 min) fires timeout alerts
- Scheduled job (`@Scheduled`) runs every 2 minutes to check dwell times

### 6 · Meeting Room Management
- `/rooms` shows all rooms with AVAILABLE / OCCUPIED / RESERVED / MAINTENANCE status
- Auto-assign endpoint picks nearest available room by capacity
- Host can extend or end meeting; QR expiry updated dynamically

### 7 · Emergency Evacuation
- `/emergency` — Admin activates FIRE / SECURITY / MEDICAL / DRILL protocol
- WebSocket `/topic/emergency` broadcasts to all connected dashboards instantly
- Roll call screen lists every active visitor with last-known location
- Guard marks visitors as evacuated one-by-one or bulk "All Safe"
- Resolve button sends all-clear broadcast

### 8 · Admin Analytics
- `/admin` — KPI tiles, 7-day visitor trend chart, location heatmap
- Export visitors / gate logs / audit trail as PDF or Excel
- Full blacklist management (add / remove)
- Staff user CRUD with role assignment

---

## Quick Start (H2 Dev)

**Prerequisites:** Java 17+, Maven 3.9+

```bash
# Clone and run
cd Application_Final
./mvnw spring-boot:run

# App starts on http://localhost:8080
# H2 console: http://localhost:8080/h2-console
#   JDBC URL: jdbc:h2:mem:vrgtdb  |  user: sa  |  password: (blank)
# Swagger UI: http://localhost:8080/swagger-ui.html
```

Navigate to `http://localhost:8080` → redirects to `/login`.

---

## Docker Deployment (MySQL)

```bash
# 1. Copy environment file
cp .env.example .env
# Edit .env with your passwords / secrets

# 2. Build and start (first run pulls images + builds JAR ~3 min)
docker compose up -d --build

# 3. Follow logs
docker compose logs -f app

# 4. Open in browser
open http://localhost:8080

# With Nginx reverse proxy (port 80):
docker compose --profile production up -d
```

**Stop and clean up:**
```bash
docker compose down          # stop containers
docker compose down -v       # stop + remove MySQL data volume
```

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `MYSQL_HOST` | `localhost` | MySQL host |
| `MYSQL_PORT` | `3306` | MySQL port |
| `MYSQL_DB` | `vrgt_db` | Database name |
| `MYSQL_USER` | `vrgt_user` | DB username |
| `MYSQL_PASSWORD` | `VrgtSecure@2024` | DB password |
| `JWT_SECRET` | *(long default)* | JWT signing key — **change in production** |
| `QR_AES_KEY` | `VRGT2024AES256KY` | AES key for QR encryption (16 chars) |
| `MSG91_API_KEY` | *(blank)* | MSG91 API key for OTP delivery |

Switch to MySQL profile: `SPRING_PROFILES_ACTIVE=mysql`

---

## API Reference

Full interactive docs at: `http://localhost:8080/swagger-ui.html`

### Authentication

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/login` | Login → returns JWT |
| POST | `/api/auth/logout` | Clears JWT cookie |

### Visitor

| Method | Path | Description |
|---|---|---|
| POST | `/api/visitor/register` | Pre-register visitor (public) |
| POST | `/api/visitor/verify-otp` | Verify OTP → generates QR |
| GET | `/api/visitor/all` | All visitors |
| GET | `/api/visitor/active` | Active visitors |
| GET | `/api/visitor/queue` | Reception queue |
| GET | `/api/visitor/{id}` | Visitor by ID |
| POST | `/api/visitor/{id}/checkout` | Check out visitor |
| POST | `/api/visitor/{id}/resend-otp` | Resend OTP |

### Gate

| Method | Path | Description |
|---|---|---|
| POST | `/api/gate/scan-qr` | Validate QR at gate |
| POST | `/api/gate/verify-id` | Record ID verification |
| GET | `/api/gate/logs/today` | Today's gate logs |

### Reception

| Method | Path | Description |
|---|---|---|
| GET | `/api/reception/queue` | Live reception queue |
| POST | `/api/reception/checkin` | Check in + assign room |
| POST | `/api/reception/auto-assign-room` | Auto-assign nearest room |
| POST | `/api/reception/start-route/{id}` | Start visitor route |

### Tracking

| Method | Path | Description |
|---|---|---|
| POST | `/api/tracking/location` | Record location event |
| GET | `/api/tracking/all-locations` | Current positions |
| GET | `/api/tracking/history/{id}` | Movement history |
| GET | `/api/tracking/heatmap` | Location heatmap data |

### Meeting Rooms

| Method | Path | Description |
|---|---|---|
| GET | `/api/rooms` | All rooms |
| GET | `/api/rooms/available` | Available rooms |
| POST | `/api/rooms/meeting/{id}/start` | Start meeting |
| POST | `/api/rooms/meeting/{id}/extend` | Extend meeting |
| POST | `/api/rooms/meeting/{id}/end` | End meeting |

### Emergency

| Method | Path | Description |
|---|---|---|
| POST | `/api/emergency/activate` | Activate emergency protocol |
| POST | `/api/emergency/{id}/resolve` | Resolve emergency |
| POST | `/api/emergency/{id}/evacuation-update` | Update evacuation count |
| GET | `/api/emergency/status` | Visitor evacuation status |
| GET | `/api/emergency/active` | Active emergencies |
| GET | `/api/emergency/history` | Emergency history |

### Admin

| Method | Path | Description |
|---|---|---|
| GET | `/api/admin/dashboard-stats` | KPI statistics |
| GET | `/api/admin/analytics/visitors` | Visitor trend |
| GET | `/api/admin/audit-logs` | Audit trail |
| GET | `/api/admin/users` | Staff users |
| POST | `/api/admin/users` | Create user |
| PUT | `/api/admin/users/{id}` | Update user |
| GET | `/api/admin/export/visitors/pdf` | Export PDF |
| GET | `/api/admin/export/visitors/excel` | Export Excel |

---

## WebSocket Topics

Connect: `ws://localhost:8080/ws` (SockJS + STOMP)

| Topic | Description |
|---|---|
| `/topic/gate-events` | Gate entry events |
| `/topic/reception-queue` | Reception queue updates |
| `/topic/tracking` | Live visitor position updates |
| `/topic/notifications` | System notifications |
| `/topic/rooms` | Room status changes |
| `/topic/emergency` | Emergency alerts & updates |

---

## Database Schema

See [`database/schema.sql`](database/schema.sql) for the complete MySQL DDL.

Key tables:

| Table | Purpose |
|---|---|
| `app_users` | Staff accounts (Admin, Guard, Receptionist, Host) |
| `visitors` | Visitor registrations, QR, OTP, status |
| `blacklist_entries` | Blocked visitors |
| `gate_logs` | Gate scan events |
| `meeting_rooms` | Room inventory |
| `meetings` | Meeting sessions |
| `location_events` | Real-time positioning |
| `emergency_logs` | Emergency activations |
| `notifications` | In-app notifications |
| `audit_logs` | Full audit trail |
| `alerts` | Security / zone alerts |

---

## Project Structure

```
src/main/java/com/example/Application/
├── Application.java                    ← Spring Boot entry point
├── config/
│   ├── DataInitializer.java            ← Seed default users + rooms
│   ├── JwtAuthFilter.java              ← JWT request filter
│   ├── JwtUtil.java                    ← Token generation / validation
│   ├── Msg91Properties.java            ← MSG91 config binding
│   ├── OpenApiConfig.java              ← Swagger / OpenAPI setup
│   ├── SecurityConfig.java             ← HTTP security rules
│   └── WebSocketConfig.java            ← STOMP endpoint config
├── controller/
│   ├── AdminApiController.java
│   ├── AuthController.java
│   ├── BlacklistController.java
│   ├── EmergencyController.java        ← NEW
│   ├── GateController.java
│   ├── MeetingRoomController.java
│   ├── PageController.java             ← Thymeleaf page routes
│   ├── ReceptionController.java
│   ├── TrackingController.java
│   └── VisitorController.java
├── dto/
│   ├── ApiResponseDTO.java
│   ├── DashboardStatsDTO.java
│   ├── LoginRequestDTO.java
│   └── LoginResponseDTO.java
├── entity/
│   ├── AppUser.java
│   ├── AuditLog.java
│   ├── BlacklistEntry.java
│   ├── EmergencyLog.java               ← NEW
│   ├── GateLog.java
│   ├── LocationEvent.java
│   ├── Meeting.java
│   ├── MeetingRoom.java
│   ├── Notification.java
│   └── Visitor.java
├── enums/
│   ├── AlertType.java
│   ├── RoomStatus.java
│   ├── UserRole.java
│   ├── VisitorLocation.java
│   └── VisitorStatus.java
├── repository/  (one per entity)
└── service/
    ├── AnalyticsService.java
    ├── AuditService.java
    ├── EmergencyService.java           ← NEW
    ├── ExportService.java
    ├── GateService.java
    ├── MeetingRoomService.java
    ├── Msg91Service.java
    ├── NotificationService.java
    ├── QRCodeService.java
    ├── ReceptionService.java
    ├── TrackingService.java
    └── VisitorService.java

src/main/resources/
├── application.properties              ← Dev (H2) config
├── application-mysql.properties        ← Production MySQL config
├── static/
│   ├── css/styles.css                  ← Design system (dark glassmorphism)
│   └── js/app.js                       ← Core JS (API, Toast, WS, helpers)
└── templates/
    ├── admin.html                      ← Admin dashboard + analytics
    ├── emergency.html                  ← Emergency control centre (NEW)
    ├── gate-kiosk.html                 ← Security gate QR scanner
    ├── indoor-map.html                 ← Animated indoor navigation
    ├── login.html                      ← JWT login
    ├── meeting-rooms.html              ← Room management
    ├── notifications.html              ← Notification centre
    ├── reception.html                  ← Reception queue
    ├── register.html                   ← Visitor pre-registration (3-step)
    └── tracking.html                   ← Live visitor tracking

database/
└── schema.sql                          ← Complete MySQL 8 DDL + seed data

docker/
└── nginx.conf                          ← Nginx reverse proxy config

Dockerfile
docker-compose.yml
.env.example
```

---

## Default Credentials

| Username | Password | Role | Start page |
|---|---|---|---|
| `admin` | `admin123` | Admin | `/admin` |
| `guard1` | `guard123` | Security Guard | `/gate` |
| `reception1` | `reception123` | Receptionist | `/reception` |
| `host1` | `host123` | Host | `/tracking` |

---

## Testing Strategy

### Unit Tests
- `QRCodeServiceTest` — encrypt/decrypt round-trip
- `VisitorServiceTest` — blacklist check, duplicate detection, OTP expiry
- `JwtUtilTest` — token generation and validation

### Integration Tests
- `VisitorFlowIntegrationTest` — full registration → OTP → QR flow with H2
- `GateIntegrationTest` — QR scan → gate log → WebSocket event
- `EmergencyIntegrationTest` — activate → evacuation update → resolve

### Manual Test Flows

**Happy path:**
1. Register at `/register` with any email/phone
2. Copy the OTP from the API response (dev mode — real OTP in MSG91 mode)
3. Complete OTP verification → QR image displayed
4. Login as `guard1` → paste QR string → scan
5. Login as `reception1` → see visitor in queue → check in → assign room
6. Login as `admin` → `/admin` → confirm visitor appears in table and charts

**Emergency drill:**
1. Login as `admin` → `/emergency`
2. Select **Drill** type → severity **Low** → Activate
3. Open a second tab on any dashboard — see the live banner appear
4. Return to emergency page → mark visitors safe → Resolve

### Run tests
```bash
./mvnw test
./mvnw test -Dtest=VisitorServiceTest
```
