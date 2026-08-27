import { sqliteTable, text, real, integer } from 'drizzle-orm/sqlite-core';

export const fields = sqliteTable('fields', {
  id: text('id').primaryKey(),
  name: text('name').notNull(),
  boundaryJson: text('boundary_json').notNull(), // JSON array of GeoPoint {lat, lon}
  areaM2: real('area_m2').notNull(),
  perimeterM: real('perimeter_m').notNull(),
  createdAt: integer('created_at').notNull(),
  updatedAt: integer('updated_at').notNull(),
});

export const missions = sqliteTable('missions', {
  id: text('id').primaryKey(),
  fieldId: text('field_id').notNull().references(() => fields.id, { onDelete: 'cascade' }),
  fieldName: text('field_name').notNull(),
  name: text('name').notNull(),
  status: text('status').notNull().default('DRAFT'),
  routeJson: text('route_json').notNull(), // JSON array of Waypoint
  machineWidthM: real('machine_width_m').notNull(),
  headlandWidthM: real('headland_width_m').notNull(),
  laneOrientationDeg: real('lane_orientation_deg').notNull(),
  totalLanes: integer('total_lanes').notNull(),
  estimatedCoveragePct: real('estimated_coverage_pct').notNull(),
  actualCoveragePct: real('actual_coverage_pct'),
  startedAt: integer('started_at'),
  completedAt: integer('completed_at'),
  createdAt: integer('created_at').notNull(),
});

export const telemetryPoints = sqliteTable('telemetry_points', {
  id: integer('id').primaryKey({ autoIncrement: true }),
  missionId: text('mission_id').notNull().references(() => missions.id, { onDelete: 'cascade' }),
  timestamp: integer('timestamp').notNull(),
  positionLat: real('position_lat').notNull(),
  positionLon: real('position_lon').notNull(),
  positionAccuracyM: real('position_accuracy_m').notNull(),
  batteryPct: real('battery_pct').notNull(),
  speedMps: real('speed_mps').notNull(),
  headingDeg: real('heading_deg').notNull(),
  gpsStatus: text('gps_status').notNull(),
  missionProgressPct: real('mission_progress_pct').notNull(),
  currentLaneIndex: integer('current_lane_index').notNull(),
  totalLanes: integer('total_lanes').notNull(),
});

export const missionEvents = sqliteTable('mission_events', {
  id: integer('id').primaryKey({ autoIncrement: true }),
  missionId: text('mission_id').notNull().references(() => missions.id, { onDelete: 'cascade' }),
  eventType: text('event_type').notNull(),
  message: text('message').notNull(),
  severity: text('severity').notNull(),
  timestamp: integer('timestamp').notNull(),
});
