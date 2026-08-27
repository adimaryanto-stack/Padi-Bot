import * as SQLite from 'expo-sqlite';
import { drizzle } from 'drizzle-orm/expo-sqlite';
import * as schema from './schema';

let sqliteDb: SQLite.SQLiteDatabase | null = null;

try {
  sqliteDb = SQLite.openDatabaseSync('padibot.db');
} catch (e) {
  console.warn('Failed to open SQLite database sync:', e);
}

export const db = sqliteDb ? drizzle(sqliteDb, { schema }) : null;

export async function initDatabase() {
  if (!sqliteDb) return;
  try {
    await sqliteDb.execAsync(`
      PRAGMA journal_mode = WAL;
      
      CREATE TABLE IF NOT EXISTS fields (
        id TEXT PRIMARY KEY NOT NULL,
        name TEXT NOT NULL,
        boundary_json TEXT NOT NULL,
        area_m2 REAL NOT NULL,
        perimeter_m REAL NOT NULL,
        created_at INTEGER NOT NULL,
        updated_at INTEGER NOT NULL
      );

      CREATE TABLE IF NOT EXISTS missions (
        id TEXT PRIMARY KEY NOT NULL,
        field_id TEXT NOT NULL REFERENCES fields(id) ON DELETE CASCADE,
        field_name TEXT NOT NULL,
        name TEXT NOT NULL,
        status TEXT NOT NULL DEFAULT 'DRAFT',
        route_json TEXT NOT NULL,
        machine_width_m REAL NOT NULL,
        headland_width_m REAL NOT NULL,
        lane_orientation_deg REAL NOT NULL,
        total_lanes INTEGER NOT NULL,
        estimated_coverage_pct REAL NOT NULL,
        actual_coverage_pct REAL,
        started_at INTEGER,
        completed_at INTEGER,
        created_at INTEGER NOT NULL
      );

      CREATE TABLE IF NOT EXISTS telemetry_points (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        mission_id TEXT NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
        timestamp INTEGER NOT NULL,
        position_lat REAL NOT NULL,
        position_lon REAL NOT NULL,
        position_accuracy_m REAL NOT NULL,
        battery_pct REAL NOT NULL,
        speed_mps REAL NOT NULL,
        heading_deg REAL NOT NULL,
        gps_status TEXT NOT NULL,
        mission_progress_pct REAL NOT NULL,
        current_lane_index INTEGER NOT NULL,
        total_lanes INTEGER NOT NULL
      );

      CREATE TABLE IF NOT EXISTS mission_events (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        mission_id TEXT NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
        event_type TEXT NOT NULL,
        message TEXT NOT NULL,
        severity TEXT NOT NULL,
        timestamp INTEGER NOT NULL
      );
    `);
    console.log('Database initialized successfully on Native');
  } catch (err) {
    console.error('Error initializing SQLite database:', err);
  }
}
