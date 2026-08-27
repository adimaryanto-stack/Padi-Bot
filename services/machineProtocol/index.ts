import { ConnectionConfig } from '@/types';
import { MachineConnection } from './types';
import { SimulatorConnection } from './SimulatorConnection';
import { WiFiConnection } from './WiFiConnection';
import { BluetoothConnection } from './BluetoothConnection';

let activeConnection: MachineConnection | null = null;

export function createMachineConnection(config: ConnectionConfig): MachineConnection {
  // If changing connection type or not created, instantiate new
  switch (config.type) {
    case 'WIFI':
      if (config.wifi) {
        return new WiFiConnection(config.wifi);
      }
      return new WiFiConnection({ ipAddress: '192.168.4.1', port: 80 });

    case 'BLUETOOTH':
      return new BluetoothConnection(config.bluetooth ?? undefined);

    case 'SIMULATOR':
    default:
      return new SimulatorConnection();
  }
}

export function getActiveMachineConnection(config?: ConnectionConfig): MachineConnection {
  if (!activeConnection) {
    activeConnection = createMachineConnection(config || { type: 'SIMULATOR' });
  }
  return activeConnection;
}

export type { MachineConnection };
