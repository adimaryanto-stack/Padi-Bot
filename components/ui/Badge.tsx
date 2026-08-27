import React from 'react';
import { View, Text, StyleSheet, ViewStyle } from 'react-native';
import { MissionStatus } from '@/types';
import { getMissionStatusColors, BorderRadius, Typography } from '@/constants/theme';
import { Strings } from '@/constants/strings';

interface BadgeProps {
  status: MissionStatus;
  style?: ViewStyle;
}

export const Badge: React.FC<BadgeProps> = ({ status, style }) => {
  const { text: textColor, bg: bgColor } = getMissionStatusColors(status);

  const getLabel = () => {
    switch (status) {
      case 'DRAFT': return Strings.statusDraft;
      case 'READY': return Strings.statusReady;
      case 'RUNNING': return Strings.statusRunning;
      case 'PAUSED': return Strings.statusPaused;
      case 'COMPLETED': return Strings.statusCompleted;
      case 'STOPPED': return Strings.statusStopped;
      case 'ERROR': return Strings.statusError;
      default: return status;
    }
  };

  return (
    <View style={[styles.container, { backgroundColor: bgColor }, style]}>
      <Text style={[styles.text, { color: textColor }]}>{getLabel()}</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    paddingVertical: 4,
    paddingHorizontal: 12,
    borderRadius: BorderRadius.pill,
    alignSelf: 'flex-start',
    alignItems: 'center',
    justifyContent: 'center',
  },
  text: {
    ...Typography.labelSmall,
    fontWeight: '700',
    textTransform: 'uppercase',
  },
});
