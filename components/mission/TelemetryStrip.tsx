import React from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import { Telemetry } from '@/types';
import { Colors, Typography, Spacing } from '@/constants/theme';
import { Strings } from '@/constants/strings';

interface TelemetryStripProps {
  telemetry: Telemetry | null;
}

export const TelemetryStrip: React.FC<TelemetryStripProps> = ({ telemetry }) => {
  const battery = telemetry ? Math.round(telemetry.batteryPct) : 85;
  const speed = telemetry ? telemetry.speedMps.toFixed(1) : '0.0';
  const gpsStatus = telemetry?.gpsStatus ?? 'GPS';
  const accuracy = telemetry ? `±${telemetry.positionAccuracyM.toFixed(1)}m` : '±1.2m';
  const laneText = telemetry ? `${telemetry.currentLaneIndex + 1}/${telemetry.totalLanes}` : '-/-';

  const getBatteryColor = () => {
    if (battery <= 15) return Colors.error;
    if (battery <= 30) return Colors.warning;
    return Colors.success;
  };

  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      contentContainerStyle={styles.container}
    >
      <View style={styles.card}>
        <Text style={styles.icon}>🔋</Text>
        <Text style={[styles.value, { color: getBatteryColor() }]}>{battery}%</Text>
        <Text style={styles.label}>{Strings.telemetryBattery}</Text>
      </View>

      <View style={styles.card}>
        <Text style={styles.icon}>📍</Text>
        <Text style={styles.value}>{gpsStatus}</Text>
        <Text style={styles.label}>{accuracy}</Text>
      </View>

      <View style={styles.card}>
        <Text style={styles.icon}>⚡</Text>
        <Text style={styles.value}>{speed} m/s</Text>
        <Text style={styles.label}>{Strings.telemetrySpeed}</Text>
      </View>

      <View style={styles.card}>
        <Text style={styles.icon}>🛤️</Text>
        <Text style={styles.value}>{laneText}</Text>
        <Text style={styles.label}>{Strings.telemetryLane}</Text>
      </View>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    gap: Spacing.sm,
    paddingVertical: Spacing.xs,
  },
  card: {
    backgroundColor: Colors.surface,
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    minWidth: 84,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  icon: {
    fontSize: 16,
    marginBottom: 2,
  },
  value: {
    ...Typography.bodyLarge,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  label: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
    fontSize: 10,
    marginTop: 2,
  },
});
