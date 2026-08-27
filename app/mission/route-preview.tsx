import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  useWindowDimensions,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useFieldStore } from '@/stores/fieldStore';
import { useMissionStore } from '@/stores/missionStore';
import { RouteCanvas } from '@/components/mission/RouteCanvas';
import { Button } from '@/components/ui/Button';
import { Colors, Typography, Spacing } from '@/constants/theme';
import { Strings } from '@/constants/strings';

export default function RoutePreviewScreen() {
  const router = useRouter();
  const { width } = useWindowDimensions();

  const params = useLocalSearchParams<{
    machineWidth: string;
    headlandWidth: string;
    orientationDeg: string;
    pattern?: string;
  }>();

  const getActiveField = useFieldStore((state) => state.getActiveField);
  const activeField = getActiveField();

  const pendingRoute = useMissionStore((state) => state.pendingRoute);
  const addMission = useMissionStore((state) => state.addMission);
  const setActiveMission = useMissionStore((state) => state.setActiveMission);

  const [isApproving, setIsApproving] = useState(false);

  if (!activeField || !pendingRoute) {
    return (
      <View style={styles.emptyContainer}>
        <Text style={styles.emptyText}>Data rute tidak ditemukan</Text>
        <Button label="Kembali" onPress={() => router.back()} style={{ marginTop: 16 }} />
      </View>
    );
  }

  const canvasWidth = Math.min(width - 32, 440);

  const getPatternLabel = () => {
    const pat = params.pattern || pendingRoute.pattern || 'BOUSTROPHEDON';
    if (pat === 'HEADLAND_INNER') return '🔄 2. Dengan Headland (Area Putar)';
    if (pat === 'SPIRAL_INWARD') return '🌀 3. Oval/Spiral (Pinggir ke Tengah)';
    if (pat === 'SPIRAL_OUTWARD') return '💫 4. Oval/Spiral (Tengah ke Pinggir)';
    return '⚡ 1. Persegi Panjang (Boustrophedon)';
  };

  const handleApprove = async () => {
    setIsApproving(true);
    try {
      const now = new Date();
      const missionName = `Misi ${activeField.name} #${now.getDate()}/${now.getMonth() + 1}`;

      const newMission = await addMission({
        fieldId: activeField.id,
        fieldName: activeField.name,
        name: missionName,
        status: 'READY',
        route: pendingRoute.waypoints,
        machineWidthM: parseFloat(params.machineWidth) || 1.5,
        headlandWidthM: parseFloat(params.headlandWidth) || 3.0,
        laneOrientationDeg: parseFloat(params.orientationDeg) || 0,
        totalLanes: pendingRoute.totalLanes,
        estimatedCoveragePct: pendingRoute.estimatedCoveragePct,
      });

      setActiveMission(newMission);
      router.replace('/mission/execution');
    } catch (e) {
      console.error('Approve mission error:', e);
    } finally {
      setIsApproving(false);
    }
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Pattern & Field Badge Header */}
      <View style={styles.headerInfoCard}>
        <View>
          <Text style={styles.headerFieldTitle}>{activeField.name}</Text>
          <Text style={styles.headerPatternText}>{getPatternLabel()}</Text>
        </View>
        <Text style={styles.headerOrientationText}>{params.orientationDeg || 0}° orientasi</Text>
      </View>

      {/* Route Canvas */}
      <View style={styles.canvasContainer}>
        <RouteCanvas
          boundary={activeField.boundary}
          waypoints={pendingRoute.waypoints}
          width={canvasWidth}
          height={320}
        />
      </View>

      {/* Metrics Strip */}
      <View style={styles.metricsCard}>
        <View style={styles.metric}>
          <Text style={styles.metricVal}>{pendingRoute.totalLanes}</Text>
          <Text style={styles.metricLbl}>{Strings.routeLanesLabel}</Text>
        </View>
        <View style={styles.divider} />
        <View style={styles.metric}>
          <Text style={styles.metricVal}>{pendingRoute.totalDistanceM} m</Text>
          <Text style={styles.metricLbl}>{Strings.routeDistanceLabel}</Text>
        </View>
        <View style={styles.divider} />
        <View style={styles.metric}>
          <Text style={styles.metricVal}>{pendingRoute.estimatedCoveragePct}%</Text>
          <Text style={styles.metricLbl}>{Strings.routeCoverageLabel}</Text>
        </View>
      </View>

      {/* Action Buttons */}
      <View style={styles.actions}>
        <Button
          label={Strings.routeApprove}
          onPress={handleApprove}
          loading={isApproving}
          size="lg"
          style={{ width: '100%' }}
        />

        <Button
          label={Strings.routeEdit}
          onPress={() => router.back()}
          variant="secondary"
          style={{ width: '100%', marginTop: 8 }}
        />
      </View>
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
  },
  emptyContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: Spacing.xl,
  },
  emptyText: {
    ...Typography.titleLarge,
    color: Colors.textPrimary,
  },
  headerInfoCard: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    padding: Spacing.md,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: Colors.border,
    marginBottom: Spacing.md,
  },
  headerFieldTitle: {
    ...Typography.titleMedium,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  headerPatternText: {
    ...Typography.labelSmall,
    color: Colors.primaryDark,
    fontWeight: '700',
    marginTop: 2,
  },
  headerOrientationText: {
    ...Typography.bodyMedium,
    color: Colors.textSecondary,
    fontWeight: '600',
  },
  canvasContainer: {
    alignItems: 'center',
    marginBottom: Spacing.md,
  },
  metricsCard: {
    flexDirection: 'row',
    backgroundColor: Colors.surface,
    borderRadius: 12,
    padding: Spacing.md,
    borderWidth: 1,
    borderColor: Colors.border,
    justifyContent: 'space-around',
    marginBottom: Spacing.lg,
  },
  metric: {
    alignItems: 'center',
  },
  metricVal: {
    ...Typography.titleLarge,
    color: Colors.textPrimary,
    fontWeight: '800',
  },
  metricLbl: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
    marginTop: 2,
  },
  divider: {
    width: 1,
    height: 32,
    backgroundColor: Colors.border,
  },
  actions: {
    gap: Spacing.sm,
  },
});
