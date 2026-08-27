import React from 'react';
import { View, StyleSheet, Alert, Platform } from 'react-native';
import { MissionStatus } from '@/types';
import { Button } from '@/components/ui/Button';
import { Spacing } from '@/constants/theme';
import { Strings } from '@/constants/strings';

interface MissionControlsProps {
  status: MissionStatus;
  onStart: () => void;
  onPause: () => void;
  onResume: () => void;
  onStop: () => void;
  disabled?: boolean;
}

export const MissionControls: React.FC<MissionControlsProps> = ({
  status,
  onStart,
  onPause,
  onResume,
  onStop,
  disabled = false,
}) => {
  const handleStopWithConfirm = () => {
    if (Platform.OS === 'web') {
      const ok = window.confirm(
        `${Strings.missionStopConfirmTitle}\n${Strings.missionStopConfirmBody}`
      );
      if (ok) onStop();
    } else {
      Alert.alert(
        Strings.missionStopConfirmTitle,
        Strings.missionStopConfirmBody,
        [
          { text: Strings.missionStopConfirmCancel, style: 'cancel' },
          {
            text: Strings.missionStopConfirmOk,
            style: 'destructive',
            onPress: onStop,
          },
        ]
      );
    }
  };

  if (status === 'READY' || status === 'DRAFT') {
    return (
      <View style={styles.container}>
        <Button
          label={Strings.missionStart}
          onPress={onStart}
          disabled={disabled}
          size="lg"
          style={styles.fullButton}
        />
      </View>
    );
  }

  if (status === 'RUNNING') {
    return (
      <View style={styles.containerRow}>
        <Button
          label={Strings.missionPause}
          onPress={onPause}
          variant="secondary"
          size="lg"
          style={styles.halfButton}
        />
        <Button
          label={Strings.missionStop}
          onPress={handleStopWithConfirm}
          variant="danger"
          size="lg"
          style={styles.halfButton}
        />
      </View>
    );
  }

  if (status === 'PAUSED') {
    return (
      <View style={styles.containerRow}>
        <Button
          label={Strings.missionResume}
          onPress={onResume}
          variant="primary"
          size="lg"
          style={styles.halfButton}
        />
        <Button
          label={Strings.missionStop}
          onPress={handleStopWithConfirm}
          variant="danger"
          size="lg"
          style={styles.halfButton}
        />
      </View>
    );
  }

  return null;
};

const styles = StyleSheet.create({
  container: {
    width: '100%',
  },
  containerRow: {
    flexDirection: 'row',
    gap: Spacing.md,
    width: '100%',
  },
  fullButton: {
    width: '100%',
  },
  halfButton: {
    flex: 1,
  },
});
