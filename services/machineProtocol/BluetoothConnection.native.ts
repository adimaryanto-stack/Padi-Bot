import { Mission, Telemetry, ManualCommand, MachineStatus, BluetoothConfig, ArduinoCommand, ArduinoTelemetry } from '@/types';
import { MachineConnection } from './types';
import { BleManager, Device } from 'react-native-ble-plx';

const HM10_SERVICE_UUID = '0000ffe0-0000-1000-8000-00805f9b34fb';
const HM10_CHAR_UUID = '0000ffe1-0000-1000-8000-00805f9b34fb';

export class BluetoothConnection implements MachineConnection {
  private config: BluetoothConfig | null = null;
  private manager: BleManager | null = null;
  private connectedDevice: Device | null = null;
  private connected: boolean = false;
  private buffer: string = '';

  private telemetryListeners = new Set<(t: Telemetry) => void>();
  private errorListeners = new Set<(e: Error) => void>();
  private statusListeners = new Set<(s: MachineStatus) => void>();

  constructor(config?: BluetoothConfig) {
    if (config) this.config = config;
    try {
      this.manager = new BleManager();
    } catch (e) {
      console.warn('BleManager init failed:', e);
    }
  }

  async connect(): Promise<void> {
    if (!this.manager || !this.config?.deviceId) {
      throw new Error('Bluetooth device belum dipilih atau tidak didukung');
    }

    try {
      const device = await this.manager.connectToDevice(this.config.deviceId);
      await device.discoverAllServicesAndCharacteristics();
      this.connectedDevice = device;
      this.connected = true;
      this.notifyStatus();

      // Monitor characteristic for incoming telemetry JSON
      device.monitorCharacteristicForService(
        HM10_SERVICE_UUID,
        HM10_CHAR_UUID,
        (error, characteristic) => {
          if (error) {
            console.warn('BLE monitor error:', error);
            return;
          }
          if (characteristic?.value) {
            const chunk = Buffer.from(characteristic.value, 'base64').toString('utf-8');
            this.buffer += chunk;
            const lines = this.buffer.split('\n');
            this.buffer = lines.pop() || '';

            for (const line of lines) {
              if (line.trim()) {
                this.parseTelemetryLine(line.trim());
              }
            }
          }
        }
      );
    } catch (e) {
      this.connected = false;
      this.notifyStatus();
      throw e;
    }
  }

  async disconnect(): Promise<void> {
    if (this.connectedDevice) {
      try {
        await this.connectedDevice.cancelConnection();
      } catch (e) {
        console.warn('BLE disconnect error:', e);
      }
      this.connectedDevice = null;
    }
    this.connected = false;
    this.notifyStatus();
  }

  isConnected(): boolean {
    return this.connected;
  }

  getStatus(): MachineStatus {
    return {
      connectionStatus: this.connected ? 'CONNECTED' : 'DISCONNECTED',
      connectionType: 'BLUETOOTH',
    };
  }

  async uploadMission(mission: Mission): Promise<void> {
    await this.sendCommand({
      cmd: 'UPLOAD_MISSION',
      id: Date.now(),
      payload: {
        missionId: mission.id,
        waypoints: mission.route,
        machineWidth: mission.machineWidthM,
        headlandWidth: mission.headlandWidthM,
      },
    });
  }

  async startMission(): Promise<void> {
    await this.sendCommand({ cmd: 'START_MISSION', id: Date.now() });
  }

  async pauseMission(): Promise<void> {
    await this.sendCommand({ cmd: 'PAUSE', id: Date.now() });
  }

  async resumeMission(): Promise<void> {
    await this.sendCommand({ cmd: 'RESUME', id: Date.now() });
  }

  async stopMission(): Promise<void> {
    await this.sendCommand({ cmd: 'STOP', id: Date.now() });
  }

  async emergencyStop(): Promise<void> {
    await this.sendCommand({ cmd: 'E_STOP', id: Date.now() });
  }

  async sendManualCommand(cmd: ManualCommand): Promise<void> {
    await this.sendCommand({
      cmd: 'MANUAL',
      id: Date.now(),
      payload: {
        direction: cmd.direction,
        intensity: cmd.intensity,
      },
    });
  }

  onTelemetry(callback: (telemetry: Telemetry) => void): () => void {
    this.telemetryListeners.add(callback);
    return () => this.telemetryListeners.delete(callback);
  }

  onError(callback: (error: Error) => void): () => void {
    this.errorListeners.add(callback);
    return () => this.errorListeners.delete(callback);
  }

  onStatusChange(callback: (status: MachineStatus) => void): () => void {
    this.statusListeners.add(callback);
    return () => this.statusListeners.delete(callback);
  }

  private async sendCommand(cmd: ArduinoCommand): Promise<void> {
    if (!this.connectedDevice) return;
    const jsonStr = JSON.stringify(cmd) + '\n';
    const base64Str = Buffer.from(jsonStr).toString('base64');
    try {
      await this.connectedDevice.writeCharacteristicWithResponseForService(
        HM10_SERVICE_UUID,
        HM10_CHAR_UUID,
        base64Str
      );
    } catch (e) {
      console.warn('BLE write error:', e);
    }
  }

  private parseTelemetryLine(line: string): void {
    try {
      const data = JSON.parse(line) as ArduinoTelemetry;
      if (data.type === 'TELEMETRY') {
        const telemetry: Telemetry = {
          positionLat: data.lat,
          positionLon: data.lon,
          positionAccuracyM: data.accuracy,
          batteryPct: data.battery,
          speedMps: data.speed,
          headingDeg: data.heading,
          gpsStatus: data.gpsStatus,
          missionProgressPct: data.progress,
          currentLaneIndex: data.lane,
          totalLanes: data.totalLanes,
          timestamp: Date.now(),
        };
        this.telemetryListeners.forEach((cb) => cb(telemetry));
      }
    } catch (e) {
      console.warn('Failed to parse telemetry line from BT:', line);
    }
  }

  private notifyStatus(): void {
    const status = this.getStatus();
    this.statusListeners.forEach((cb) => cb(status));
  }
}
