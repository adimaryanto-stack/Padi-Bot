# PadiBot MVP Roadmap
## Sprint-Based Development Plan (Weeks 1-6)

**Total Duration:** 6 weeks  
**Release Target:** PadiBot v0.1.0 (MVP with Simulator)  
**Team Size:** 1-2 developers  

---

## 📊 ROADMAP OVERVIEW

```
Week 1-2: Sprint 1 — Foundation & Route Planner
Week 2-3: Sprint 2 — Mobile UI & Dashboard  
Week 3-4: Sprint 3 — Mission Control & Simulator
Week 4-5: Sprint 4 — Integration & Polishing
Week 5-6: Sprint 5 — Testing & Documentation
```

---

## 🏃 SPRINT 1: Foundation & Route Planner
### Weeks 1-2 | Capacity: 80 hours

**Goal:** Setup project structure, implement core route planning logic, define data models

### Story 1.1: Project Setup & Architecture
**Story Points:** 8  
**Days:** 1-2  

**Tasks:**
- [ ] Create monorepo structure (pnpm workspaces)
- [ ] Setup package.json & TypeScript configs
- [ ] Configure ESLint + Prettier
- [ ] Setup GitHub repository with CI/CD skeleton
- [ ] Create README dengan setup instructions
- [ ] Document folder structure & contribution guidelines

**Definition of Done:**
- ✅ Monorepo buildable without errors
- ✅ TypeScript strict mode enabled
- ✅ Linting & formatting working
- ✅ README complete dengan setup steps

**Deliverables:**
```
padi-bot/
├── apps/
│   ├── mobile/
│   │   └── package.json (Expo config ready)
│   └── backend/
│       └── package.json (Express skeleton ready)
├── packages/
│   ├── domain/
│   ├── route-planner/
│   ├── machine-protocol/
│   └── ui/
├── tsconfig.json
├── pnpm-workspace.yaml
└── README.md
```

---

### Story 1.2: Domain Types & Data Models
**Story Points:** 5  
**Days:** 1  

**Tasks:**
- [ ] Define TypeScript interfaces untuk core entities:
  - `Field` (polygon, area, metadata)
  - `Mission` (field_id, route, status, telemetry)
  - `Waypoint` (lat, lon, order, status)
  - `Telemetry` (position, battery, speed, etc)
  - `MachineStatus` (connected, state, error)
- [ ] Create Zod validation schemas untuk setiap type
- [ ] Document type relationships & constraints

**Definition of Done:**
- ✅ All types properly typed & validated
- ✅ Schemas testable dengan Zod
- ✅ Export dari `@padi-bot/domain` package

**Deliverables:**
```typescript
// packages/domain/types/index.ts
export interface Field { ... }
export interface Mission { ... }
export interface Waypoint { ... }
export interface Telemetry { ... }
export const FieldSchema = z.object({...})
export const MissionSchema = z.object({...})
...
```

---

### Story 1.3: Route Planner Package
**Story Points:** 13  
**Days:** 3-4  

**Tasks:**
- [ ] Implement `generateCoverageRoute()` function:
  - Input: field boundary (GeoPoint[]), machine width, headland width, orientation
  - Output: waypoints array, coverage %, distance estimate
- [ ] Implement parallel line generation (boustrophedon)
- [ ] Implement polygon validation & area calculation
- [ ] Implement polygon clipping (keep lines inside boundary)
- [ ] Implement lane zig-zag ordering
- [ ] Add configurable lane orientation

**Algorithm Details:**

```typescript
interface RouteGenerationInput {
  fieldBoundary: GeoPoint[];      // Polygon vertices
  machineWidthM: number;            // Working width (e.g., 1.5m)
  headlandWidthM: number;           // Turning area width (e.g., 3.0m)
  orientationDeg?: number;          // Lane direction (0-360°)
}

interface Waypoint {
  lat: number;
  lon: number;
  order: number;
  type: 'lane' | 'turn' | 'headland';
  laneIndex?: number;
}

interface RouteResult {
  waypoints: Waypoint[];
  totalDistanceM: number;
  estimatedTurns: number;
  estimatedCoveragePct: number;
  uncoveredAreasM2: number[];
}

function generateCoverageRoute(input: RouteGenerationInput): RouteResult {
  // 1. Validate field polygon
  // 2. Calculate field area & offset by headland
  // 3. Generate parallel lines (working area)
  // 4. Clip lines to field boundaries
  // 5. Order lines in zig-zag pattern
  // 6. Generate turn waypoints between lines
  // 7. Calculate coverage metrics
  // 8. Return result
}
```

**Definition of Done:**
- ✅ All functions implemented & pure
- ✅ Unit tests written (min 80% coverage)
- ✅ Test cases cover: regular fields, irregular shapes, small fields, edge cases
- ✅ Documented dengan examples
- ✅ Performance acceptable (<100ms untuk typical field)

**Test Coverage:**
```
✓ Regular rectangular field
✓ Irregular polygon field
✓ Field with concave sections
✓ Minimum field size validation
✓ Orientation angle variations
✓ Lane overlap detection
✓ Coverage calculation accuracy
✓ Waypoint ordering correctness
```

**Deliverables:**
```typescript
// packages/route-planner/src/index.ts
export { generateCoverageRoute }
export { validateField }
export { calculateFieldArea }
export { RouteGenerationInput, RouteResult }

// packages/route-planner/__tests__/
// - generateCoverageRoute.test.ts
// - validateField.test.ts
// - coverage.test.ts
```

---

### Story 1.4: Machine Protocol Abstraction
**Story Points:** 8  
**Days:** 2  

**Tasks:**
- [ ] Define `MachineConnection` interface
- [ ] Implement `SimulatorMachineConnection` adapter
- [ ] Implement message serialization/deserialization
- [ ] Setup error handling & retry logic
- [ ] Document protocol specification

**Definition of Done:**
- ✅ Interface complete & documented
- ✅ Simulator implementation working
- ✅ Message format JSON-RPC 2.0 compliant
- ✅ Timeout & error handling implemented

**Deliverables:**
```typescript
// packages/machine-protocol/src/index.ts
export interface MachineConnection { ... }
export class SimulatorMachineConnection implements MachineConnection { ... }
export { MessageFormat, ErrorCodes }
```

---

### Story 1.5: Backend Skeleton (Express + Database)
**Story Points:** 5  
**Days:** 1-2  

**Tasks:**
- [ ] Setup Express.js with TypeScript
- [ ] Setup PostgreSQL connection (Prisma or Drizzle)
- [ ] Create migration scripts untuk fields & missions tables
- [ ] Implement basic CRUD endpoints (no auth yet):
  - POST /api/fields
  - GET /api/fields
  - GET /api/missions
- [ ] Setup error handling middleware
- [ ] Seed development database dengan sample fields

**Definition of Done:**
- ✅ Backend runnable dengan `npm run dev`
- ✅ Database migrations working
- ✅ Sample endpoints tested manually
- ✅ Error handling middleware active

**Deliverables:**
```
apps/backend/
├── src/
│   ├── db/
│   │   ├── schema.ts (Prisma/Drizzle)
│   │   └── migrations/
│   ├── routes/
│   │   ├── fields.ts
│   │   └── missions.ts
│   ├── middleware/
│   │   └── errorHandler.ts
│   └── app.ts
└── package.json
```

---

## 🎨 SPRINT 2: Mobile UI & Dashboard
### Weeks 2-3 | Capacity: 80 hours

**Goal:** Build core screens dengan mock data, setup navigation structure

### Story 2.1: Expo Project Setup & Navigation
**Story Points:** 8  
**Days:** 1-2  

**Tasks:**
- [ ] Initialize Expo project dengan TypeScript
- [ ] Setup React Navigation (bottom tab + stack navigation)
- [ ] Create folder structure (components, screens, stores, services)
- [ ] Setup Zustand untuk global state (fields, missions, machine connection)
- [ ] Create basic app theme & color scheme (green agricultural theme)
- [ ] Setup Zod validation untuk forms

**Navigation Structure:**
```
Root
├── Splash Screen
├── Main (Bottom Tab)
│   ├── Dashboard Tab
│   │   ├── Dashboard Screen
│   │   ├── Field Selection Modal
│   │   └── Quick Actions
│   │
│   ├── Fields Tab
│   │   ├── Field List Screen
│   │   ├── Field Detail Screen
│   │   └── Field Creation Flow
│   │
│   ├── History Tab
│   │   ├── Mission History List
│   │   └── Mission Detail Report
│   │
│   └── Settings Tab
│       └── Settings Screen
│
└── Mission Stack (Modal)
    ├── Field Mapping (future)
    ├── Planting Settings
    ├── Route Preview
    └── Mission Execution
```

**Definition of Done:**
- ✅ App navigable through all screens
- ✅ Bottom tab navigation working
- ✅ Global state with Zustand accessible
- ✅ Color theme applied consistently
- ✅ Mock data available untuk testing

---

### Story 2.2: Dashboard Screen
**Story Points:** 8  
**Days:** 2  

**Tasks:**
- [ ] Create Dashboard main screen layout:
  - Header: Machine status badge (Connected/Disconnected)
  - Card 1: Current Field info (name, area, status)
  - Card 2: Last Mission summary (if any)
  - Card 3: Quick Action Buttons:
    - "Mulai Misi Baru"
    - "Kontrol Manual"
    - "Lihat Riwayat"
- [ ] Implement field selection modal (dropdown or list)
- [ ] Display telemetry summary (battery, GPS, connection status)
- [ ] Add refresh button untuk update status

**Mock Data:**
```typescript
{
  fields: [
    { id: 1, name: "Sawah Utama", area: 1240, boundaryPointCount: 4 },
    { id: 2, name: "Sawah Timur", area: 890, boundaryPointCount: 5 }
  ],
  lastMission: {
    id: 1,
    fieldId: 1,
    status: "COMPLETED",
    completedAt: "2026-08-27 10:30",
    coverage: 96
  },
  machineStatus: {
    connected: true,
    battery: 82,
    gpsStatus: "GPS_FIX",
    speed: 0
  }
}
```

**Definition of Done:**
- ✅ All UI elements render correctly
- ✅ Buttons navigate to correct screens
- ✅ Mock data displays properly
- ✅ Layout responsive untuk portrait & landscape

**Deliverables:**
```
apps/mobile/src/screens/
├── DashboardScreen.tsx
├── components/
│   ├── FieldCard.tsx
│   ├── LastMissionCard.tsx
│   ├── MachineStatusBadge.tsx
│   ├── QuickActionButtons.tsx
│   └── FieldSelectionModal.tsx
└── hooks/
    └── useDashboardData.ts
```

---

### Story 2.3: Field List & Detail Screens
**Story Points:** 8  
**Days:** 2  

**Tasks:**
- [ ] Create Field List screen:
  - Display list of saved fields
  - Each item shows: name, area, last modified date, action button (Edit/Delete)
  - Add button untuk "Tambah Lapangan Baru"
- [ ] Create Field Detail screen:
  - Show field info (name, polygon, area, perimeter)
  - Display field boundary map (Canvas-based simple visualization)
  - Show list of missions untuk field ini
  - Action buttons: Edit, Delete, New Mission

**Definition of Done:**
- ✅ List displays correctly dengan mock data
- ✅ Actions (add/edit/delete) trigger alerts
- ✅ Map visualization shows field boundary
- ✅ Responsive layout

---

### Story 2.4: Planting Settings Form
**Story Points:** 5  
**Days:** 1-2  

**Tasks:**
- [ ] Create form screen dengan fields:
  - Machine Working Width (m): input number (default 1.5)
  - Headland Width (m): input number (default 3.0)
  - Lane Orientation: slider atau picker (0-360°)
  - Button: "Generate Jalur"
- [ ] Implement form validation dengan Zod
- [ ] Create submit handler yang calls route planner logic
- [ ] Add reset button untuk default values

**Definition of Done:**
- ✅ Form renders correctly
- ✅ Validation working
- ✅ Submit button disabled when invalid
- ✅ Input errors displayed clearly

**Deliverables:**
```
apps/mobile/src/screens/
├── PlantingSettingsScreen.tsx
└── components/
    ├── MachineWidthInput.tsx
    ├── HeadlandWidthInput.tsx
    ├── OrientationSlider.tsx
    └── GenerateRouteButton.tsx
```

---

### Story 2.5: Mission History Screen
**Story Points:** 5  
**Days:** 1  

**Tasks:**
- [ ] Create Mission History list screen:
  - Display all past missions
  - Each item: date, field name, status, coverage %
  - Tap to view detail
- [ ] Create Mission Detail screen:
  - Display mission metadata
  - Show route map
  - Display telemetry summary
  - Button: "Export Report" (future)

**Definition of Done:**
- ✅ List displays mock data
- ✅ Detail screen navigable
- ✅ Layout responsive

---

## 🎯 SPRINT 3: Mission Control & Simulator
### Weeks 3-4 | Capacity: 80 hours

**Goal:** Implement mission execution flow, integrate simulator, real-time monitoring

### Story 3.1: Route Preview Screen
**Story Points:** 8  
**Days:** 2  

**Tasks:**
- [ ] Create Route Preview screen:
  - Display generated route on canvas map
  - Show field boundary (green polygon)
  - Show planting lanes (blue lines)
  - Show start/end points (green/red markers)
  - Display stats: total distance, estimated coverage, number of lanes
  - Button: "Approve Mission" → proceed to execution
  - Button: "Adjust" → go back to settings

**Canvas-Based Map Implementation:**
```typescript
// Simple 2D canvas renderer, no external map library
- Zoom & pan controls
- Click to show coordinates
- Legend untuk lane/boundary/start/end
```

**Definition of Done:**
- ✅ Canvas rendering working
- ✅ Route displays correctly
- ✅ Zoom/pan functional
- ✅ Stats accurate

---

### Story 3.2: Mission Execution Screen
**Story Points:** 13  
**Days:** 3-4  

**Tasks:**
- [ ] Create Mission Execution screen layout:
  - Top: Mission status badge (READY, RUNNING, PAUSED, COMPLETED)
  - Middle: Real-time map dengan machine position
  - Machine position indicator (red dot)
  - Current lane highlight
  - Progress bar (mission completion %)
  - Bottom: Telemetry display:
    - Battery: %, timestamp
    - GPS Status: accuracy, fix type
    - Speed: m/s
    - Current Lane: N/total lanes
  - Control buttons:
    - "MULAI" (green, large) - Start mission
    - "PAUSE" (yellow) - Appears when running
    - "RESUME" (green) - Appears when paused
    - "STOP" (red)
    - "EMERGENCY STOP" (bright red, always visible)

**State Management:**
- Track mission state (READY → RUNNING → PAUSED → COMPLETED/STOPPED)
- Real-time telemetry updates dari simulator
- Progress calculation

**Definition of Done:**
- ✅ All UI elements render
- ✅ Buttons functional (state transitions work)
- ✅ Telemetry display updates
- ✅ Map shows machine position
- ✅ Progress bar accurate

---

### Story 3.3: Simulator Machine Connection
**Story Points:** 13  
**Days:** 3-4  

**Tasks:**
- [ ] Implement `SimulatorMachineConnection` class:
  - Virtual machine starts at mission start point
  - Moves along route at configurable speed (default 2 m/s)
  - Updates position every 100-500ms
  - Generates realistic telemetry:
    - Battery drain: 1-2% per 10 min of operation
    - GPS accuracy: fluctuate 0.5-2.0m
    - Speed: varies with terrain (mocked)
  - Detects waypoint reached (within 0.5m radius)
  - Increments lane counter when completing lane
  - Calculates mission progress %
  - Supports pause/resume (freeze position, resume from same point)
  - Supports stop (reset to start or final position)

**Telemetry Streaming:**
- Update rate: 1 Hz (every 1 second)
- Emit via event emitter atau callback

**Error Simulation:**
- Optional error injection: GPS_LOSS, CONNECTION_DROP, LOW_BATTERY

**Definition of Done:**
- ✅ Simulator runs mission end-to-end
- ✅ Telemetry streaming works
- ✅ Pause/resume functional
- ✅ Progress calculation accurate
- ✅ Error injection works

---

### Story 3.4: Telemetry Display & Logging
**Story Points:** 8  
**Days:** 2  

**Tasks:**
- [ ] Implement telemetry data collection:
  - Record every telemetry point dari machine connection
  - Store locally dalam memory atau AsyncStorage
  - Display real-time telemetry on mission screen
- [ ] Implement mission event logging:
  - Log mission state changes (START, PAUSE, RESUME, STOP)
  - Log errors/warnings
  - Log mission completion
- [ ] Create telemetry data structure (for persistence later)

**Definition of Done:**
- ✅ Telemetry collected during mission
- ✅ Display updates in real-time
- ✅ Data persist after mission completes
- ✅ Events logged properly

---

### Story 3.5: Backend Mission Management Endpoints
**Story Points:** 10  
**Days:** 2-3  

**Tasks:**
- [ ] Implement REST endpoints:
  - POST /api/missions → create new mission
  - GET /api/missions/:id → get mission detail
  - POST /api/missions/:id/start → start mission
  - POST /api/missions/:id/pause → pause mission
  - POST /api/missions/:id/resume → resume mission
  - POST /api/missions/:id/stop → stop mission
  - GET /api/missions/:id/telemetry → get telemetry history
- [ ] Implement mission state validation
- [ ] Add telemetry recording endpoint:
  - POST /api/missions/:id/telemetry → record telemetry point
- [ ] Database persistence untuk missions & telemetry

**Definition of Done:**
- ✅ All endpoints implemented
- ✅ State transitions validated
- ✅ Data persisted correctly
- ✅ Manual testing passed

---

## ⚙️ SPRINT 4: Integration & Polishing
### Weeks 4-5 | Capacity: 80 hours

**Goal:** Connect frontend dengan backend, refine UX, polish UI

### Story 4.1: API Client Setup (Mobile)
**Story Points:** 5  
**Days:** 1  

**Tasks:**
- [ ] Create API client dengan proper error handling:
  - Base URL configuration
  - Request interceptors (headers, auth)
  - Response interceptors (error handling)
  - Timeout management
- [ ] Integrate TanStack Query untuk data fetching
- [ ] Create custom hooks: `useFields()`, `useMissions()`, `useTelemetry()`
- [ ] Setup environment variables (.env.local)

**Definition of Done:**
- ✅ API calls working
- ✅ Error handling graceful
- ✅ Loading states visible
- ✅ Hooks properly typed

---

### Story 4.2: Connect Frontend ↔ Backend
**Story Points:** 13  
**Days:** 3  

**Tasks:**
- [ ] Replace mock data dengan real API calls:
  - Field List: fetch dari /api/fields
  - Mission History: fetch dari /api/missions
  - Mission Creation: POST ke /api/missions
  - Mission Execution: call /api/missions/:id/start, pause, resume, stop
- [ ] Implement real-time telemetry updates:
  - Poll /api/missions/:id/telemetry setiap 1 second
  - Atau setup WebSocket (future enhancement)
- [ ] Handle API errors gracefully:
  - Display error message in Indonesian
  - Retry logic untuk transient errors
  - Offline fallback (use local cache)
- [ ] Add loading indicators untuk async operations

**Definition of Done:**
- ✅ All API calls integrated
- ✅ Mock data removed
- ✅ Error handling working
- ✅ Loading states visible
- ✅ Performance acceptable

---

### Story 4.3: Manual Control Mode
**Story Points:** 5  
**Days:** 1-2  

**Tasks:**
- [ ] Create Manual Control screen:
  - D-Pad style controls (↑ forward, ↓ backward, ← left, → right)
  - Speed slider (0-100%)
  - Button labels in Indonesian
  - Cancel button to exit
- [ ] Implement manual command sending:
  - POST /api/machine/manual-command → {direction, intensity}
- [ ] Visual feedback untuk commands

**Definition of Done:**
- ✅ Controls responsive
- ✅ Commands sent correctly
- ✅ Feedback visible

---

### Story 4.4: Settings Screen & Localization
**Story Points:** 5  
**Days:** 1  

**Tasks:**
- [ ] Create Settings screen:
  - Machine Connection Settings (connection type selector)
  - Debug mode toggle
  - Clear cache button
  - About section
- [ ] Centralize all UI strings untuk i18n (future-proof):
  - Create strings.ts atau i18n config
  - All labels/buttons/messages in Indonesian

**Definition of Done:**
- ✅ Settings screen functional
- ✅ Strings centralized
- ✅ Ready untuk multi-language support

---

### Story 4.5: UI Polish & Responsive Design
**Story Points:** 8  
**Days:** 2  

**Tasks:**
- [ ] Test responsive layout (portrait, landscape, different screen sizes)
- [ ] Ensure minimum touch targets (44x44 px)
- [ ] Verify color contrast (WCAG AA compliant)
- [ ] Ensure font sizes readable outdoor
- [ ] Add loading spinners
- [ ] Add empty state screens (no fields, no missions)
- [ ] Add transition animations (smooth screen navigation)
- [ ] Test on actual phone (iOS/Android simulator)

**Definition of Done:**
- ✅ Responsive design tested
- ✅ Accessibility guidelines met
- ✅ UI polished
- ✅ Performance optimized

---

## ✅ SPRINT 5: Testing & Documentation
### Weeks 5-6 | Capacity: 80 hours

**Goal:** Comprehensive testing, documentation, final polish

### Story 5.1: Route Planner Unit Tests
**Story Points:** 10  
**Days:** 2  

**Tasks:**
- [ ] Write unit tests untuk route-planner package:
  - Regular rectangular field
  - Irregular polygon field
  - Concave fields
  - Edge cases (too small, invalid polygon)
  - Coverage calculation accuracy
  - Waypoint ordering
  - Lane overlap detection
- [ ] Achieve >80% code coverage
- [ ] Run tests dengan Jest

**Definition of Done:**
- ✅ All test cases passing
- ✅ Coverage >80%
- ✅ No console errors

---

### Story 5.2: Mission State Machine Tests
**Story Points:** 8  
**Days:** 1-2  

**Tasks:**
- [ ] Write integration tests untuk mission lifecycle:
  - Create mission → start → pause → resume → complete
  - Create mission → stop (abort)
  - Error handling (connection loss, invalid state transitions)
  - Emergency stop dari any state

**Definition of Done:**
- ✅ All state transitions tested
- ✅ Error cases covered

---

### Story 5.3: API Endpoint Tests
**Story Points:** 8  
**Days:** 1-2  

**Tasks:**
- [ ] Write API integration tests (Supertest):
  - Field CRUD operations
  - Mission creation & management
  - Telemetry recording
  - Error responses (400, 404, 500)

**Definition of Done:**
- ✅ All endpoints tested
- ✅ Happy path & error cases covered

---

### Story 5.4: Manual E2E Testing
**Story Points:** 8  
**Days:** 2  

**Tasks:**
- [ ] Test complete user journey:
  - 1. Open app
  - 2. Select field
  - 3. Enter planting settings
  - 4. Generate route
  - 5. Start mission
  - 6. Pause/resume
  - 7. Complete mission
  - 8. View history
- [ ] Test on actual devices (iOS simulator + Android emulator)
- [ ] Document any issues found
- [ ] Fix critical bugs

**Test Scenarios:**
```
✓ Happy path: Create field → Settings → Generate → Execute → Complete
✓ Pause/Resume: Start → Pause at 50% → Resume → Complete
✓ Emergency Stop: Running → Press E-Stop → Stop immediately
✓ Connection Loss: Simulator error injection → Handle gracefully
✓ Manual Controls: Navigate without mission
✓ History View: View past missions
✓ Offline: App works without backend
```

**Definition of Done:**
- ✅ All scenarios pass
- ✅ No crashes
- ✅ UX smooth
- ✅ Performance acceptable

---

### Story 5.5: Documentation
**Story Points:** 8  
**Days:** 2  

**Tasks:**
- [ ] Write API documentation (OpenAPI/Swagger spec)
- [ ] Write route planner algorithm documentation
- [ ] Write deployment guide
- [ ] Create CONTRIBUTING.md
- [ ] Create ARCHITECTURE.md
- [ ] Write README dengan setup & usage instructions
- [ ] Add JSDoc comments untuk all public APIs
- [ ] Create user guide (in Indonesian)

**Definition of Done:**
- ✅ All docs complete & accurate
- ✅ Setup guide tested (fresh clone → working app)
- ✅ API spec matches implementation

---

### Story 5.6: Release Preparation
**Story Points:** 5  
**Days:** 1  

**Tasks:**
- [ ] Tag version v0.1.0
- [ ] Create CHANGELOG
- [ ] Create release notes (in Indonesian)
- [ ] Verify all acceptance criteria met
- [ ] Prepare demo script

**Definition of Done:**
- ✅ Version tagged
- ✅ Release notes published
- ✅ Demo-ready

---

## 📊 SPRINT SUMMARY

| Sprint | Focus | Stories | Points | Days |
|--------|-------|---------|--------|------|
| 1 | Foundation & Routing | 5 | 39 | 9 |
| 2 | Mobile UI | 5 | 34 | 9 |
| 3 | Mission Control | 5 | 52 | 12 |
| 4 | Integration | 5 | 36 | 10 |
| 5 | Testing & Docs | 6 | 47 | 10 |
| **TOTAL** | **Full MVP** | **26** | **208** | **50 days** |

---

## 🎯 KEY MILESTONES

### End of Sprint 1
✅ Project structure ready  
✅ Route planner algorithm working  
✅ Backend skeleton ready  

### End of Sprint 2
✅ All major screens implemented  
✅ Mock data displays correctly  
✅ Navigation working  

### End of Sprint 3
✅ Mission execution functional  
✅ Simulator running end-to-end  
✅ Real-time monitoring working  

### End of Sprint 4
✅ Frontend ↔ Backend integrated  
✅ API calls working  
✅ UI polished  

### End of Sprint 5 (MVP Release)
✅ Comprehensive testing passed  
✅ Documentation complete  
✅ v0.1.0 released  
✅ **Ready untuk Phase 1+ (real hardware)**

---

## 🚨 RISKS & MITIGATION

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|-----------|
| Route planning algorithm complexity | High | High | Start with simple algo, iterate |
| React Native Expo learning curve | Medium | Medium | Good documentation, tutorials available |
| Backend database schema changes | Medium | High | Proper migrations, versioning |
| Simulator edge cases | Medium | Medium | Extensive testing, error injection |
| Performance on older devices | Low | Medium | Regular profiling, optimization |

---

## 📝 NEXT STEPS

1. **Kickoff Sprint 1** — Setup environment & start foundation work
2. **Daily standups** — Track progress, identify blockers
3. **Sprint reviews** — Demo working features, gather feedback
4. **Sprint retros** — Reflect on process, improve
5. **Continuous testing** — Don't wait until Sprint 5
6. **Documentation** — Write as you code, not after

---

**Roadmap Owner:** Adi Maryanto  
**Created:** August 27, 2026  
**Status:** Ready for Sprint 1 Kickoff
