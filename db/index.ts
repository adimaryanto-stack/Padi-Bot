import type { ExpoSQLiteDatabase } from 'drizzle-orm/expo-sqlite';
import * as schema from './schema';

export const db = null as unknown as ExpoSQLiteDatabase<typeof schema> | null;

export async function initDatabase() {
  // Web uses client-side store persistence
  console.log('Web environment: using client-side store persistence');
}
