import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Platform } from 'react-native';
import {
  Mission,
  MissionStatus,
  Telemetry,
  MissionEvent,
  MissionEventType,
  MissionEventSeverity,
  RouteResult,
} from '@/types';
import * as MissionQueries from '@/db/queries/missions';
import * as TelemetryQueries from '@/db/queries/telemetry';

const DEFAULT_SAMPLE_MISSIONS: Mission[] = [
  {
    id: 'mission-sample-1',
    fieldId: 'field-sawah-utama',
    fieldName: 'Sawah Utama',
    name: 'Misi Sawah Utama #1',
    status: 'COMPLETED',
    route: [
      { lat: -6.9234, lon: 107.6100, order: 0, type: 'lane', laneIndex: 0 },
      { lat: -6.9240, lon: 107.6108, order: 1, type: 'lane', laneIndex: 0 },
      { lat: -6.9239, lon: 107.6109, order: 2, type: 'lane', laneIndex: 1 },
      { lat: -6.9233, lon: 107.6101, order: 3, type: 'lane', laneIndex: 1 },
      { lat: -6.9232, lon: 107.6103, order: 4, type: 'lane', laneIndex: 2 },
      { lat: -6.9238, lon: 107.6110, order: 5, type: 'lane', laneIndex: 2 },
    ],
    machineWidthM: 1.5,
    headlandWidthM: 3.0,
    laneOrientationDeg: 0,
    totalLanes: 3,
    estimatedCoveragePct: 94,
    actualCoveragePct: 94,
    startedAt: Date.now() - 86400000 * 2,
    completedAt: Date.now() - 86400000 * 2 + 754000,
    createdAt: Date.now() - 86400000 * 2,
  },
];

interface MissionState {
  missions: Mission[];
  activeMissionId: string | null;
  activeMission: Mission | null;
  pendingRoute: RouteResult | null;
  liveTelemetry: Telemetry | null;
  missionEvents: MissionEvent[];
  isLoading: boolean;

  setMissions: (missions: Mission[]) => void;
  addMission: (missionData: Omit<Mission, 'id' | 'createdAt'>) => Promise<Mission>;
  updateMissionStatus: (id: string, status: MissionStatus, extra?: Partial<Mission>) => Promise<void>;
  deleteMission: (id: string) => Promise<void>;
  setActiveMission: (mission: Mission | null) => void;
  setPendingRoute: (route: RouteResult | null) => void;
  updateTelemetry: (telemetry: Telemetry) => void;
  addMissionEvent: (eventType: MissionEventType, message: string, severity: MissionEventSeverity) => Promise<void>;
  clearMissionEvents: () => void;
  loadFromDB: () => Promise<void>;
  clearAllMissions: () => Promise<void>;
}

export const useMissionStore = create<MissionState>()(
  persist(
    (set, get) => ({
      missions: DEFAULT_SAMPLE_MISSIONS,
      activeMissionId: 'mission-sample-1',
      activeMission: DEFAULT_SAMPLE_MISSIONS[0],
      pendingRoute: null,
      liveTelemetry: null,
      missionEvents: [],
      isLoading: false,

      setMissions: (missions) => set({ missions }),

      addMission: async (missionData) => {
        const newMission = await MissionQueries.createMission(missionData);
        set((state) => ({
          missions: [newMission, ...state.missions],
          activeMissionId: newMission.id,
          activeMission: newMission,
        }));
        return newMission;
      },

      updateMissionStatus: async (id, status, extra) => {
        await MissionQueries.updateMissionStatus(id, status, extra);
        set((state) => {
          const updatedMissions = state.missions.map((m) =>
            m.id === id ? { ...m, status, ...extra } : m
          );
          const active =
            state.activeMission?.id === id
              ? { ...state.activeMission, status, ...extra }
              : state.activeMission;
          return { missions: updatedMissions, activeMission: active };
        });
      },

      deleteMission: async (id) => {
        await MissionQueries.deleteMission(id);
        set((state) => ({
          missions: state.missions.filter((m) => m.id !== id),
          activeMissionId: state.activeMissionId === id ? null : state.activeMissionId,
          activeMission: state.activeMission?.id === id ? null : state.activeMission,
        }));
      },

      setActiveMission: (mission) =>
        set({ activeMission: mission, activeMissionId: mission ? mission.id : null }),

      setPendingRoute: (route) => set({ pendingRoute: route }),

      updateTelemetry: (telemetry) => {
        set({ liveTelemetry: telemetry });
        const activeId = get().activeMissionId;
        if (activeId) {
          TelemetryQueries.insertTelemetryPoint(activeId, telemetry);
        }
      },

      addMissionEvent: async (eventType, message, severity) => {
        const activeId = get().activeMissionId;
        if (activeId) {
          const ev = await TelemetryQueries.insertMissionEvent(activeId, eventType, message, severity);
          set((state) => ({ missionEvents: [...state.missionEvents, ev] }));
        }
      },

      clearMissionEvents: () => set({ missionEvents: [] }),

      loadFromDB: async () => {
        set({ isLoading: true });
        try {
          if (Platform.OS !== 'web') {
            const dbMissions = await MissionQueries.getAllMissions();
            if (dbMissions && dbMissions.length > 0) {
              set({ missions: dbMissions });
            }
          }
        } catch (e) {
          console.error('Failed to load missions from DB:', e);
        } finally {
          set({ isLoading: false });
        }
      },

      clearAllMissions: async () => {
        const current = get().missions;
        for (const m of current) {
          await MissionQueries.deleteMission(m.id);
        }
        set({
          missions: [],
          activeMissionId: null,
          activeMission: null,
          missionEvents: [],
          liveTelemetry: null,
        });
      },
    }),
    {
      name: 'padibot-mission-storage-v2',
      storage: createJSONStorage(() => AsyncStorage),
    }
  )
);
