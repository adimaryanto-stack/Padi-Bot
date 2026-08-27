import { Mission, Telemetry, ManualCommand, MachineStatus, WiFiConfig, ArduinoCommand, ArduinoTelemetry } from '@/types';
import { MachineConnection } from './types';
import { WIFI_WEBSOCKET_PORT, WIFI_RECONNECT_INTERVAL_MS } from '@/constants/defaults';

export class WiFiConnection implements MachineConnection {
  private config: WiFiConfig;
  private ws: WebSocket | null = null;
  private connected: boolean = false;
  private reconnectTimer: NodeJS.Timeout | null = null;

  private telemetryListeners = new Set<(t: Telemetry) => void>();
  private errorListeners = new Set<(e: Error) => void>();
  private statusListeners = new Set<(s: MachineStatus) => void>();

  constructor(config: WiFiConfig) {
    this.config = config;
  }

  async connect(): Promise<void> {
    const wsUrl = `ws://${this.config.ipAddress}:${WIFI_WEBSOCKET_PORT}`;
    try {
      this.ws = new WebSocket(wsUrl);

      this.ws.onopen = () => {
        this.connected = true;
        this.notifyStatus();
      };

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data) as ArduinoTelemetry;
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
          console.error('Failed to parse Arduino telemetry over WiFi:', e);
        }
      };

      this.ws.onerror = (err) => {
        console.warn('WebSocket error:', err);
        this.errorListeners.forEach((cb) => cb(new Error('Koneksi WiFi WebSocket error')));
      };

      this.ws.onclose = () => {
        this.connected = false;
        this.notifyStatus();
        this.scheduleReconnect();
      };
    } catch (e) {
      this.connected = false;
      this.notifyStatus();
      throw e;
    }
  }

  async disconnect(): Promise<void> {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.ws) {
      this.ws.close();
      this.ws = null;
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
      connectionType: 'WIFI',
    };
  }

  async uploadMission(mission: Mission): Promise<void> {
    await this.sendHttpCommand({
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
    await this.sendHttpCommand({ cmd: 'START_MISSION', id: Date.now() });
  }

  async pauseMission(): Promise<void> {
    await this.sendHttpCommand({ cmd: 'PAUSE', id: Date.now() });
  }

  async resumeMission(): Promise<void> {
    await this.sendHttpCommand({ cmd: 'RESUME', id: Date.now() });
  }

  async stopMission(): Promise<void> {
    await this.sendHttpCommand({ cmd: 'STOP', id: Date.now() });
  }

  async emergencyStop(): Promise<void> {
    await this.sendHttpCommand({ cmd: 'E_STOP', id: Date.now() });
  }

  async sendManualCommand(cmd: ManualCommand): Promise<void> {
    await this.sendHttpCommand({
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

  private async sendHttpCommand(cmd: ArduinoCommand): Promise<void> {
    const url = `http://${this.config.ipAddress}:${this.config.port}/command`;
    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(cmd),
      });
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
    } catch (e) {
      console.warn(`Failed to send command ${cmd.cmd} over HTTP to ${url}:`, e);
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    this.reconnectTimer = setTimeout(() => {
      if (!this.connected) {
        this.connect().catch(() => {});
      }
    }, WIFI_RECONNECT_INTERVAL_MS);
  }

  private notifyStatus(): void {
    const status = this.getStatus();
    this.statusListeners.forEach((cb) => cb(status));
  }
}
