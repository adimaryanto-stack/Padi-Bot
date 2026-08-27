import { Mission, Telemetry, ManualCommand, MachineStatus } from '@/types';

export interface MachineConnection {
  connect(): Promise<void>;
  disconnect(): Promise<void>;
  isConnected(): boolean;
  getStatus(): MachineStatus;
  uploadMission(mission: Mission): Promise<void>;
  startMission(): Promise<void>;
  pauseMission(): Promise<void>;
  resumeMission(): Promise<void>;
  stopMission(): Promise<void>;
  emergencyStop(): Promise<void>;
  sendManualCommand(cmd: ManualCommand): Promise<void>;
  onTelemetry(callback: (telemetry: Telemetry) => void): () => void;
  onError(callback: (error: Error) => void): () => void;
  onStatusChange(callback: (status: MachineStatus) => void): () => void;
}
