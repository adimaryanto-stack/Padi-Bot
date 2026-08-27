import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  useWindowDimensions,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useMissionStore } from '@/stores/missionStore';
import { useFieldStore } from '@/stores/fieldStore';
import { MissionEvent } from '@/types';
import { getMissionEvents } from '@/db/queries/telemetry';
import { Badge } from '@/components/ui/Badge';
import { RouteCanvas } from '@/components/mission/RouteCanvas';
import { Button } from '@/components/ui/Button';
import { Colors, Typography, Spacing } from '@/constants/theme';
import { Strings } from '@/constants/strings';

export default function MissionDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const { width } = useWindowDimensions();

  const missions = useMissionStore((state) => state.missions);
  const fields = useFieldStore((state) => state.fields);

  const mission = missions.find((m) => m.id === id);
  const field = fields.find((f) => f.id === mission?.fieldId);

  const [events, setEvents] = useState<MissionEvent[]>([]);

  useEffect(() => {
    if (id) {
      getMissionEvents(id).then(setEvents);
    }
  }, [id]);

  if (!mission) {
    return (
      <View style={styles.notFound}>
        <Text style={styles.notFoundText}>Misi tidak ditemukan</Text>
        <Button label="Kembali" onPress={() => router.back()} style={{ marginTop: 16 }} />
      </View>
    );
  }

  const canvasWidth = Math.min(width - 32, 440);
  const boundary = field ? field.boundary : [];

  const durationSec = mission.completedAt && mission.startedAt
    ? Math.round((mission.completedAt - mission.startedAt) / 1000)
    : 0;
  const durationMin = Math.floor(durationSec / 60);
  const durationRemainderSec = durationSec % 60;

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Header Card */}
      <View style={styles.headerCard}>
        <View style={styles.headerRow}>
          <Text style={styles.missionName}>{mission.name}</Text>
          <Badge status={mission.status} />
        </View>
        <Text style={styles.fieldSubtitle}>{mission.fieldName}</Text>
        <Text style={styles.timestamp}>
          {new Date(mission.createdAt).toLocaleDateString('id-ID', {
            day: 'numeric',
            month: 'long',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          })}
        </Text>
      </View>

      {/* 2x2 Stats Grid */}
      <View style={styles.statsGrid}>
        <View style={styles.statCard}>
          <Text style={styles.statValue}>
            {mission.actualCoveragePct ?? mission.estimatedCoveragePct}%
          </Text>
          <Text style={styles.statLabel}>{Strings.missionDetailCoverage}</Text>
        </View>

        <View style={styles.statCard}>
          <Text style={styles.statValue}>
            {field ? Math.round(field.areaM2) : '-'} m²
          </Text>
          <Text style={styles.statLabel}>{Strings.missionDetailArea}</Text>
        </View>

        <View style={styles.statCard}>
          <Text style={styles.statValue}>
            {durationMin > 0 ? `${durationMin}m ${durationRemainderSec}s` : '0m 0s'}
          </Text>
          <Text style={styles.statLabel}>Durasi Pengerjaan</Text>
        </View>

        <View style={styles.statCard}>
          <Text style={styles.statValue}>
            {mission.totalLanes}/{mission.totalLanes}
          </Text>
          <Text style={styles.statLabel}>{Strings.missionDetailLanes}</Text>
        </View>
      </View>

      {/* Final Route Visualizer */}
      <View style={styles.routeSection}>
        <Text style={styles.sectionTitle}>Peta Jalur Misi</Text>
        <RouteCanvas
          boundary={boundary}
          waypoints={mission.route}
          width={canvasWidth}
          height={260}
          completedLaneIndex={mission.totalLanes} // all completed
        />
      </View>

      {/* Mission Events Log */}
      <View style={styles.logSection}>
        <Text style={styles.sectionTitle}>{Strings.missionDetailLog}</Text>
        {events.length === 0 ? (
          <Text style={styles.emptyLog}>Tidak ada catatan event pada misi ini</Text>
        ) : (
          events.map((ev) => (
            <View key={ev.id} style={styles.logItem}>
              <Text style={styles.logTime}>
                {new Date(ev.timestamp).toLocaleTimeString('id-ID')}
              </Text>
              <Text style={styles.logBadge}>[{ev.eventType}]</Text>
              <Text style={styles.logMsg}>{ev.message}</Text>
            </View>
          ))
        )}
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
  notFound: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: Spacing.xl,
  },
  notFoundText: {
    ...Typography.titleLarge,
    color: Colors.textPrimary,
  },
  headerCard: {
    backgroundColor: Colors.surface,
    padding: Spacing.md,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: Colors.border,
    marginBottom: Spacing.md,
  },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  missionName: {
    ...Typography.titleLarge,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  fieldSubtitle: {
    ...Typography.bodyLarge,
    color: Colors.textSecondary,
    marginTop: 2,
  },
  timestamp: {
    ...Typography.labelSmall,
    color: Colors.textDisabled,
    marginTop: 4,
  },
  statsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.sm,
    marginBottom: Spacing.lg,
  },
  statCard: {
    flex: 1,
    minWidth: '45%',
    backgroundColor: Colors.surface,
    padding: Spacing.md,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: Colors.border,
    alignItems: 'center',
  },
  statValue: {
    ...Typography.headlineSmall,
    color: Colors.primary,
    fontWeight: '800',
  },
  statLabel: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
    marginTop: 4,
  },
  routeSection: {
    marginBottom: Spacing.lg,
  },
  sectionTitle: {
    ...Typography.titleMedium,
    color: Colors.textPrimary,
    fontWeight: '700',
    marginBottom: Spacing.sm,
  },
  logSection: {
    backgroundColor: Colors.surface,
    padding: Spacing.md,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  emptyLog: {
    ...Typography.bodyMedium,
    color: Colors.textDisabled,
  },
  logItem: {
    flexDirection: 'row',
    gap: 8,
    paddingVertical: 6,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  logTime: {
    ...Typography.labelSmall,
    color: Colors.textDisabled,
  },
  logBadge: {
    ...Typography.labelSmall,
    color: Colors.primaryDark,
    fontWeight: '700',
  },
  logMsg: {
    ...Typography.bodyMedium,
    color: Colors.textPrimary,
    flex: 1,
  },
});
