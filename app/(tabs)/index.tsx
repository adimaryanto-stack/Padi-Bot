import React from 'react';
import { View, Text, StyleSheet, ScrollView, RefreshControl } from 'react-native';
import { useRouter } from 'expo-router';
import { useFieldStore } from '@/stores/fieldStore';
import { useMissionStore } from '@/stores/missionStore';
import { useMachineStore } from '@/stores/machineStore';
import { MachineStatusCard } from '@/components/dashboard/MachineStatusCard';
import { FieldSummaryCard } from '@/components/dashboard/FieldSummaryCard';
import { LastMissionCard } from '@/components/dashboard/LastMissionCard';
import { QuickActions } from '@/components/dashboard/QuickActions';
import { Colors, Typography, Spacing } from '@/constants/theme';
import { Strings } from '@/constants/strings';

export default function DashboardScreen() {
  const router = useRouter();

  const getActiveField = useFieldStore((state) => state.getActiveField);
  const activeField = getActiveField();
  const loadFields = useFieldStore((state) => state.loadFromDB);

  const missions = useMissionStore((state) => state.missions);
  const lastMission = missions.length > 0 ? missions[0] : null;
  const loadMissions = useMissionStore((state) => state.loadFromDB);

  const machineStatus = useMachineStore((state) => state.machineStatus);
  const connectionConfig = useMachineStore((state) => state.connectionConfig);

  const [refreshing, setRefreshing] = React.useState(false);

  const onRefresh = async () => {
    setRefreshing(true);
    await Promise.all([loadFields(), loadMissions()]);
    setRefreshing(false);
  };

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={styles.content}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
    >
      {/* Welcome Banner */}
      <View style={styles.welcomeBanner}>
        <Text style={styles.welcomeSubtitle}>{Strings.dashboardWelcome}</Text>
        <Text style={styles.welcomeTitle}>{Strings.appName}</Text>
        <Text style={styles.tagline}>{Strings.tagline}</Text>
      </View>

      {/* Machine Status Card */}
      <MachineStatusCard
        machineStatus={machineStatus}
        connectionConfig={connectionConfig}
        onPress={() => router.push('/(tabs)/settings')}
      />

      {/* Active Field Summary */}
      <FieldSummaryCard
        field={activeField}
        onChangeField={() => router.push('/(tabs)/fields')}
      />

      {/* Last Mission */}
      <LastMissionCard
        mission={lastMission}
        onPress={() => {
          if (lastMission) {
            router.push(`/(tabs)/history/${lastMission.id}`);
          }
        }}
      />

      {/* Quick Action Buttons */}
      <QuickActions
        onStartMission={() => {
          if (activeField) {
            router.push('/mission/planting-settings');
          } else {
            router.push('/(tabs)/fields/create');
          }
        }}
        onManualControl={() => router.push('/manual-control')}
        onHistory={() => router.push('/(tabs)/history')}
        disableStart={false}
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
  },
  welcomeBanner: {
    marginBottom: Spacing.lg,
  },
  welcomeSubtitle: {
    ...Typography.bodyMedium,
    color: Colors.textSecondary,
  },
  welcomeTitle: {
    ...Typography.headlineLarge,
    color: Colors.textPrimary,
    fontWeight: '800',
  },
  tagline: {
    ...Typography.bodyMedium,
    color: Colors.primaryDark,
    fontWeight: '600',
    marginTop: 2,
  },
});
