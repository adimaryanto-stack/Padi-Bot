import { Platform } from 'react-native';
import { Field, GeoPoint } from '@/types';
import { db } from '@/db';
import { fields } from '@/db/schema';
import { eq } from 'drizzle-orm';
import { v4 as uuidv4 } from 'uuid';

export async function getAllFields(): Promise<Field[]> {
  if (Platform.OS === 'web' || !db) {
    return [];
  }
  try {
    const rows = await db.select().from(fields);
    return rows.map((row) => ({
      id: row.id,
      name: row.name,
      boundary: JSON.parse(row.boundaryJson) as GeoPoint[],
      areaM2: row.areaM2,
      perimeterM: row.perimeterM,
      createdAt: row.createdAt,
      updatedAt: row.updatedAt,
    }));
  } catch (e) {
    console.error('Failed to get all fields:', e);
    return [];
  }
}

export async function getFieldById(id: string): Promise<Field | null> {
  if (Platform.OS === 'web' || !db) {
    return null;
  }
  try {
    const rows = await db.select().from(fields).where(eq(fields.id, id)).limit(1);
    if (rows.length === 0) return null;
    const row = rows[0];
    return {
      id: row.id,
      name: row.name,
      boundary: JSON.parse(row.boundaryJson) as GeoPoint[],
      areaM2: row.areaM2,
      perimeterM: row.perimeterM,
      createdAt: row.createdAt,
      updatedAt: row.updatedAt,
    };
  } catch (e) {
    console.error(`Failed to get field ${id}:`, e);
    return null;
  }
}

export async function createField(
  data: Omit<Field, 'id' | 'createdAt' | 'updatedAt'>
): Promise<Field> {
  const now = Date.now();
  const id = uuidv4();
  const newField: Field = {
    id,
    ...data,
    createdAt: now,
    updatedAt: now,
  };

  if (Platform.OS !== 'web' && db) {
    try {
      await db.insert(fields).values({
        id,
        name: data.name,
        boundaryJson: JSON.stringify(data.boundary),
        areaM2: data.areaM2,
        perimeterM: data.perimeterM,
        createdAt: now,
        updatedAt: now,
      });
    } catch (e) {
      console.error('Failed to insert field into SQLite:', e);
    }
  }

  return newField;
}

export async function updateField(id: string, updates: Partial<Field>): Promise<Field | null> {
  const existing = await getFieldById(id);
  const now = Date.now();

  if (Platform.OS !== 'web' && db) {
    try {
      const updateData: Record<string, unknown> = { updatedAt: now };
      if (updates.name !== undefined) updateData.name = updates.name;
      if (updates.boundary !== undefined) updateData.boundaryJson = JSON.stringify(updates.boundary);
      if (updates.areaM2 !== undefined) updateData.areaM2 = updates.areaM2;
      if (updates.perimeterM !== undefined) updateData.perimeterM = updates.perimeterM;

      await db.update(fields).set(updateData).where(eq(fields.id, id));
    } catch (e) {
      console.error(`Failed to update field ${id}:`, e);
    }
  }

  if (!existing) return null;
  return { ...existing, ...updates, updatedAt: now };
}

export async function deleteField(id: string): Promise<void> {
  if (Platform.OS !== 'web' && db) {
    try {
      await db.delete(fields).where(eq(fields.id, id));
    } catch (e) {
      console.error(`Failed to delete field ${id}:`, e);
    }
  }
}
