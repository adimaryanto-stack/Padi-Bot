import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Platform } from 'react-native';
import {
  ConnectionConfig,
  ConnectionType,
  MachineStatus,
  WiFiConfig,
  GSMConfig,
  BluetoothConfig,
} from '@/types';
import {
  DEFAULT_CONNECTION_CONFIG,
  WIFI_DEFAULT_PORT,
  MQTT_DEFAULT_BROKER,
  DEFAULT_MACHINE_WIDTH_M,
  DEFAULT_HEADLAND_WIDTH_M,
  DEFAULT_MAX_SPEED_MPS,
} from '@/constants/defaults';

interface MachineState {
  connectionConfig: ConnectionConfig;
  machineStatus: MachineStatus;
  wifiConfig: WiFiConfig;
  gsmConfig: GSMConfig;
  bluetoothConfig: BluetoothConfig | null;
  scannedBluetoothDevices: Array<{ id: string; name: string }>;
  defaultMachineWidthM: number;
  defaultHeadlandWidthM: number;
  defaultMaxSpeedMps: number;
  debugMode: boolean;

  setConnectionType: (type: ConnectionType) => void;
  setConnectionConfig: (config: ConnectionConfig) => void;
  setMachineStatus: (status: Partial<MachineStatus>) => void;
  setWifiConfig: (config: Partial<WiFiConfig>) => void;
  setGsmConfig: (config: Partial<GSMConfig>) => void;
  setBluetoothConfig: (config: BluetoothConfig | null) => void;
  setScannedBluetoothDevices: (devices: Array<{ id: string; name: string }>) => void;
  setDefaultMachineWidthM: (width: number) => void;
  setDefaultHeadlandWidthM: (width: number) => void;
  setDefaultMaxSpeedMps: (speed: number) => void;
  setDebugMode: (debug: boolean) => void;
  resetConnection: () => void;
}

export const useMachineStore = create<MachineState>()(
  persist(
    (set) => ({
      connectionConfig: DEFAULT_CONNECTION_CONFIG,
      machineStatus: {
        connectionStatus: 'DISCONNECTED',
        connectionType: 'SIMULATOR',
      },
      wifiConfig: {
        ipAddress: '192.168.4.1',
        port: WIFI_DEFAULT_PORT,
      },
      gsmConfig: {
        mqttBroker: MQTT_DEFAULT_BROKER,
        deviceId: 'padibot-001',
      },
      bluetoothConfig: null,
      scannedBluetoothDevices: [],
      defaultMachineWidthM: DEFAULT_MACHINE_WIDTH_M,
      defaultHeadlandWidthM: DEFAULT_HEADLAND_WIDTH_M,
      defaultMaxSpeedMps: DEFAULT_MAX_SPEED_MPS,
      debugMode: false,

      setConnectionType: (type) =>
        set((state) => ({
          connectionConfig: { ...state.connectionConfig, type },
          machineStatus: { ...state.machineStatus, connectionType: type },
        })),

      setConnectionConfig: (connectionConfig) =>
        set({ connectionConfig, machineStatus: { ...DEFAULT_CONNECTION_CONFIG, connectionStatus: 'DISCONNECTED', connectionType: connectionConfig.type } }),

      setMachineStatus: (statusUpdate) =>
        set((state) => ({
          machineStatus: { ...state.machineStatus, ...statusUpdate },
        })),

      setWifiConfig: (update) =>
        set((state) => ({
          wifiConfig: { ...state.wifiConfig, ...update },
        })),

      setGsmConfig: (update) =>
        set((state) => ({
          gsmConfig: { ...state.gsmConfig, ...update },
        })),

      setBluetoothConfig: (bluetoothConfig) => set({ bluetoothConfig }),

      setScannedBluetoothDevices: (scannedBluetoothDevices) => set({ scannedBluetoothDevices }),

      setDefaultMachineWidthM: (defaultMachineWidthM) => set({ defaultMachineWidthM }),
      setDefaultHeadlandWidthM: (defaultHeadlandWidthM) => set({ defaultHeadlandWidthM }),
      setDefaultMaxSpeedMps: (defaultMaxSpeedMps) => set({ defaultMaxSpeedMps }),
      setDebugMode: (debugMode) => set({ debugMode }),

      resetConnection: () =>
        set((state) => ({
          machineStatus: {
            connectionStatus: 'DISCONNECTED',
            connectionType: state.connectionConfig.type,
          },
        })),
    }),
    {
      name: 'padibot-machine-storage',
      storage: createJSONStorage(() => (Platform.OS === 'web' ? localStorage : AsyncStorage)),
    }
  )
);
