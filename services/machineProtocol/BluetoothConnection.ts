import { Mission, Telemetry, ManualCommand, MachineStatus, BluetoothConfig } from '@/types';
import { MachineConnection } from './types';

export class BluetoothConnection implements MachineConnection {
  constructor(_config?: BluetoothConfig) {}

  async connect(): Promise<void> {
    throw new Error('Koneksi Bluetooth hanya didukung di aplikasi Android');
  }

  async disconnect(): Promise<void> {}

  isConnected(): boolean {
    return false;
  }

  getStatus(): MachineStatus {
    return {
      connectionStatus: 'DISCONNECTED',
      connectionType: 'BLUETOOTH',
      errorMessage: 'Bluetooth hanya didukung di Android',
    };
  }

  async uploadMission(_mission: Mission): Promise<void> {
    throw new Error('Bluetooth hanya didukung di Android');
  }

  async startMission(): Promise<void> {
    throw new Error('Bluetooth hanya didukung di Android');
  }

  async pauseMission(): Promise<void> {
    throw new Error('Bluetooth hanya didukung di Android');
  }

  async resumeMission(): Promise<void> {
    throw new Error('Bluetooth hanya didukung di Android');
  }

  async stopMission(): Promise<void> {
    throw new Error('Bluetooth hanya didukung di Android');
  }

  async emergencyStop(): Promise<void> {}

  async sendManualCommand(_cmd: ManualCommand): Promise<void> {
    throw new Error('Bluetooth hanya didukung di Android');
  }

  onTelemetry(_callback: (telemetry: Telemetry) => void): () => void {
    return () => {};
  }

  onError(_callback: (error: Error) => void): () => void {
    return () => {};
  }

  onStatusChange(_callback: (status: MachineStatus) => void): () => void {
    return () => {};
  }
}
