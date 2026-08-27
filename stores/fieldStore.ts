import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Platform } from 'react-native';
import { Field } from '@/types';
import * as FieldQueries from '@/db/queries/fields';

const DEFAULT_SAMPLE_FIELDS: Field[] = [
  {
    id: 'field-sawah-utama',
    name: 'Sawah Utama',
    boundary: [
      { lat: -6.9234, lon: 107.6100 },
      { lat: -6.9240, lon: 107.6108 },
      { lat: -6.9228, lon: 107.6115 },
      { lat: -6.9222, lon: 107.6105 },
    ],
    areaM2: 1240,
    perimeterM: 145,
    createdAt: Date.now() - 86400000 * 2,
    updatedAt: Date.now() - 86400000 * 2,
  },
  {
    id: 'field-sawah-timur',
    name: 'Sawah Timur',
    boundary: [
      { lat: -6.9250, lon: 107.6120 },
      { lat: -6.9258, lon: 107.6129 },
      { lat: -6.9248, lon: 107.6136 },
      { lat: -6.9242, lon: 107.6125 },
    ],
    areaM2: 890,
    perimeterM: 120,
    createdAt: Date.now() - 86400000 * 5,
    updatedAt: Date.now() - 86400000 * 5,
  },
];

interface FieldState {
  fields: Field[];
  activeFieldId: string | null;
  isLoading: boolean;
  setFields: (fields: Field[]) => void;
  addField: (field: Omit<Field, 'id' | 'createdAt' | 'updatedAt'>) => Promise<Field>;
  updateField: (id: string, updates: Partial<Field>) => Promise<Field | null>;
  removeField: (id: string) => Promise<void>;
  setActiveField: (id: string | null) => void;
  getActiveField: () => Field | null;
  loadFromDB: () => Promise<void>;
  clearAllFields: () => Promise<void>;
}

export const useFieldStore = create<FieldState>()(
  persist(
    (set, get) => ({
      fields: DEFAULT_SAMPLE_FIELDS,
      activeFieldId: 'field-sawah-utama',
      isLoading: false,

      setFields: (fields) => set({ fields }),

      addField: async (fieldData) => {
        const newField = await FieldQueries.createField(fieldData);
        set((state) => {
          const updated = [newField, ...state.fields.filter((f) => f.id !== newField.id)];
          return {
            fields: updated,
            activeFieldId: newField.id,
          };
        });
        return newField;
      },

      updateField: async (id, updates) => {
        const updated = await FieldQueries.updateField(id, updates);
        if (updated) {
          set((state) => ({
            fields: state.fields.map((f) => (f.id === id ? updated : f)),
          }));
        }
        return updated;
      },

      removeField: async (id) => {
        await FieldQueries.deleteField(id);
        set((state) => {
          const remaining = state.fields.filter((f) => f.id !== id);
          return {
            fields: remaining,
            activeFieldId:
              state.activeFieldId === id
                ? (remaining[0]?.id ?? null)
                : state.activeFieldId,
          };
        });
      },

      setActiveField: (id) => set({ activeFieldId: id }),

      getActiveField: () => {
        const state = get();
        return (
          state.fields.find((f) => f.id === state.activeFieldId) ||
          state.fields[0] ||
          null
        );
      },

      loadFromDB: async () => {
        set({ isLoading: true });
        try {
          if (Platform.OS !== 'web') {
            const dbFields = await FieldQueries.getAllFields();
            if (dbFields && dbFields.length > 0) {
              set({ fields: dbFields });
            }
          }
        } catch (e) {
          console.error('Failed to load fields from DB:', e);
        } finally {
          set({ isLoading: false });
        }
      },

      clearAllFields: async () => {
        const currentFields = get().fields;
        for (const f of currentFields) {
          await FieldQueries.deleteField(f.id);
        }
        set({ fields: [], activeFieldId: null });
      },
    }),
    {
      name: 'padibot-field-storage-v2',
      storage: createJSONStorage(() => AsyncStorage),
    }
  )
);
