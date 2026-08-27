import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  useWindowDimensions,
  Alert,
  Platform,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useFieldStore } from '@/stores/fieldStore';
import { useMissionStore } from '@/stores/missionStore';
import { FieldBoundaryCanvas } from '@/components/field/FieldBoundaryCanvas';
import { Button } from '@/components/ui/Button';
import { Colors, Typography, Spacing } from '@/constants/theme';
import { Strings } from '@/constants/strings';

export default function FieldDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const { width } = useWindowDimensions();

  const fields = useFieldStore((state) => state.fields);
  const activeFieldId = useFieldStore((state) => state.activeFieldId);
  const setActiveField = useFieldStore((state) => state.setActiveField);
  const removeField = useFieldStore((state) => state.removeField);

  const missions = useMissionStore((state) => state.missions);
  const field = fields.find((f) => f.id === id);
  const fieldMissions = missions.filter((m) => m.fieldId === id);

  if (!field) {
    return (
      <View style={styles.notFoundContainer}>
        <Text style={styles.notFoundText}>Sawah tidak ditemukan</Text>
        <Button label="Kembali" onPress={() => router.back()} style={{ marginTop: 16 }} />
      </View>
    );
  }

  const isActive = field.id === activeFieldId;
  const canvasWidth = Math.min(width - 32, 440);

  const handleDelete = () => {
    if (Platform.OS === 'web') {
      const ok = window.confirm(Strings.fieldDeleteConfirmBody(field.name));
      if (ok) {
        removeField(field.id);
        router.back();
      }
    } else {
      Alert.alert(
        Strings.fieldDeleteConfirmTitle,
        Strings.fieldDeleteConfirmBody(field.name),
        [
          { text: Strings.cancel, style: 'cancel' },
          {
            text: Strings.delete,
            style: 'destructive',
            onPress: () => {
              removeField(field.id);
              router.back();
            },
          },
        ]
      );
    }
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Header Info */}
      <View style={styles.header}>
        <View>
          <Text style={styles.title}>{field.name}</Text>
          <Text style={styles.subtitle}>
            Dibuat {new Date(field.createdAt).toLocaleDateString('id-ID')}
          </Text>
        </View>
        {!isActive && (
          <Button
            label="Jadikan Aktif"
            onPress={() => setActiveField(field.id)}
            variant="secondary"
            size="sm"
          />
        )}
      </View>

      {/* Field Polygon Visualizer */}
      <View style={styles.canvasWrapper}>
        <FieldBoundaryCanvas
          boundary={field.boundary}
          width={canvasWidth}
          height={240}
          showPoints={true}
        />
      </View>

      {/* Metrics Row */}
      <View style={styles.metricsCard}>
        <View style={styles.metric}>
          <Text style={styles.metricValue}>{Math.round(field.areaM2)} m²</Text>
          <Text style={styles.metricLabel}>{Strings.fieldAreaLabel}</Text>
        </View>
        <View style={styles.divider} />
        <View style={styles.metric}>
          <Text style={styles.metricValue}>{field.boundary.length}</Text>
          <Text style={styles.metricLabel}>Titik Batas</Text>
        </View>
        <View style={styles.divider} />
        <View style={styles.metric}>
          <Text style={styles.metricValue}>{Math.round(field.perimeterM)} m</Text>
          <Text style={styles.metricLabel}>{Strings.fieldPerimeterLabel}</Text>
        </View>
      </View>

      {/* Action Buttons */}
      <View style={styles.actions}>
        <Button
          label="🌾  Mulai Misi Tanam di Sawah Ini"
          onPress={() => {
            setActiveField(field.id);
            router.push('/mission/planting-settings');
          }}
          size="lg"
          style={{ width: '100%' }}
        />

        <Button
          label="Hapus Sawah"
          onPress={handleDelete}
          variant="danger"
          style={{ width: '100%', marginTop: 8 }}
        />
      </View>

      {/* Associated Missions */}
      <View style={styles.missionsSection}>
        <Text style={styles.sectionTitle}>
          Riwayat Misi Sawah ({fieldMissions.length})
        </Text>
        {fieldMissions.length === 0 ? (
          <Text style={styles.emptyMissions}>Belum ada misi tanam di sawah ini</Text>
        ) : (
          fieldMissions.map((m) => (
            <View key={m.id} style={styles.missionItem}>
              <View>
                <Text style={styles.missionName}>{m.name}</Text>
                <Text style={styles.missionDate}>
                  {new Date(m.createdAt).toLocaleDateString('id-ID')}
                </Text>
              </View>
              <Text style={styles.missionCoverage}>
                {m.actualCoveragePct ?? m.estimatedCoveragePct}% cakupan
              </Text>
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
  notFoundContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: Spacing.xl,
  },
  notFoundText: {
    ...Typography.titleLarge,
    color: Colors.textPrimary,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.md,
  },
  title: {
    ...Typography.headlineMedium,
    color: Colors.textPrimary,
    fontWeight: '800',
  },
  subtitle: {
    ...Typography.bodyMedium,
    color: Colors.textSecondary,
  },
  canvasWrapper: {
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
  metricValue: {
    ...Typography.titleMedium,
    color: Colors.textPrimary,
    fontWeight: '700',
  },
  metricLabel: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
    marginTop: 2,
  },
  divider: {
    width: 1,
    height: 28,
    backgroundColor: Colors.border,
  },
  actions: {
    gap: Spacing.sm,
    marginBottom: Spacing.xl,
  },
  missionsSection: {
    marginTop: Spacing.md,
  },
  sectionTitle: {
    ...Typography.titleMedium,
    color: Colors.textPrimary,
    fontWeight: '700',
    marginBottom: Spacing.sm,
  },
  emptyMissions: {
    ...Typography.bodyMedium,
    color: Colors.textDisabled,
  },
  missionItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    padding: Spacing.md,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: Colors.border,
    marginBottom: Spacing.xs,
  },
  missionName: {
    ...Typography.bodyLarge,
    fontWeight: '600',
    color: Colors.textPrimary,
  },
  missionDate: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
  },
  missionCoverage: {
    ...Typography.bodyMedium,
    color: Colors.primary,
    fontWeight: '700',
  },
});
