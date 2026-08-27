import React from 'react';
import { View, Text, StyleSheet, ViewStyle } from 'react-native';
import { Colors, Typography } from '@/constants/theme';

interface StatusDotProps {
  status: 'connected' | 'connecting' | 'disconnected' | 'error';
  label?: string;
  style?: ViewStyle;
}

export const StatusDot: React.FC<StatusDotProps> = ({ status, label, style }) => {
  const getColor = () => {
    switch (status) {
      case 'connected': return Colors.success;
      case 'connecting': return Colors.warning;
      case 'error': return Colors.error;
      case 'disconnected':
      default:
        return Colors.textDisabled;
    }
  };

  return (
    <View style={[styles.container, style]}>
      <View style={[styles.dot, { backgroundColor: getColor() }]} />
      {label && <Text style={styles.label}>{label}</Text>}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  label: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
  },
});
