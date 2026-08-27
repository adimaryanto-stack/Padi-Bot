import { Mission, Telemetry, ManualCommand, MachineStatus, Waypoint } from '@/types';
import { MachineConnection } from './types';
import { SIMULATOR_SPEED_MPS, SIMULATOR_TELEMETRY_INTERVAL_MS } from '@/constants/defaults';
import * as Geom from '../routePlanner/geometry';

export class SimulatorConnection implements MachineConnection {
  private connected: boolean = false;
  private currentMission: Mission | null = null;
  private waypoints: Waypoint[] = [];
  private currentWaypointIndex: number = 0;
  private isMissionRunning: boolean = false;
  private isMissionPaused: boolean = false;

  private currentLat: number = -6.9234;
  private currentLon: number = 107.6100;
  private currentBattery: number = 88;
  private currentSpeed: number = 0;
  private currentHeading: number = 0;
  private timer: NodeJS.Timeout | null = null;

  private telemetryListeners = new Set<(t: Telemetry) => void>();
  private errorListeners = new Set<(e: Error) => void>();
  private statusListeners = new Set<(s: MachineStatus) => void>();

  async connect(): Promise<void> {
    this.connected = true;
    this.notifyStatus();
    this.startTelemetryLoop();
  }

  async disconnect(): Promise<void> {
    this.connected = false;
    this.isMissionRunning = false;
    this.isMissionPaused = false;
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
    this.notifyStatus();
  }

  isConnected(): boolean {
    return this.connected;
  }

  getStatus(): MachineStatus {
    return {
      connectionStatus: this.connected ? 'CONNECTED' : 'DISCONNECTED',
      connectionType: 'SIMULATOR',
      lastTelemetry: this.generateTelemetry(),
    };
  }

  async uploadMission(mission: Mission): Promise<void> {
    this.currentMission = mission;
    this.waypoints = mission.route;
    this.currentWaypointIndex = 0;
    if (this.waypoints.length > 0) {
      this.currentLat = this.waypoints[0].lat;
      this.currentLon = this.waypoints[0].lon;
    }
  }

  async startMission(): Promise<void> {
    if (!this.connected) throw new Error('Simulator tidak terhubung');
    this.isMissionRunning = true;
    this.isMissionPaused = false;
    this.currentSpeed = SIMULATOR_SPEED_MPS;
    this.notifyStatus();
  }

  async pauseMission(): Promise<void> {
    this.isMissionPaused = true;
    this.currentSpeed = 0;
    this.notifyStatus();
  }

  async resumeMission(): Promise<void> {
    this.isMissionPaused = false;
    this.currentSpeed = SIMULATOR_SPEED_MPS;
    this.notifyStatus();
  }

  async stopMission(): Promise<void> {
    this.isMissionRunning = false;
    this.isMissionPaused = false;
    this.currentSpeed = 0;
    this.notifyStatus();
  }

  async emergencyStop(): Promise<void> {
    await this.stopMission();
  }

  async sendManualCommand(cmd: ManualCommand): Promise<void> {
    if (cmd.direction === 'STOP') {
      this.currentSpeed = 0;
    } else {
      this.currentSpeed = (cmd.intensity / 100) * SIMULATOR_SPEED_MPS;
      const step = 0.00001 * (cmd.intensity / 100);
      if (cmd.direction === 'FORWARD') this.currentLat += step;
      if (cmd.direction === 'BACKWARD') this.currentLat -= step;
      if (cmd.direction === 'LEFT') this.currentLon -= step;
      if (cmd.direction === 'RIGHT') this.currentLon += step;
    }
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

  private startTelemetryLoop(): void {
    if (this.timer) clearInterval(this.timer);

    this.timer = setInterval(() => {
      if (!this.connected) return;

      // Advance mission simulation
      if (this.isMissionRunning && !this.isMissionPaused && this.waypoints.length > 0) {
        this.stepMissionSimulation();
      }

      // Battery slow drain
      this.currentBattery = Math.max(1, this.currentBattery - 0.01);

      const telemetry = this.generateTelemetry();
      this.telemetryListeners.forEach((listener) => {
        try {
          listener(telemetry);
        } catch (e) {
          console.error('Error in telemetry listener:', e);
        }
      });
    }, SIMULATOR_TELEMETRY_INTERVAL_MS);
  }

  private stepMissionSimulation(): void {
    if (this.currentWaypointIndex >= this.waypoints.length) {
      this.isMissionRunning = false;
      this.currentSpeed = 0;
      this.notifyStatus();
      return;
    }

    const targetWp = this.waypoints[this.currentWaypointIndex];
    const distToTarget = Geom.distanceM(
      { lat: this.currentLat, lon: this.currentLon },
      { lat: targetWp.lat, lon: targetWp.lon }
    );

    const stepDist = SIMULATOR_SPEED_MPS * (SIMULATOR_TELEMETRY_INTERVAL_MS / 1000);

    if (distToTarget <= stepDist) {
      // Reached waypoint
      this.currentLat = targetWp.lat;
      this.currentLon = targetWp.lon;
      this.currentWaypointIndex++;
    } else {
      // Move towards waypoint
      const ratio = stepDist / distToTarget;
      this.currentLat += (targetWp.lat - this.currentLat) * ratio;
      this.currentLon += (targetWp.lon - this.currentLon) * ratio;
    }
  }

  private generateTelemetry(): Telemetry {
    const totalWaypoints = this.waypoints.length || 1;
    const progress = Math.min(100, Math.round((this.currentWaypointIndex / totalWaypoints) * 100));
    const currentWp = this.waypoints[Math.min(this.currentWaypointIndex, this.waypoints.length - 1)];

    return {
      positionLat: this.currentLat,
      positionLon: this.currentLon,
      positionAccuracyM: 1.2 + (Math.random() * 0.4 - 0.2),
      batteryPct: Math.round(this.currentBattery),
      speedMps: Math.round(this.currentSpeed * 100) / 100,
      headingDeg: this.currentHeading,
      gpsStatus: 'GPS',
      missionProgressPct: progress,
      currentLaneIndex: currentWp?.laneIndex ?? 0,
      totalLanes: this.currentMission?.totalLanes ?? 1,
      timestamp: Date.now(),
    };
  }

  private notifyStatus(): void {
    const status = this.getStatus();
    this.statusListeners.forEach((listener) => {
      try {
        listener(status);
      } catch (e) {
        console.error('Error in status listener:', e);
      }
    });
  }
}
