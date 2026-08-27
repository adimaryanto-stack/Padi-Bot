import React from 'react';
import {
  Pressable,
  Text,
  StyleSheet,
  Alert,
  Platform,
  ViewStyle,
} from 'react-native';
import { Colors, BorderRadius, Typography, Shadow } from '@/constants/theme';
import { Strings } from '@/constants/strings';
import * as Haptics from 'expo-haptics';

interface EmergencyStopButtonProps {
  onEmergencyStop: () => void;
  style?: ViewStyle;
  compact?: boolean;
}

export const EmergencyStopButton: React.FC<EmergencyStopButtonProps> = ({
  onEmergencyStop,
  style,
  compact = false,
}) => {
  const triggerStop = () => {
    if (Platform.OS !== 'web') {
      try {
        Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error);
      } catch {}
    }
    onEmergencyStop();
  };

  const handlePress = () => {
    if (Platform.OS === 'web') {
      const ok = window.confirm(`${Strings.missionEmergencyConfirmTitle}\n${Strings.missionEmergencyConfirmBody}`);
      if (ok) triggerStop();
    } else {
      Alert.alert(
        Strings.missionEmergencyConfirmTitle,
        Strings.missionEmergencyConfirmBody,
        [
          { text: Strings.missionEmergencyConfirmCancel, style: 'cancel' },
          {
            text: Strings.missionEmergencyConfirmOk,
            style: 'destructive',
            onPress: triggerStop,
          },
        ],
        { cancelable: true }
      );
    }
  };

  return (
    <Pressable
      onPress={handlePress}
      onLongPress={triggerStop} // Hold 2s for direct emergency stop without prompt
      delayLongPress={1500}
      accessibilityRole="button"
      accessibilityLabel={Strings.missionEmergencyStop}
      style={({ pressed }) => [
        styles.container,
        compact ? styles.compactContainer : styles.fullContainer,
        pressed && styles.pressed,
        style,
      ]}
    >
      <Text style={[styles.text, compact && styles.compactText]}>
        {Strings.missionEmergencyStop}
      </Text>
    </Pressable>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: Colors.emergency,
    borderWidth: 3,
    borderColor: Colors.emergencyAccent,
    borderRadius: BorderRadius.lg,
    alignItems: 'center',
    justifyContent: 'center',
    ...Shadow.emergency,
  },
  fullContainer: {
    height: 80,
    width: '100%',
    paddingHorizontal: 16,
  },
  compactContainer: {
    height: 48,
    paddingHorizontal: 16,
  },
  text: {
    ...Typography.emergency,
    color: Colors.emergencyText,
    textAlign: 'center',
    letterSpacing: 1,
  },
  compactText: {
    fontSize: 14,
    fontWeight: '700',
  },
  pressed: {
    opacity: 0.9,
    transform: [{ scale: 0.98 }],
  },
});
