import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Mission } from '@/types';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Colors, Typography, Spacing } from '@/constants/theme';
import { Strings } from '@/constants/strings';

interface LastMissionCardProps {
  mission: Mission | null;
  onPress: () => void;
}

export const LastMissionCard: React.FC<LastMissionCardProps> = ({
  mission,
  onPress,
}) => {
  if (!mission) {
    return (
      <Card style={styles.card}>
        <Text style={styles.sectionTitle}>{Strings.dashboardLastMission}</Text>
        <Text style={styles.emptyText}>{Strings.dashboardNoMission}</Text>
      </Card>
    );
  }

  const formattedDate = new Date(mission.createdAt).toLocaleDateString('id-ID', {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });

  return (
    <Card style={styles.card} onPress={onPress}>
      <View style={styles.header}>
        <Text style={styles.sectionTitle}>{Strings.dashboardLastMission}</Text>
        <Badge status={mission.status} />
      </View>

      <Text style={styles.missionName}>{mission.name}</Text>
      <Text style={styles.fieldName}>
        {mission.fieldName} • {formattedDate}
      </Text>

      <View style={styles.footer}>
        <Text style={styles.coverageText}>
          {mission.actualCoveragePct ?? mission.estimatedCoveragePct}% {Strings.plantingCoverage}
        </Text>
        <Text style={styles.lanesText}>
          {mission.totalLanes} {Strings.plantingLanes}
        </Text>
      </View>
    </Card>
  );
};

const styles = StyleSheet.create({
  card: {
    marginBottom: Spacing.lg,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.xs,
  },
  sectionTitle: {
    ...Typography.labelMedium,
    color: Colors.textSecondary,
    fontWeight: '600',
  },
  emptyText: {
    ...Typography.bodyMedium,
    color: Colors.textDisabled,
    marginTop: Spacing.xs,
  },
  missionName: {
    ...Typography.titleMedium,
    color: Colors.textPrimary,
    fontWeight: '700',
    marginTop: 4,
  },
  fieldName: {
    ...Typography.bodyMedium,
    color: Colors.textSecondary,
    marginBottom: Spacing.sm,
  },
  footer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingTop: Spacing.sm,
    borderTopWidth: 1,
    borderTopColor: Colors.border,
  },
  coverageText: {
    ...Typography.bodyMedium,
    color: Colors.primary,
    fontWeight: '700',
  },
  lanesText: {
    ...Typography.bodyMedium,
    color: Colors.textSecondary,
  },
});
