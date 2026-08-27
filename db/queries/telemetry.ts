import { Platform } from 'react-native';
import { Telemetry, MissionEvent, MissionEventType, MissionEventSeverity } from '@/types';
import { db } from '@/db';
import { telemetryPoints, missionEvents } from '@/db/schema';
import { eq } from 'drizzle-orm';

export async function insertTelemetryPoint(missionId: string, telemetry: Telemetry): Promise<void> {
  if (Platform.OS !== 'web' && db) {
    try {
      await db.insert(telemetryPoints).values({
        missionId,
        timestamp: telemetry.timestamp,
        positionLat: telemetry.positionLat,
        positionLon: telemetry.positionLon,
        positionAccuracyM: telemetry.positionAccuracyM,
        batteryPct: telemetry.batteryPct,
        speedMps: telemetry.speedMps,
        headingDeg: telemetry.headingDeg,
        gpsStatus: telemetry.gpsStatus,
        missionProgressPct: telemetry.missionProgressPct,
        currentLaneIndex: telemetry.currentLaneIndex,
        totalLanes: telemetry.totalLanes,
      });
    } catch (e) {
      console.error('Failed to insert telemetry point into SQLite:', e);
    }
  }
}

export async function insertMissionEvent(
  missionId: string,
  eventType: MissionEventType,
  message: string,
  severity: MissionEventSeverity
): Promise<MissionEvent> {
  const now = Date.now();
  const event: MissionEvent = {
    id: `${now}-${Math.random().toString(36).substring(2, 7)}`,
    missionId,
    eventType,
    message,
    severity,
    timestamp: now,
  };

  if (Platform.OS !== 'web' && db) {
    try {
      await db.insert(missionEvents).values({
        missionId,
        eventType,
        message,
        severity,
        timestamp: now,
      });
    } catch (e) {
      console.error('Failed to insert mission event into SQLite:', e);
    }
  }

  return event;
}

export async function getMissionEvents(missionId: string): Promise<MissionEvent[]> {
  if (Platform.OS === 'web' || !db) {
    return [];
  }
  try {
    const rows = await db.select().from(missionEvents).where(eq(missionEvents.missionId, missionId));
    return rows.map((r) => ({
      id: r.id.toString(),
      missionId: r.missionId,
      eventType: r.eventType as MissionEventType,
      message: r.message,
      severity: r.severity as MissionEventSeverity,
      timestamp: r.timestamp,
    }));
  } catch (e) {
    console.error(`Failed to get mission events for ${missionId}:`, e);
    return [];
  }
}
