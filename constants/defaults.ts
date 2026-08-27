import { ConnectionConfig } from '@/types';

// Default machine/planting parameters
export const DEFAULT_MACHINE_WIDTH_M = 1.5;
export const DEFAULT_HEADLAND_WIDTH_M = 3.0;
export const DEFAULT_LANE_ORIENTATION_DEG = 0;
export const DEFAULT_MAX_SPEED_MPS = 1.0;

// Validation limits
export const MIN_FIELD_POINTS = 3;
export const MIN_FIELD_AREA_M2 = 100;
export const MIN_MACHINE_WIDTH_M = 0.5;
export const MAX_MACHINE_WIDTH_M = 5.0;
export const MIN_HEADLAND_WIDTH_M = 1.0;
export const MAX_HEADLAND_WIDTH_M = 10.0;

// Simulator settings
export const SIMULATOR_SPEED_MPS = 1.0;           // m/s virtual speed
export const SIMULATOR_TELEMETRY_INTERVAL_MS = 1000; // 1 Hz
export const SIMULATOR_WAYPOINT_REACHED_RADIUS_M = 0.5;

// Connection settings
export const WIFI_DEFAULT_PORT = 80;
export const WIFI_WEBSOCKET_PORT = 81;
export const WIFI_RECONNECT_INTERVAL_MS = 3000;
export const BT_DEFAULT_BAUD = 9600;
export const MQTT_DEFAULT_BROKER = 'broker.hivemq.com';
export const MQTT_DEFAULT_PORT = 1883;

// Default connection config (Simulator for MVP)
export const DEFAULT_CONNECTION_CONFIG: ConnectionConfig = {
  type: 'SIMULATOR',
};

// Battery alert thresholds
export const BATTERY_WARNING_PCT = 30;
export const BATTERY_CRITICAL_PCT = 15;

// GPS accuracy thresholds (meters)
export const GPS_GOOD_ACCURACY_M = 3.0;
export const GPS_POOR_ACCURACY_M = 10.0;

// App info
export const APP_VERSION = '0.1.0';
export const APP_BUILD = '2026.08.27';
export const GITHUB_URL = 'https://github.com/adimaryanto-stack/Padi-Bot';
