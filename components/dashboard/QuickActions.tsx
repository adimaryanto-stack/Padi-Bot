import React from 'react';
import { View, StyleSheet } from 'react-native';
import { Button } from '@/components/ui/Button';
import { Spacing } from '@/constants/theme';
import { Strings } from '@/constants/strings';

interface QuickActionsProps {
  onStartMission: () => void;
  onManualControl: () => void;
  onHistory: () => void;
  disableStart?: boolean;
}

export const QuickActions: React.FC<QuickActionsProps> = ({
  onStartMission,
  onManualControl,
  onHistory,
  disableStart = false,
}) => {
  return (
    <View style={styles.container}>
      <Button
        label={Strings.dashboardStartMission}
        onPress={onStartMission}
        disabled={disableStart}
        size="lg"
        style={styles.primaryAction}
      />

      <View style={styles.secondaryRow}>
        <Button
          label={Strings.dashboardManualControl}
          onPress={onManualControl}
          variant="secondary"
          style={styles.secondaryAction}
        />
        <Button
          label={Strings.dashboardHistory}
          onPress={onHistory}
          variant="secondary"
          style={styles.secondaryAction}
        />
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    gap: Spacing.md,
    marginBottom: Spacing.xl,
  },
  primaryAction: {
    width: '100%',
  },
  secondaryRow: {
    flexDirection: 'row',
    gap: Spacing.md,
  },
  secondaryAction: {
    flex: 1,
  },
});
