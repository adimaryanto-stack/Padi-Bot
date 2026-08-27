import React, { useEffect, useRef } from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import { useRouter } from 'expo-router';
import { useMachineStore } from '@/stores/machineStore';
import { createMachineConnection, MachineConnection } from '@/services/machineProtocol';
import { ManualCommand } from '@/types';
import { DPadControls } from '@/components/manual/DPadControls';
import { EmergencyStopButton } from '@/components/ui/EmergencyStopButton';
import { StatusDot } from '@/components/ui/StatusDot';
import { Button } from '@/components/ui/Button';
import { Colors, Typography, Spacing } from '@/constants/theme';
import { Strings } from '@/constants/strings';

export default function ManualControlScreen() {
  const router = useRouter();
  const connectionConfig = useMachineStore((state) => state.connectionConfig);
  const machineRef = useRef<MachineConnection | null>(null);

  useEffect(() => {
    const conn = createMachineConnection(connectionConfig);
    machineRef.current = conn;
    conn.connect().catch((e) => console.warn('Manual conn error:', e));

    return () => {
      conn.disconnect();
    };
  }, []);

  const handleCommand = (cmd: ManualCommand) => {
    if (machineRef.current) {
      machineRef.current.sendManualCommand(cmd).catch((e) => {
        console.warn('Failed to send manual cmd:', e);
      });
    }
  };

  const handleEmergencyStop = () => {
    if (machineRef.current) {
      machineRef.current.emergencyStop().catch(() => {});
    }
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Warning Banner */}
      <View style={styles.warningBanner}>
        <Text style={styles.warningText}>{Strings.manualWarning}</Text>
      </View>

      {/* Connection Indicator */}
      <View style={styles.statusRow}>
        <StatusDot status="connected" label={`Terhubung ke ${connectionConfig.type}`} />
      </View>

      {/* Interactive D-Pad */}
      <DPadControls onCommand={handleCommand} />

      {/* Emergency Stop Button */}
      <EmergencyStopButton
        onEmergencyStop={handleEmergencyStop}
        style={styles.emergencyBtn}
      />

      {/* Close Modal Button */}
      <Button
        label={Strings.close}
        onPress={() => router.back()}
        variant="ghost"
        style={styles.closeBtn}
      />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  content: {
    padding: Spacing.lg,
    paddingBottom: Spacing.xxxl,
    justifyContent: 'space-between',
  },
  warningBanner: {
    backgroundColor: '#FFF3E0',
    borderWidth: 1,
    borderColor: '#FFE0B2',
    borderRadius: 8,
    padding: Spacing.md,
    marginBottom: Spacing.md,
  },
  warningText: {
    ...Typography.bodyMedium,
    color: '#E65100',
    fontWeight: '600',
    textAlign: 'center',
  },
  statusRow: {
    alignItems: 'center',
    marginBottom: Spacing.md,
  },
  emergencyBtn: {
    marginTop: Spacing.xl,
  },
  closeBtn: {
    marginTop: Spacing.sm,
  },
});
