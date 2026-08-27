# PRD: PadiBot — Smart Rice Planter v1.1
## Aplikasi Mobile + Mesin Tanam Padi Otomatis dengan Coverage Path Planning

**Version:** 1.1 (Refined dari draft v0.1)  
**Last Updated:** August 2026  
**Status:** Ready for Phase 1 MVP Development  
**Primary Language:** Indonesian (UI + Documentation)  

---

## 📋 PROJECT OVERVIEW

### Vision
Create a practical precision-agriculture platform that makes automated rice planting accessible for Indonesian farmers through an intuitive mobile application integrated with autonomous planting machinery.

### Problem Statement
Indonesian rice farmers face:
- Repetitive manual planting labor with inconsistent coverage
- Difficulty maintaining uniform planting lanes in irregular field shapes
- Lack of digital visibility into planting progress and field records
- No systematic way to optimize planting routes for efficiency

### Solution
PadiBot adalah sistem integrated yang memungkinkan petani untuk:
1. **Map** sawah mereka secara digital
2. **Generate** rute tanam optimal otomatis (coverage path planning)
3. **Control & Monitor** mesin tanam secara real-time
4. **Track** progress dan laporan historis

### Key Differentiator
**Coverage Path Planning:** Algoritma yang mengkonversi batas sawah menjadi rute tanam sistematis (boustrophedon/lawn-mower pattern) dengan minimal overlap dan missed areas.

### Target Users
| User Type | Primary Needs | Constraints |
|-----------|---------------|-------------|
| **Farmer / Machine Operator** | Simple UI, large controls, clear status, emergency stop | Outdoor use, limited tech literacy, time-sensitive |
| **Technician** (Fase 2) | Hardware diagnostics, sensor status, calibration | Technical knowledge available |
| **Farm Manager** (Fase 3) | Multiple fields, fleet management, analytics | Admin dashboard required |

---

## 🎯 CORE FEATURES (MVP v1)

### F1: Field Mapping & Management
- [x] Create polygon field boundary manually (point-by-point)
- [x] View field area & perimeter calculations
- [x] Save multiple field profiles
- [x] Basic field validation (min size, valid polygon)

**Excluded from MVP v1:** Walk & Map, Drive & Map, GPS-based auto-mapping → Move to Phase 2

### F2: Route Generation (Coverage Path Planning)
- [x] Input machine parameters (working width)
- [x] Input headland/turning area width
- [x] Auto-generate parallel planting lanes (boustrophedon pattern)
- [x] Preview route on map visualization
- [x] Adjust lane orientation if needed
- [x] Display estimated field coverage %

**Excluded from MVP v1:** Obstacle avoidance, complex headland strategies → Move to Phase 2.5

### F3: Mission Control & Execution
- [x] Start mission to machine
- [x] Pause/Resume mission
- [x] Emergency Stop
- [x] View real-time machine position
- [x] View progress (completed vs remaining lanes)
- [x] Basic telemetry display (battery, GPS status, speed)

**Excluded from MVP v1:** Recovery from specific waypoint, complex state transitions → Move to Phase 2

### F4: Machine Connection & Communication
- [x] Connect to machine (Simulator for MVP)
- [x] Display connection status
- [x] Bluetooth/WiFi connection simulation
- [x] Upload mission to machine
- [x] Receive telemetry updates
- [x] Manual control mode (basic directional controls)

**Excluded from MVP v1:** Real Bluetooth/WiFi/GSM → Move to Phase 1+

### F5: Monitoring & Logging
- [x] Real-time progress indicator
- [x] Telemetry visualization (battery %, GPS accuracy)
- [x] Error/warning logging
- [x] Mission history list with basic metadata

**Excluded from MVP v1:** Cloud sync, push notifications, detailed analytics → Move to Phase 3

### F6: Simulator Mode
- [x] Virtual rice field and machine
- [x] Simulated GPS movement along route
- [x] Simulated sensor data (battery drain, GPS accuracy)
- [x] Error injection (GPS loss, connection drop, low battery)
- [x] Fully offline-capable

---

## 📱 USER FLOWS

### Flow A: First Launch & Setup
```
Open App
  ↓
Splash/Onboarding Screen
  ↓
Dashboard
  ↓
Select "Mulai Misi Baru"
```

### Flow B: Create & Save Field
```
Dashboard
  ↓
Tap "Pemetaan Sawah"
  ↓
Tap "Tambah Lapangan Baru"
  ↓
Input Mode: "Manual (Titik Demi Titik)"
  ↓
Collect boundary points (click on map)
  ↓
Validate polygon (area > min, valid geometry)
  ↓
Input field name
  ↓
Save Field
  ↓
Return to Dashboard
```

### Flow C: Generate & Preview Route
```
Dashboard
  ↓
Select Field
  ↓
Tap "Pengaturan Tanam"
  ↓
Input Machine Width: [e.g., 1.5m]
  ↓
Input Headland Width: [e.g., 3.0m]
  ↓
Tap "Generate Jalur"
  ↓
Preview Route (show lanes, start/end points)
  ↓
Optional: Adjust orientation (drag rotation slider)
  ↓
Tap "Approve Mission"
  ↓
Navigate to "Jalur Otomatis" screen
```

### Flow D: Execute Mission
```
Mission Screen
  ↓
Display pre-flight checklist (GPS fix, battery level, connection status)
  ↓
Tap "Mulai Misi"
  ↓
Machine start planting
  ↓
App receives telemetry every 1-5 seconds
  ↓
User can: Pause, Resume, or Emergency Stop
  ↓
Upon completion: Show summary (area planted, time taken, coverage %)
```

### Flow E: Manual Control (For Setup/Testing)
```
Dashboard
  ↓
Select "Kontrol Manual"
  ↓
Display: Forward, Backward, Left, Right buttons
  ↓
Display: Speed slider
  ↓
User sends commands
  ↓
Machine responds locally
  ↓
Tap "Kembali" to exit manual mode
```

---

## 🗄️ DATABASE SCHEMA (SQLite — Local Android)

> **Database Engine:** SQLite via `expo-sqlite` v14+ dengan Drizzle ORM  
> **Platform:** Android local storage — fully offline, no cloud required untuk MVP

### Primary Tables

#### Table: `fields`
| Column | Type (SQLite) | Description |
|--------|--------------|-------------|
| `id` | TEXT (UUID) | Primary key, generated client-side |
| `name` | TEXT NOT NULL | Field name (e.g., "Sawah Utama") |
| `boundary_json` | TEXT (JSON) | Field boundary sebagai JSON array of {lat, lon} points |
| `area_m2` | REAL | Calculated field area in m² |
| `perimeter_m` | REAL | Calculated perimeter in m |
| `created_at` | INTEGER (Unix ms) | Creation timestamp |
| `updated_at` | INTEGER (Unix ms) | Last update timestamp |

#### Table: `missions`
| Column | Type (SQLite) | Description |
|--------|--------------|-------------|
| `id` | TEXT (UUID) | Primary key |
| `field_id` | TEXT (FK) | Reference to fields.id |
| `name` | TEXT | Mission name |
| `status` | TEXT CHECK | 'DRAFT'\|'READY'\|'RUNNING'\|'PAUSED'\|'COMPLETED'\|'FAILED'\|'STOPPED' |
| `route_json` | TEXT (JSON) | Array of waypoints [{lat, lon, order, type, laneIndex}] |
| `machine_width_m` | REAL | Machine working width |
| `headland_width_m` | REAL | Headland/turning area width |
| `lane_orientation_deg` | REAL | Route orientation (0-360°) |
| `total_lanes` | INTEGER | Total number of planting lanes |
| `estimated_coverage_pct` | REAL | Expected coverage percentage |
| `actual_coverage_pct` | REAL | Actual coverage after mission |
| `started_at` | INTEGER (Unix ms) | Mission start timestamp |
| `completed_at` | INTEGER (Unix ms) | Mission completion timestamp |
| `created_at` | INTEGER (Unix ms) | Creation timestamp |

#### Table: `telemetry_points`
| Column | Type (SQLite) | Description |
|--------|--------------|-------------|
| `id` | INTEGER (PK AUTOINCREMENT) | Primary key |
| `mission_id` | TEXT (FK) | Reference to missions.id |
| `timestamp` | INTEGER (Unix ms) | Data timestamp |
| `position_lat` | REAL | Current latitude |
| `position_lon` | REAL | Current longitude |
| `position_accuracy_m` | REAL | GPS accuracy in meters |
| `battery_pct` | REAL | Battery percentage (0-100) |
| `speed_mps` | REAL | Speed in m/s |
| `heading_deg` | REAL | Heading in degrees (0-360) |
| `gps_status` | TEXT | 'GPS'\|'RTK'\|'DGPS'\|'FLOAT'\|'NONE' |
| `mission_progress_pct` | REAL | Mission completion % |
| `current_lane_index` | INTEGER | Current lane being planted |
| `total_lanes` | INTEGER | Total lanes in route |

#### Table: `mission_events`
| Column | Type (SQLite) | Description |
|--------|--------------|-------------|
| `id` | INTEGER (PK AUTOINCREMENT) | Primary key |
| `mission_id` | TEXT (FK) | Reference to missions.id |
| `event_type` | TEXT CHECK | 'START'\|'PAUSE'\|'RESUME'\|'STOP'\|'ERROR'\|'WARNING'\|'COMPLETED' |
| `message` | TEXT | Event description |
| `severity` | TEXT CHECK | 'INFO'\|'WARNING'\|'CRITICAL' |
| `timestamp` | INTEGER (Unix ms) | Event timestamp |

### Drizzle ORM Schema (TypeScript)
```typescript
// db/schema.ts
import { sqliteTable, text, real, integer } from 'drizzle-orm/sqlite-core';

export const fields = sqliteTable('fields', {
  id: text('id').primaryKey(), // UUID
  name: text('name').notNull(),
  boundaryJson: text('boundary_json').notNull(), // JSON stringified GeoPoint[]
  areaM2: real('area_m2'),
  perimeterM: real('perimeter_m'),
  createdAt: integer('created_at', { mode: 'timestamp_ms' }),
  updatedAt: integer('updated_at', { mode: 'timestamp_ms' }),
});

export const missions = sqliteTable('missions', {
  id: text('id').primaryKey(),
  fieldId: text('field_id').references(() => fields.id),
  name: text('name'),
  status: text('status').default('DRAFT'), // DRAFT|READY|RUNNING|PAUSED|COMPLETED|FAILED|STOPPED
  routeJson: text('route_json'),           // JSON stringified Waypoint[]
  machineWidthM: real('machine_width_m'),
  headlandWidthM: real('headland_width_m'),
  laneOrientationDeg: real('lane_orientation_deg'),
  totalLanes: integer('total_lanes'),
  estimatedCoveragePct: real('estimated_coverage_pct'),
  actualCoveragePct: real('actual_coverage_pct'),
  startedAt: integer('started_at', { mode: 'timestamp_ms' }),
  completedAt: integer('completed_at', { mode: 'timestamp_ms' }),
  createdAt: integer('created_at', { mode: 'timestamp_ms' }),
});

export const telemetryPoints = sqliteTable('telemetry_points', {
  id: integer('id', { mode: 'number' }).primaryKey({ autoIncrement: true }),
  missionId: text('mission_id').references(() => missions.id),
  timestamp: integer('timestamp', { mode: 'timestamp_ms' }),
  positionLat: real('position_lat'),
  positionLon: real('position_lon'),
  positionAccuracyM: real('position_accuracy_m'),
  batteryPct: real('battery_pct'),
  speedMps: real('speed_mps'),
  headingDeg: real('heading_deg'),
  gpsStatus: text('gps_status'),
  missionProgressPct: real('mission_progress_pct'),
  currentLaneIndex: integer('current_lane_index'),
  totalLanes: integer('total_lanes'),
});

export const missionEvents = sqliteTable('mission_events', {
  id: integer('id', { mode: 'number' }).primaryKey({ autoIncrement: true }),
  missionId: text('mission_id').references(() => missions.id),
  eventType: text('event_type'),
  message: text('message'),
  severity: text('severity'),
  timestamp: integer('timestamp', { mode: 'timestamp_ms' }),
});
```


---

## 🔌 API ENDPOINTS (Backend)

### Authentication & Session
```
POST   /api/auth/login              → {email, password} → {token, user}
POST   /api/auth/logout             → {token} → {success}
GET    /api/auth/session            → {token} → {user, permissions}
```

### Field Management
```
GET    /api/fields                  → List all fields
POST   /api/fields                  → {name, boundary} → {field}
GET    /api/fields/:id              → {field}
PUT    /api/fields/:id              → {name, boundary} → {field}
DELETE /api/fields/:id              → {success}
```

### Mission Management
```
GET    /api/missions                → List missions (paginated)
POST   /api/missions                → {field_id, machine_width, headland_width, lane_orientation} → {mission}
GET    /api/missions/:id            → {mission, route_preview}
PUT    /api/missions/:id            → {status, ...updates} → {mission}
DELETE /api/missions/:id            → {success}
```

### Route Generation
```
POST   /api/routes/generate         → {field_id, machine_width_m, headland_width_m, orientation_deg} → {route, coverage_pct, estimated_distance_m}
POST   /api/routes/validate         → {field_polygon, route} → {valid, coverage_pct, uncovered_areas}
```

### Mission Execution
```
POST   /api/missions/:id/start      → {mission_id} → {success, mission_state}
POST   /api/missions/:id/pause      → {mission_id} → {success, mission_state}
POST   /api/missions/:id/resume     → {mission_id} → {success, mission_state}
POST   /api/missions/:id/stop       → {mission_id} → {success, mission_state}
POST   /api/missions/:id/emergency-stop → {mission_id} → {success, mission_state}
```

### Machine Connection & Control
```
POST   /api/machine/connect         → {connection_type} → {status, device_info}
GET    /api/machine/status          → {status, telemetry, last_update}
POST   /api/machine/upload-mission  → {mission_id} → {success, mission_hash}
POST   /api/machine/manual-control  → {command, intensity} → {executed}
```

### Telemetry & Monitoring
```
WS     /ws/missions/:id/telemetry   → Real-time telemetry stream
GET    /api/missions/:id/telemetry  → List historical telemetry
POST   /api/missions/:id/telemetry  → {telemetry_point} → {recorded}
```

### Simulator Control
```
POST   /api/simulator/inject-error  → {error_type, duration_ms} → {success}
GET    /api/simulator/status        → {current_state, virtual_position}
```

---

## 🔐 AUTENTIKASI & OTORISASI

### MVP Phase 1 (Offline — No Auth Required)
- **MVP:** Tidak ada autentikasi (single-user, app digunakan oleh 1 petani/1 device)
- **Data:** Semua data disimpan lokal di SQLite device
- **Privacy:** App data private di app sandbox Android

### Phase 3+ (Cloud Sync — Autentikasi Diperlukan)

#### User Roles
| Role | Permissions | Scope |
|------|-------------|-------|
| **Farmer** | View own fields, create missions, control machine | Own fields only |
| **Technician** | All farmer perms + diagnostics + calibration | Own + assigned fields |
| **Admin** | Full access | All fields, all machines |

#### Session Management
- **Method:** JWT tokens (disimpan di `expo-secure-store`, BUKAN AsyncStorage)
- **Token Expiry:** 7 hari
- **Refresh:** Auto-refresh sebelum expired
- **Logout:** Clear secure token, redirect ke login screen

#### Security Requirements (Phase 3+ Cloud)
- HTTPS only untuk semua cloud API calls
- CORS configured (allow only known origins)
- Rate limiting (10 req/sec per user)
- Input validation di semua endpoints (Zod schemas)
- Sensitive data di `expo-secure-store` (bukan AsyncStorage)

---

## 🛠️ TECH STACK

### Mobile App (Android-First)
- **Target Platform:** Android 8.0+ (API Level 26+) — Smartphone Petani
- **Framework:** React Native + Expo (SDK 51+)
- **Language:** TypeScript (strict mode)
- **Navigation:** Expo Router v3 (file-based routing)
- **State Management:** Zustand
- **Data Fetching:** TanStack Query (React Query)
- **Mapping:** React Native Maps (untuk preview sawah)
- **Local Database:** SQLite via `expo-sqlite` (offline-first, no backend required)
- **ORM (Local):** Drizzle ORM + drizzle-orm/expo-sqlite
- **Location:** Expo Location (GPS, Barometer)
- **UI Components:** Custom Components + React Native Paper
- **Styling:** StyleSheet + React Native Reanimated
- **Icons:** Expo Vector Icons (MaterialCommunityIcons)
- **Validation:** Zod
- **Testing:** Jest + React Native Testing Library
- **Bluetooth (Phase 1):** `react-native-ble-plx` — koneksi ke Arduino via Bluetooth
- **WiFi (Phase 1):** Built-in HTTP/WebSocket — koneksi ke Arduino ESP8266/ESP32 via WiFi
- **GSM 4G (Phase 2+):** MQTT over cellular — koneksi remote via SIM800L/SIM7600
- **Build:** Expo EAS Build (APK/AAB untuk Android)

### Arduino Hardware Layer
- **Mikrokontroler:** Arduino Uno / Mega / Nano (logic utama mesin)
- **Modul WiFi:** ESP8266 (NodeMCU) atau ESP32 — HTTP server + WebSocket server
- **Modul Bluetooth:** HC-05 / HC-06 (Bluetooth Classic) atau HM-10 (BLE)
- **Modul GSM 4G:** SIM800L (2G/GPRS) atau SIM7600 (4G LTE) — untuk jangkauan jauh
- **GPS Module:** u-blox NEO-6M/7M/8M — posisi mesin di lapangan
- **Motor Driver:** L298N / IBT-2 — kontrol motor penggerak
- **Firmware Language:** Arduino C/C++ (Arduino IDE / PlatformIO)
- **Communication Protocol:** JSON over Serial → WiFi/BT/GSM
- **Rationale:** Arduino dipilih karena ekosistem luas, modul murah, dan banyak tersedia di Indonesia

### Local Database (SQLite — Android)
- **Engine:** SQLite via `expo-sqlite` v14+
- **ORM:** Drizzle ORM (`drizzle-orm` + `drizzle-orm/expo-sqlite`)
- **Migration:** Drizzle Kit (`drizzle-kit push` atau migration files)
- **Data Location:** App-private SQLite file (`/data/data/<package>/databases/padibot.db`)
- **Backup:** Export JSON ke local storage atau shared storage Android
- **Rationale:** 100% offline operation, no internet required untuk petani di lapangan

### Backend (Opsional — Phase 3+)
- **Runtime:** Node.js 20+
- **Framework:** Express.js atau Fastify
- **Language:** TypeScript (strict mode)
- **Database:** PostgreSQL + PostGIS (untuk cloud sync & fleet analytics)
- **ORM:** Prisma atau Drizzle ORM
- **API:** RESTful + WebSocket untuk real-time telemetry
- **Authentication:** JWT + Better Auth
- **Validation:** Zod
- **Logging:** Winston atau Pino
- **Testing:** Jest + Supertest
- **Deployment:** Docker + Google Cloud Run
- **Note:** Backend TIDAK diperlukan untuk MVP Phase 1 (fully offline dengan SQLite)

### Infrastructure
- **Version Control:** Git (GitHub)
- **CI/CD:** GitHub Actions + Expo EAS (untuk build APK)
- **Database (Cloud — Phase 3):** Supabase (PostgreSQL + PostGIS)
- **App Distribution:** Google Play Store / EAS Update (OTA update)
- **Error Tracking:** Sentry (react-native-sentry)
- **Analytics:** Firebase Analytics (opsional)

---

## 📊 MACHINE COMMUNICATION PROTOCOL

> **Hardware Target:** Arduino (Uno/Mega/Nano) dengan modul komunikasi terpisah.  
> App Android berkomunikasi dengan Arduino melalui 3 metode: **WiFi**, **Bluetooth**, atau **GSM 4G**.

### Connection Abstraction (TypeScript Interface)
```typescript
// services/machineProtocol/MachineConnection.ts
interface MachineConnection {
  connect(config: ConnectionConfig): Promise<void>;
  disconnect(): Promise<void>;
  getStatus(): Promise<MachineStatus>;
  isConnected(): boolean;

  uploadMission(mission: Mission): Promise<void>;
  startMission(missionId: string): Promise<void>;
  pauseMission(missionId: string): Promise<void>;
  resumeMission(missionId: string): Promise<void>;
  stopMission(missionId: string): Promise<void>;
  emergencyStop(): Promise<void>;

  sendManualCommand(cmd: ManualCommand): Promise<void>;
  getTelemetry(): Promise<Telemetry>;

  onTelemetry(callback: (data: Telemetry) => void): void;
  onError(callback: (error: Error) => void): void;
  onStatusChange(callback: (status: MachineStatus) => void): void;
}

// Implementasi tersedia untuk:
// - SimulatorConnection   → MVP (tanpa hardware)
// - WiFiConnection        → Phase 1 (ESP8266/ESP32 HTTP + WebSocket)
// - BluetoothConnection   → Phase 1 (HC-05/HC-06/HM-10 via BLE PLX)
// - GSMConnection         → Phase 2+ (SIM800L/SIM7600 via MQTT)
```

### Message Format (JSON over Serial/Network)
```json
{
  "cmd": "START_MISSION",
  "id": 1,
  "payload": {
    "missionId": "abc-123",
    "waypoints": [
      { "lat": -6.9234, "lon": 107.6100, "order": 0 },
      { "lat": -6.9240, "lon": 107.6108, "order": 1 }
    ],
    "machineWidth": 1.5,
    "speed": 0.75
  }
}
```

Arduino merespons dengan:
```json
{
  "status": "OK",
  "id": 1,
  "data": { "state": "RUNNING" }
}
```

Telemetry dikirim Arduino setiap 1 detik:
```json
{
  "type": "TELEMETRY",
  "lat": -6.9237,
  "lon": 107.6103,
  "accuracy": 1.5,
  "battery": 82,
  "speed": 0.73,
  "heading": 90,
  "gpsStatus": "GPS",
  "progress": 42,
  "lane": 5,
  "totalLanes": 12
}
```

### Connection Types — Arduino

| Tipe | Phase | Modul Arduino | Library Android | Jarak | Latensi |
|------|-------|--------------|----------------|-------|---------|
| **Simulator** | MVP (Phase 1) | — (virtual) | Built-in | — | Instant |
| **WiFi** | Phase 1 | ESP8266 / ESP32 | `fetch` + WebSocket | ~50–100m | 10–50ms |
| **Bluetooth** | Phase 1 | HC-05 / HC-06 / HM-10 (BLE) | `react-native-ble-plx` | ~10–30m | 50–200ms |
| **GSM 4G** | Phase 2+ | SIM800L / SIM7600 | MQTT (`async-mqtt`) | Unlimited | 100–500ms |

### WiFi Connection (ESP8266 / ESP32)
```
Topologi: Android ← HTTP/WebSocket → ESP8266/ESP32 ← Serial → Arduino

Arduino Setup:
├── ESP8266/ESP32 sebagai WiFi Access Point (hotspot) ATAU join WiFi router
├── HTTP Server: GET /status, POST /command
├── WebSocket Server: ws://192.168.x.x:81 (untuk telemetry stream)
├── Baud rate Serial: 115200
└── Format: JSON newline-delimited

Android Connection:
├── User input IP address Arduino di settings app
├── Fetch: POST http://{ip}/command {cmd, payload}
├── WebSocket: ws://{ip}:81 → receive telemetry real-time
└── Reconnect otomatis setiap 3 detik bila terputus

Keunggulan: Latensi rendah, bandwidth tinggi, reliable dalam jangkauan
```

### Bluetooth Connection (HC-05 / HC-06 / HM-10 BLE)
```
Topologi: Android ← BLE/SPP → HC-05/HC-06 ← Serial → Arduino

HC-05/HC-06 (Bluetooth Classic SPP):
├── Library Android: react-native-ble-plx (BLE) atau
│   react-native-bluetooth-serial (Classic)
├── Pairing: PIN "1234" (default HC-05)
├── Baud rate: 9600 atau 115200
└── Format: JSON string diakhiri '\n'

HM-10 (BLE):
├── Service UUID: FFE0
├── Characteristic UUID: FFE1
├── Library: react-native-ble-plx
└── Format: JSON chunks (max 20 byte per packet, dirangkai)

Keunggulan: Tidak perlu router WiFi, koneksi langsung smartphone ↔ mesin
```

### GSM 4G Connection (SIM800L / SIM7600)
```
Topologi: Android ← MQTT/Internet → Broker ← MQTT/GSM → SIM7600 ← Serial → Arduino

MQTT Broker: HiveMQ Cloud / Mosquitto / EMQX
Topics:
├── padibot/{deviceId}/command   → Android PUBLISH → Arduino SUBSCRIBE
├── padibot/{deviceId}/telemetry → Arduino PUBLISH → Android SUBSCRIBE
└── padibot/{deviceId}/status    → Arduino PUBLISH → Android SUBSCRIBE

SIM7600 AT Commands:
├── AT+CMQTTSTART      → Mulai MQTT
├── AT+CMQTTCONN       → Koneksi ke broker
├── AT+CMQTTPUB        → Publish telemetry
└── AT+CMQTTSUB        → Subscribe command topic

Keunggulan: Jangkauan unlimited (selama ada sinyal seluler), cocok untuk monitoring remote
```

### Arduino Firmware Skeleton (C/C++)
```cpp
// arduino/PadiBot_Firmware/PadiBot_Firmware.ino
#include <ArduinoJson.h>
#include <SoftwareSerial.h>

// Pilih salah satu mode koneksi:
// #define MODE_WIFI       // ESP8266/ESP32 via Serial2
// #define MODE_BLUETOOTH  // HC-05/HC-06 via Serial1
// #define MODE_GSM        // SIM7600 via Serial3

void setup() {
  Serial.begin(115200);  // Debug
  // initConnection();   // Init WiFi / BT / GSM
  // initMotors();
  // initGPS();
}

void loop() {
  // handleIncomingCommand();
  // sendTelemetry();    // Setiap 1 detik
  // updateMotors();
}

void handleCommand(const char* json) {
  StaticJsonDocument<512> doc;
  deserializeJson(doc, json);
  const char* cmd = doc["cmd"];

  if (strcmp(cmd, "START_MISSION") == 0) startMission(doc["payload"]);
  else if (strcmp(cmd, "PAUSE") == 0)    pauseMission();
  else if (strcmp(cmd, "RESUME") == 0)   resumeMission();
  else if (strcmp(cmd, "STOP") == 0)     stopMission();
  else if (strcmp(cmd, "E_STOP") == 0)   emergencyStop();  // Prioritas tertinggi
  else if (strcmp(cmd, "MANUAL") == 0)   manualControl(doc["payload"]);
}

void sendTelemetry() {
  StaticJsonDocument<256> doc;
  doc["type"] = "TELEMETRY";
  doc["lat"] = gps.location.lat();
  doc["lon"] = gps.location.lng();
  doc["battery"] = getBatteryPct();
  doc["speed"] = getSpeed();
  doc["progress"] = getMissionProgress();
  // Kirim ke modul komunikasi aktif
  serializeJson(doc, Serial);  // atau Serial1/2/3
  Serial.println();
}
```


---

## 🚨 MISSION STATE MACHINE

```
┌─────────────────────────────────────────────────────────────┐
│                   MISSION STATE LIFECYCLE                   │
└─────────────────────────────────────────────────────────────┘

  DRAFT
    │
    ├─→ READY (mission approved, ready to execute)
    │
    ├─→ DELETED (mission cancelled)

  READY
    │
    ├─→ RUNNING (mission started)
    │
    └─→ DELETED

  RUNNING
    │
    ├─→ PAUSED (pause button pressed)
    │
    ├─→ STOPPED (stop button pressed, user abort)
    │
    ├─→ ERROR (connection loss, sensor failure)
    │
    └─→ COMPLETED (all waypoints reached)

  PAUSED
    │
    ├─→ RUNNING (resume pressed)
    │
    └─→ STOPPED (stop pressed)

  COMPLETED / STOPPED / ERROR
    │
    └─→ [Final state - can only view history]
```

### State Transition Rules
- Only RUNNING can transition to COMPLETED
- PAUSED can only resume or stop
- Any state can ERROR on connection loss
- COMPLETED/STOPPED/ERROR cannot change again
- Emergency Stop: Any state → STOPPED immediately

---

## ✅ MVP ACCEPTANCE CRITERIA

### Must-Have (Phase 1 MVP)
- [ ] User dapat membuat field boundary
- [ ] User dapat generate route otomatis (boustrophedon)
- [ ] Route preview menampilkan lanes dan coverage %
- [ ] User dapat start/pause/resume/stop mission di simulator
- [ ] App display real-time progress & telemetry
- [ ] Emergency stop tersedia dan responsive
- [ ] App dapat bekerja offline (simulator mode)
- [ ] Semua screens dalam Indonesian language
- [ ] Responsive design untuk portrait orientation
- [ ] Manual controls (forward/backward/left/right) working

### Nice-to-Have (Phase 2+)
- [ ] Walk & Map atau Drive & Map untuk field boundary
- [ ] Obstacle/exclusion zones dalam route generation
- [ ] Multiple headland strategies
- [ ] Historical mission analytics & reports
- [ ] Cloud sync & remote monitoring
- [ ] Real Bluetooth/WiFi/GSM connectivity
- [ ] Multi-language support
- [ ] Dark mode

---

## 🚀 DEVELOPMENT PHASES

### Phase 1: MVP Foundation (Weeks 1-4)
**Goal:** Functional simulator-based system

#### Deliverables:
1. Monorepo structure (Expo + Node.js backend)
2. Dashboard screen (mock data)
3. Field list & CRUD operations
4. Planting settings form
5. Route preview (Canvas-based simple map)
6. Mission execution screen (simulator)
7. Telemetry monitoring
8. Basic route planner logic
9. Simulator machine connection
10. Unit tests untuk route planner (min 80% coverage)

**Tech:** React Native + Express + PostgreSQL + Simulator

---

### Phase 1+ (Weeks 5-6): Real Hardware Integration
**Goal:** Bluetooth/WiFi connectivity

#### Additions:
- Expo Bluetooth module integration
- Real connection protocol implementation
- ESP32 firmware skeleton
- Hardware state machine

---

### Phase 2: Field Mapping Enhancement (Weeks 7-10)
**Goal:** Multiple field mapping methods

#### Additions:
- Walk & Map dengan GPS tracking
- Drive & Map untuk large fields
- Boundary editing/refinement tools
- Field history & versioning

---

### Phase 2.5: Advanced Routing (Weeks 11-12)
**Goal:** Sophisticated coverage algorithms

#### Additions:
- Obstacle/exclusion zone support
- Multiple headland strategies (perpendicular, diagonal, curved)
- Adaptive lane spacing untuk irregular fields
- Route optimization (minimize turns, maximize coverage)

---

### Phase 3: Cloud & Analytics (Weeks 13-16)
**Goal:** Remote monitoring & fleet management

#### Additions:
- Cloud data sync
- Push notifications
- Analytics dashboard
- Multi-machine management
- Farm manager interface

---

### Phase 4: Advanced Autonomy (Future)
**Goal:** Full autonomous operation

#### Additions:
- RTK-GNSS integration
- LiDAR obstacle detection
- On-board computer (Jetson/Pi)
- Vision-based lane following
- Fully autonomous obstacle avoidance

---

## 📈 SUCCESS METRICS

### MVP Phase 1
| Metric | Target | Measurement |
|--------|--------|-------------|
| Route generation success rate | >95% | Automated tests |
| Mission completion rate (simulator) | >98% | Test runs |
| Average coverage % | 90-98% | Route validation |
| App startup time | <3s | Performance profiling |
| Battery drain (during 1hr mission) | <20% | Simulator telemetry |
| Manual control response time | <200ms | Latency testing |

### Future Phases
| Metric | Target |
|--------|--------|
| Real hardware mission completion rate | >95% |
| Field coverage (actual) | 92-98% |
| Planting uniformity (overlap/gap ratio) | <5% |
| Manual intervention count | <1% of missions |
| System uptime | >99.5% |

---

## 🎓 OPEN TECHNICAL QUESTIONS

**Perlu dijawab sebelum Phase 1+:**

1. **Machine Specifications**
   - Exact machine type? (wheeled/tracked/modified transplanter)
   - Working width? (typically 1.2-2.0m)
   - Turning radius? (affects headland size)
   - Motor control interface? (PWM/CAN/Modbus)

2. **Positioning**
   - GPS accuracy requirement? (±5cm untuk RTK, ±2-5m untuk standard)
   - RTK GNSS available?
   - Wheel encoder for odometry?

3. **Sensors**
   - GNSS module type? (u-blox, Emlid, etc)
   - IMU required? (for heading/tilt)
   - LiDAR/Ultrasonic for obstacle detection?

4. **Communication**
   - Primary connection type? (Bluetooth, WiFi, GSM)
   - Data rate requirements? (1-5 Hz telemetry sufficient?)
   - Coverage area? (100m to km range)

5. **Safety**
   - Emergency stop mechanism? (hardware button on machine)
   - Watchdog timer requirement?
   - Failsafe behavior on connection loss?

---

## 📁 PROJECT STRUCTURE

```
padi-bot/
├── app/                                 # Expo Router v3 (file-based routing)
│   ├── (tabs)/
│   │   ├── index.tsx                    # Dashboard
│   │   ├── fields/
│   │   │   ├── index.tsx                # Field List
│   │   │   ├── [id].tsx                 # Field Detail
│   │   │   └── create.tsx               # Create Field
│   │   ├── history/
│   │   │   ├── index.tsx                # Mission History
│   │   │   └── [id].tsx                 # Mission Detail
│   │   └── settings.tsx                 # App Settings
│   │
│   ├── mission/
│   │   ├── planting-settings.tsx        # Machine Parameters
│   │   ├── route-preview.tsx            # Route Visualization
│   │   └── execution.tsx                # Mission Control
│   │
│   ├── manual-control.tsx               # Manual D-Pad Control
│   └── _layout.tsx                      # Root layout
│
├── components/                          # Reusable UI components
│   ├── ui/
│   │   ├── Button.tsx
│   │   ├── Card.tsx
│   │   ├── Badge.tsx
│   │   ├── ProgressBar.tsx
│   │   └── EmptyState.tsx
│   │
│   ├── dashboard/
│   │   ├── FieldCard.tsx
│   │   ├── MachineStatusBadge.tsx
│   │   └── QuickActions.tsx
│   │
│   ├── field/
│   │   ├── FieldBoundaryCanvas.tsx      # Canvas-based field preview
│   │   └── FieldListItem.tsx
│   │
│   ├── mission/
│   │   ├── RouteCanvas.tsx              # Route visualization canvas
│   │   ├── TelemetryDisplay.tsx
│   │   ├── MissionControls.tsx          # Start/Pause/Resume/Stop
│   │   └── EmergencyStopButton.tsx
│   │
│   └── manual/
│       └── DPadControls.tsx
│
├── db/                                  # SQLite database layer
│   ├── schema.ts                        # Drizzle ORM schema
│   ├── migrations/                      # DB migrations
│   ├── queries/
│   │   ├── fields.ts                    # Field CRUD queries
│   │   ├── missions.ts                  # Mission CRUD queries
│   │   └── telemetry.ts                 # Telemetry queries
│   └── index.ts                         # DB connection setup
│
├── services/                            # Business logic layer
│   ├── routePlanner/
│   │   ├── index.ts                     # generateCoverageRoute()
│   │   ├── geometry.ts                  # Polygon math utilities
│   │   └── __tests__/
│   │
│   ├── machineProtocol/
│   │   ├── MachineConnection.ts         # Interface definition
│   │   ├── SimulatorConnection.ts       # Simulator (MVP)
│   │   └── BluetoothConnection.ts       # Phase 1+
│   │
│   └── telemetry/
│       └── TelemetryRecorder.ts
│
├── stores/                              # Zustand global state
│   ├── machineStore.ts
│   ├── missionStore.ts
│   └── fieldStore.ts
│
├── hooks/                               # Custom React hooks
│   ├── useFields.ts
│   ├── useMissions.ts
│   └── useTelemetry.ts
│
├── constants/
│   ├── strings.ts                       # Indonesian UI strings
│   ├── theme.ts                         # Colors, fonts, spacing
│   └── defaults.ts                      # Default machine params
│
├── types/                               # TypeScript type definitions
│   ├── field.ts
│   ├── mission.ts
│   ├── telemetry.ts
│   └── machine.ts
│
├── docs/                                # Documentation
│   ├── design.md                        # UI/UX Design System
│   ├── architecture.md
│   ├── route-planning-algorithm.md
│   └── machine-protocol.md
│
├── arduino/                             # Arduino Firmware (C/C++)
│   ├── PadiBot_WiFi/                    # Firmware untuk koneksi WiFi (ESP8266/ESP32)
│   │   ├── PadiBot_WiFi.ino             # Main firmware
│   │   ├── config.h                     # WiFi SSID, password, port
│   │   ├── CommandHandler.cpp           # Parse & handle JSON commands
│   │   ├── TelemetrySender.cpp          # Kirim telemetry JSON ke app
│   │   ├── MotorControl.cpp             # Kontrol motor L298N/IBT-2
│   │   └── GPSReader.cpp                # Baca GPS NEO-6M/7M
│   │
│   ├── PadiBot_Bluetooth/               # Firmware untuk koneksi Bluetooth (HC-05/HM-10)
│   │   ├── PadiBot_Bluetooth.ino
│   │   ├── config.h                     # BT name, baud rate, PIN
│   │   ├── CommandHandler.cpp
│   │   ├── TelemetrySender.cpp
│   │   ├── MotorControl.cpp
│   │   └── GPSReader.cpp
│   │
│   ├── PadiBot_GSM/                     # Firmware untuk koneksi GSM 4G (SIM800L/SIM7600)
│   │   ├── PadiBot_GSM.ino
│   │   ├── config.h                     # APN, MQTT broker, device ID
│   │   ├── MQTTClient.cpp               # MQTT publish/subscribe
│   │   ├── CommandHandler.cpp
│   │   ├── TelemetrySender.cpp
│   │   ├── MotorControl.cpp
│   │   └── GPSReader.cpp
│   │
│   └── libraries/                       # Library Arduino yang dibutuhkan
│       ├── ArduinoJson/                 # Parsing JSON
│       ├── TinyGPS++/                   # Baca GPS NMEA
│       └── PubSubClient/                # MQTT (untuk GSM)
│
├── assets/
│   ├── images/
│   └── fonts/
│
├── app.json                             # Expo config
├── tsconfig.json
├── drizzle.config.ts                    # Drizzle Kit config
└── README.md
```


---

## 🧪 IMPLEMENTATION RULES

### Code Quality
1. **Strict TypeScript:** No `any`, explicit types everywhere
2. **Domain-Driven:** Separate domain logic dari UI/infrastructure
3. **Composition:** Reusable components & services (DRY principle)
4. **Testing:** Unit tests untuk business logic (min 80% coverage)
5. **Documentation:** JSDoc comments untuk public APIs

### Naming Conventions
- **Files:** camelCase untuk components/services, UPPER_CASE untuk constants
- **Functions:** verbNoun pattern (e.g., `generateRoute`, `calculateArea`)
- **Variables:** Descriptive names, avoid abbreviations (except ubiquitous ones like `id`, `lat`, `lon`)
- **Constants:** UPPER_SNAKE_CASE

### UI/UX
- **Language:** Indonesian untuk semua user-facing strings
- **Touch Targets:** Minimum 44x44 pixels
- **Font Size:** Minimum 16sp untuk outdoor readability
- **Contrast:** WCAG AA compliant (min 4.5:1 for text)
- **Loading:** Always show progress indicators
- **Errors:** Clear, actionable error messages in Indonesian

### Dependencies
- Keep external dependencies minimal
- Use popular, well-maintained libraries (e.g., lodash, date-fns, zod)
- Avoid multiple libraries for same purpose
- Document why each dependency is needed

---

## 🔄 NEXT STEPS

1. **Approve PRD v1.1** (revised untuk Android + SQLite)
2. **Buat `design.md`** — UI/UX Design System untuk screens Android
3. **Setup Expo project** dengan Expo Router + Drizzle ORM + expo-sqlite
4. **Begin Phase 1 implementation:**
   - Init SQLite schema dengan Drizzle ORM
   - Implement route planner service (boustrophedon algorithm)
   - Build UI screens (Dashboard, Field, Mission, Manual Control)
   - Integrate Simulator machine connection
   - Unit tests untuk route planner (>80% coverage)
5. **Build APK** via Expo EAS Build untuk testing di device petani

---

**Document Owner:** Adi Maryanto  
**Last Revision:** August 27, 2026  
**Next Review:** After Phase 1 MVP completion
