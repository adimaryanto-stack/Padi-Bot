// ─────────────────────────────────────────────────────────────
// PadiBot — Core TypeScript Types
// ─────────────────────────────────────────────────────────────

/** Geographic coordinate point */
export interface GeoPoint {
  lat: number;
  lon: number;
}

/** A saved rice field with boundary polygon */
export interface Field {
  id: string;
  name: string;
  boundary: GeoPoint[];  // polygon vertices
  areaM2: number;
  perimeterM: number;
  createdAt: number;     // Unix ms
  updatedAt: number;
}

// ── Mission ──────────────────────────────────────────────────

export type MissionStatus =
  | 'DRAFT'
  | 'READY'
  | 'RUNNING'
  | 'PAUSED'
  | 'COMPLETED'
  | 'STOPPED'
  | 'ERROR';

export interface Waypoint {
  lat: number;
  lon: number;
  order: number;
  type: 'lane' | 'turn' | 'headland';
  laneIndex?: number;
}

export interface Mission {
  id: string;
  fieldId: string;
  fieldName: string;
  name: string;
  status: MissionStatus;
  route: Waypoint[];
  machineWidthM: number;
  headlandWidthM: number;
  laneOrientationDeg: number;
  totalLanes: number;
  estimatedCoveragePct: number;
  actualCoveragePct?: number;
  startedAt?: number;
  completedAt?: number;
  createdAt: number;
}

export type MissionEventType = 'START' | 'PAUSE' | 'RESUME' | 'STOP' | 'ERROR' | 'WARNING' | 'COMPLETED';
export type MissionEventSeverity = 'INFO' | 'WARNING' | 'CRITICAL';

export interface MissionEvent {
  id: string;
  missionId: string;
  eventType: MissionEventType;
  message: string;
  severity: MissionEventSeverity;
  timestamp: number;
}

// ── Route Planning ────────────────────────────────────────────

export type RoutePattern =
  | 'BOUSTROPHEDON'
  | 'HEADLAND_INNER'
  | 'SPIRAL_INWARD'
  | 'SPIRAL_OUTWARD';

export interface RouteGenerationInput {
  fieldBoundary: GeoPoint[];
  machineWidthM: number;
  headlandWidthM: number;
  orientationDeg: number;
  pattern?: RoutePattern;
}

export interface RouteResult {
  waypoints: Waypoint[];
  totalLanes: number;
  totalDistanceM: number;
  estimatedCoveragePct: number;
  pattern?: RoutePattern;
}

// ── Telemetry ─────────────────────────────────────────────────

export type GpsStatus = 'GPS' | 'RTK' | 'DGPS' | 'FLOAT' | 'NONE';

export interface Telemetry {
  positionLat: number;
  positionLon: number;
  positionAccuracyM: number;
  batteryPct: number;
  speedMps: number;
  headingDeg: number;
  gpsStatus: GpsStatus;
  missionProgressPct: number;
  currentLaneIndex: number;
  totalLanes: number;
  timestamp: number;
}

// ── Machine Connection ────────────────────────────────────────

export type ConnectionType = 'SIMULATOR' | 'WIFI' | 'BLUETOOTH' | 'GSM';
export type MachineConnectionStatus = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'ERROR';

export interface WiFiConfig {
  ipAddress: string;
  port: number;
}

export interface GSMConfig {
  mqttBroker: string;
  deviceId: string;
  username?: string;
  password?: string;
}

export interface BluetoothConfig {
  deviceId: string;
  deviceName: string;
}

export interface ConnectionConfig {
  type: ConnectionType;
  wifi?: WiFiConfig;
  gsm?: GSMConfig;
  bluetooth?: BluetoothConfig;
}

export interface MachineStatus {
  connectionStatus: MachineConnectionStatus;
  connectionType: ConnectionType;
  lastTelemetry?: Telemetry;
  errorMessage?: string;
}

export interface ManualCommand {
  direction: 'FORWARD' | 'BACKWARD' | 'LEFT' | 'RIGHT' | 'STOP';
  intensity: number; // 0–100
}

/** JSON message sent FROM app TO Arduino */
export interface ArduinoCommand {
  cmd: string;
  id: number;
  payload?: Record<string, unknown>;
}

/** JSON message received FROM Arduino TO app */
export interface ArduinoResponse {
  status: 'OK' | 'ERROR';
  id: number;
  data?: Record<string, unknown>;
  error?: string;
}

/** Telemetry JSON packet sent by Arduino every 1s */
export interface ArduinoTelemetry {
  type: 'TELEMETRY';
  lat: number;
  lon: number;
  accuracy: number;
  battery: number;
  speed: number;
  heading: number;
  gpsStatus: GpsStatus;
  progress: number;
  lane: number;
  totalLanes: number;
}
