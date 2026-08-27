import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  Pressable,
  ScrollView,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useMissionStore } from '@/stores/missionStore';
import { MissionStatus } from '@/types';
import { Badge } from '@/components/ui/Badge';
import { EmptyState } from '@/components/ui/EmptyState';
import { Colors, Typography, Spacing } from '@/constants/theme';
import { Strings } from '@/constants/strings';

type FilterType = 'ALL' | 'COMPLETED' | 'STOPPED' | 'ERROR';

export default function MissionHistoryScreen() {
  const router = useRouter();
  const missions = useMissionStore((state) => state.missions);
  const [filter, setFilter] = useState<FilterType>('ALL');

  const filteredMissions = missions.filter((m) => {
    if (filter === 'ALL') return true;
    if (filter === 'COMPLETED') return m.status === 'COMPLETED';
    if (filter === 'STOPPED') return m.status === 'STOPPED';
    if (filter === 'ERROR') return m.status === 'ERROR';
    return true;
  });

  return (
    <View style={styles.container}>
      {/* Filter Chips */}
      <View style={styles.filterContainer}>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.filterRow}>
          <Pressable
            onPress={() => setFilter('ALL')}
            style={[styles.chip, filter === 'ALL' && styles.chipActive]}
          >
            <Text style={[styles.chipText, filter === 'ALL' && styles.chipTextActive]}>
              {Strings.historyFilterAll} ({missions.length})
            </Text>
          </Pressable>

          <Pressable
            onPress={() => setFilter('COMPLETED')}
            style={[styles.chip, filter === 'COMPLETED' && styles.chipActive]}
          >
            <Text style={[styles.chipText, filter === 'COMPLETED' && styles.chipTextActive]}>
              {Strings.historyFilterCompleted}
            </Text>
          </Pressable>

          <Pressable
            onPress={() => setFilter('STOPPED')}
            style={[styles.chip, filter === 'STOPPED' && styles.chipActive]}
          >
            <Text style={[styles.chipText, filter === 'STOPPED' && styles.chipTextActive]}>
              {Strings.historyFilterStopped}
            </Text>
          </Pressable>

          <Pressable
            onPress={() => setFilter('ERROR')}
            style={[styles.chip, filter === 'ERROR' && styles.chipActive]}
          >
            <Text style={[styles.chipText, filter === 'ERROR' && styles.chipTextActive]}>
              {Strings.historyFilterError}
            </Text>
          </Pressable>
        </ScrollView>
      </View>

      {/* Mission List */}
      <FlatList
        data={filteredMissions}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.listContent}
        renderItem={({ item }) => {
          const dateStr = new Date(item.createdAt).toLocaleDateString('id-ID', {
            day: 'numeric',
            month: 'short',
            hour: '2-digit',
            minute: '2-digit',
          });

          return (
            <Pressable
              onPress={() => router.push(`/(tabs)/history/${item.id}`)}
              style={({ pressed }) => [styles.itemCard, pressed && styles.itemPressed]}
            >
              <View style={styles.cardHeader}>
                <Badge status={item.status} />
                <Text style={styles.dateText}>{dateStr}</Text>
              </View>

              <Text style={styles.missionName}>{item.name}</Text>
              <Text style={styles.fieldName}>{item.fieldName}</Text>

              <View style={styles.cardFooter}>
                <Text style={styles.coverageText}>
                  {item.actualCoveragePct ?? item.estimatedCoveragePct}% {Strings.plantingCoverage}
                </Text>
                <Text style={styles.lanesText}>
                  {item.totalLanes} {Strings.plantingLanes} • {item.route.length} titik
                </Text>
              </View>
            </Pressable>
          );
        }}
        ListEmptyComponent={
          <EmptyState
            icon="📋"
            title={Strings.historyEmpty}
            description={Strings.historyEmptyDesc}
            actionLabel="Mulai Misi Baru"
            onAction={() => router.push('/mission/planting-settings')}
          />
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  filterContainer: {
    backgroundColor: Colors.surface,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
    paddingVertical: Spacing.sm,
  },
  filterRow: {
    paddingHorizontal: Spacing.md,
    gap: Spacing.xs,
  },
  chip: {
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 20,
    backgroundColor: Colors.surfaceVariant,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  chipActive: {
    backgroundColor: Colors.primary,
    borderColor: Colors.primaryDark,
  },
  chipText: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
    fontWeight: '600',
  },
  chipTextActive: {
    color: '#FFFFFF',
  },
  listContent: {
    padding: Spacing.md,
    flexGrow: 1,
  },
  itemCard: {
    backgroundColor: Colors.surface,
    borderRadius: 12,
    padding: Spacing.md,
    marginBottom: Spacing.sm,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  itemPressed: {
    opacity: 0.9,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.xs,
  },
  dateText: {
    ...Typography.labelSmall,
    color: Colors.textDisabled,
  },
  missionName: {
    ...Typography.titleMedium,
    color: Colors.textPrimary,
    fontWeight: '700',
    marginTop: 2,
  },
  fieldName: {
    ...Typography.bodyMedium,
    color: Colors.textSecondary,
    marginBottom: Spacing.xs,
  },
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingTop: Spacing.sm,
    borderTopWidth: 1,
    borderTopColor: Colors.border,
    marginTop: 4,
  },
  coverageText: {
    ...Typography.bodyMedium,
    color: Colors.primary,
    fontWeight: '700',
  },
  lanesText: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
  },
});
