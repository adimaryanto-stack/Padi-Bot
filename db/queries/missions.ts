import { Platform } from 'react-native';
import { Mission, MissionStatus, Waypoint } from '@/types';
import { db } from '@/db';
import { missions } from '@/db/schema';
import { eq } from 'drizzle-orm';
import { v4 as uuidv4 } from 'uuid';

export async function getAllMissions(): Promise<Mission[]> {
  if (Platform.OS === 'web' || !db) {
    return [];
  }
  try {
    const rows = await db.select().from(missions);
    return rows.map((row) => ({
      id: row.id,
      fieldId: row.fieldId,
      fieldName: row.fieldName,
      name: row.name,
      status: row.status as MissionStatus,
      route: JSON.parse(row.routeJson) as Waypoint[],
      machineWidthM: row.machineWidthM,
      headlandWidthM: row.headlandWidthM,
      laneOrientationDeg: row.laneOrientationDeg,
      totalLanes: row.totalLanes,
      estimatedCoveragePct: row.estimatedCoveragePct,
      actualCoveragePct: row.actualCoveragePct ?? undefined,
      startedAt: row.startedAt ?? undefined,
      completedAt: row.completedAt ?? undefined,
      createdAt: row.createdAt,
    }));
  } catch (e) {
    console.error('Failed to get all missions:', e);
    return [];
  }
}

export async function getMissionsByFieldId(fieldId: string): Promise<Mission[]> {
  if (Platform.OS === 'web' || !db) {
    return [];
  }
  try {
    const rows = await db.select().from(missions).where(eq(missions.fieldId, fieldId));
    return rows.map((row) => ({
      id: row.id,
      fieldId: row.fieldId,
      fieldName: row.fieldName,
      name: row.name,
      status: row.status as MissionStatus,
      route: JSON.parse(row.routeJson) as Waypoint[],
      machineWidthM: row.machineWidthM,
      headlandWidthM: row.headlandWidthM,
      laneOrientationDeg: row.laneOrientationDeg,
      totalLanes: row.totalLanes,
      estimatedCoveragePct: row.estimatedCoveragePct,
      actualCoveragePct: row.actualCoveragePct ?? undefined,
      startedAt: row.startedAt ?? undefined,
      completedAt: row.completedAt ?? undefined,
      createdAt: row.createdAt,
    }));
  } catch (e) {
    console.error(`Failed to get missions for field ${fieldId}:`, e);
    return [];
  }
}

export async function getMissionById(id: string): Promise<Mission | null> {
  if (Platform.OS === 'web' || !db) {
    return null;
  }
  try {
    const rows = await db.select().from(missions).where(eq(missions.id, id)).limit(1);
    if (rows.length === 0) return null;
    const row = rows[0];
    return {
      id: row.id,
      fieldId: row.fieldId,
      fieldName: row.fieldName,
      name: row.name,
      status: row.status as MissionStatus,
      route: JSON.parse(row.routeJson) as Waypoint[],
      machineWidthM: row.machineWidthM,
      headlandWidthM: row.headlandWidthM,
      laneOrientationDeg: row.laneOrientationDeg,
      totalLanes: row.totalLanes,
      estimatedCoveragePct: row.estimatedCoveragePct,
      actualCoveragePct: row.actualCoveragePct ?? undefined,
      startedAt: row.startedAt ?? undefined,
      completedAt: row.completedAt ?? undefined,
      createdAt: row.createdAt,
    };
  } catch (e) {
    console.error(`Failed to get mission ${id}:`, e);
    return null;
  }
}

export async function createMission(
  data: Omit<Mission, 'id' | 'createdAt'>
): Promise<Mission> {
  const now = Date.now();
  const id = uuidv4();
  const newMission: Mission = {
    id,
    ...data,
    createdAt: now,
  };

  if (Platform.OS !== 'web' && db) {
    try {
      await db.insert(missions).values({
        id,
        fieldId: data.fieldId,
        fieldName: data.fieldName,
        name: data.name,
        status: data.status,
        routeJson: JSON.stringify(data.route),
        machineWidthM: data.machineWidthM,
        headlandWidthM: data.headlandWidthM,
        laneOrientationDeg: data.laneOrientationDeg,
        totalLanes: data.totalLanes,
        estimatedCoveragePct: data.estimatedCoveragePct,
        actualCoveragePct: data.actualCoveragePct,
        startedAt: data.startedAt,
        completedAt: data.completedAt,
        createdAt: now,
      });
    } catch (e) {
      console.error('Failed to insert mission into SQLite:', e);
    }
  }

  return newMission;
}

export async function updateMissionStatus(
  id: string,
  status: MissionStatus,
  extra?: Partial<Mission>
): Promise<void> {
  if (Platform.OS !== 'web' && db) {
    try {
      const updateData: Record<string, unknown> = { status };
      if (extra?.startedAt !== undefined) updateData.startedAt = extra.startedAt;
      if (extra?.completedAt !== undefined) updateData.completedAt = extra.completedAt;
      if (extra?.actualCoveragePct !== undefined) updateData.actualCoveragePct = extra.actualCoveragePct;

      await db.update(missions).set(updateData).where(eq(missions.id, id));
    } catch (e) {
      console.error(`Failed to update status for mission ${id}:`, e);
    }
  }
}

export async function deleteMission(id: string): Promise<void> {
  if (Platform.OS !== 'web' && db) {
    try {
      await db.delete(missions).where(eq(missions.id, id));
    } catch (e) {
      console.error(`Failed to delete mission ${id}:`, e);
    }
  }
}
