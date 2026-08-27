import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { MachineStatus, ConnectionConfig } from '@/types';
import { Card } from '@/components/ui/Card';
import { StatusDot } from '@/components/ui/StatusDot';
import { ProgressBar } from '@/components/ui/ProgressBar';
import { Colors, Typography, Spacing } from '@/constants/theme';
import { Strings } from '@/constants/strings';

interface MachineStatusCardProps {
  machineStatus: MachineStatus;
  connectionConfig: ConnectionConfig;
  onPress?: () => void;
}

export const MachineStatusCard: React.FC<MachineStatusCardProps> = ({
  machineStatus,
  connectionConfig,
  onPress,
}) => {
  const isConnected = machineStatus.connectionStatus === 'CONNECTED';
  const battery = machineStatus.lastTelemetry?.batteryPct ?? 85;
  const gpsStatus = machineStatus.lastTelemetry?.gpsStatus ?? 'GPS';
  const accuracy = machineStatus.lastTelemetry?.positionAccuracyM ?? 1.2;

  const getBatteryColor = () => {
    if (battery <= 15) return Colors.error;
    if (battery <= 30) return Colors.warning;
    return Colors.success;
  };

  const getStatusLabel = () => {
    if (connectionConfig.type === 'SIMULATOR') return Strings.machineSimulator;
    if (isConnected) return `${connectionConfig.type} ${Strings.machineConnected}`;
    if (machineStatus.connectionStatus === 'CONNECTING') return Strings.machineConnecting;
    return Strings.machineDisconnected;
  };

  return (
    <Card style={styles.card} onPress={onPress}>
      <View style={styles.header}>
        <View style={styles.titleRow}>
          <Text style={styles.icon}>🤖</Text>
          <View>
            <Text style={styles.title}>{Strings.dashboardMachineSection}</Text>
            <Text style={styles.connectionType}>{getStatusLabel()}</Text>
          </View>
        </View>
        <StatusDot
          status={
            isConnected || connectionConfig.type === 'SIMULATOR'
              ? 'connected'
              : machineStatus.connectionStatus === 'CONNECTING'
              ? 'connecting'
              : 'disconnected'
          }
        />
      </View>

      <View style={styles.metricsRow}>
        <View style={styles.metricItem}>
          <Text style={styles.metricLabel}>🔋 {Strings.telemetryBattery}</Text>
          <ProgressBar
            progress={battery}
            color={getBatteryColor()}
            height={8}
            style={styles.batteryProgress}
          />
        </View>

        <View style={styles.metricDivider} />

        <View style={styles.metricItem}>
          <Text style={styles.metricLabel}>📍 {Strings.telemetryGps}</Text>
          <Text style={styles.gpsValue}>
            {gpsStatus} (±{accuracy.toFixed(1)}m)
          </Text>
        </View>
      </View>
    </Card>
  );
};

const styles = StyleSheet.create({
  card: {
    marginBottom: Spacing.md,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.md,
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  icon: {
    fontSize: 28,
  },
  title: {
    ...Typography.titleMedium,
    color: Colors.textPrimary,
    fontWeight: '700',
  },
  connectionType: {
    ...Typography.bodyMedium,
    color: Colors.textSecondary,
  },
  metricsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surfaceVariant,
    padding: Spacing.md,
    borderRadius: 8,
    gap: 16,
  },
  metricItem: {
    flex: 1,
  },
  metricDivider: {
    width: 1,
    height: 32,
    backgroundColor: Colors.border,
  },
  metricLabel: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
    marginBottom: 4,
  },
  batteryProgress: {
    marginTop: 2,
  },
  gpsValue: {
    ...Typography.bodyMedium,
    color: Colors.textPrimary,
    fontWeight: '600',
  },
});
