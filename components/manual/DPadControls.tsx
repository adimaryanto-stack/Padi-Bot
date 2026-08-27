import React, { useState, useRef, useEffect } from 'react';
import { View, Text, Pressable, StyleSheet, Platform } from 'react-native';
import { ManualCommand } from '@/types';
import { Colors, Typography, Spacing, Shadow } from '@/constants/theme';
import { Strings } from '@/constants/strings';
import * as Haptics from 'expo-haptics';

interface DPadControlsProps {
  onCommand: (cmd: ManualCommand) => void;
  disabled?: boolean;
}

export const DPadControls: React.FC<DPadControlsProps> = ({
  onCommand,
  disabled = false,
}) => {
  const [speedPct, setSpeedPct] = useState<number>(50);
  const activeInterval = useRef<NodeJS.Timeout | null>(null);

  const startHold = (direction: ManualCommand['direction']) => {
    if (disabled) return;
    if (Platform.OS !== 'web') {
      try {
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
      } catch {}
    }
    // Send immediate command
    onCommand({ direction, intensity: speedPct });

    // Loop command while holding
    if (activeInterval.current) clearInterval(activeInterval.current);
    activeInterval.current = setInterval(() => {
      onCommand({ direction, intensity: speedPct });
    }, 200);
  };

  const stopHold = () => {
    if (activeInterval.current) {
      clearInterval(activeInterval.current);
      activeInterval.current = null;
    }
    onCommand({ direction: 'STOP', intensity: 0 });
  };

  useEffect(() => {
    return () => {
      if (activeInterval.current) clearInterval(activeInterval.current);
    };
  }, []);

  return (
    <View style={styles.container}>
      {/* D-Pad Buttons */}
      <View style={styles.dpadGrid}>
        {/* Row 1: Forward */}
        <View style={styles.row}>
          <Pressable
            onPressIn={() => startHold('FORWARD')}
            onPressOut={stopHold}
            disabled={disabled}
            style={({ pressed }) => [styles.btn, pressed && styles.btnPressed]}
          >
            <Text style={styles.btnIcon}>▲</Text>
            <Text style={styles.btnLabel}>{Strings.manualForward}</Text>
          </Pressable>
        </View>

        {/* Row 2: Left, Center, Right */}
        <View style={styles.middleRow}>
          <Pressable
            onPressIn={() => startHold('LEFT')}
            onPressOut={stopHold}
            disabled={disabled}
            style={({ pressed }) => [styles.btn, pressed && styles.btnPressed]}
          >
            <Text style={styles.btnIcon}>◀</Text>
            <Text style={styles.btnLabel}>{Strings.manualLeft}</Text>
          </Pressable>

          <View style={styles.centerDot}>
            <Text style={styles.centerIcon}>🤖</Text>
          </View>

          <Pressable
            onPressIn={() => startHold('RIGHT')}
            onPressOut={stopHold}
            disabled={disabled}
            style={({ pressed }) => [styles.btn, pressed && styles.btnPressed]}
          >
            <Text style={styles.btnIcon}>▶</Text>
            <Text style={styles.btnLabel}>{Strings.manualRight}</Text>
          </Pressable>
        </View>

        {/* Row 3: Backward */}
        <View style={styles.row}>
          <Pressable
            onPressIn={() => startHold('BACKWARD')}
            onPressOut={stopHold}
            disabled={disabled}
            style={({ pressed }) => [styles.btn, pressed && styles.btnPressed]}
          >
            <Text style={styles.btnIcon}>▼</Text>
            <Text style={styles.btnLabel}>{Strings.manualBackward}</Text>
          </Pressable>
        </View>
      </View>

      {/* Speed Presets */}
      <View style={styles.speedSection}>
        <Text style={styles.speedTitle}>
          {Strings.manualSpeedLabel}: {speedPct}% ({((speedPct / 100) * 1.0).toFixed(2)} m/s)
        </Text>
        <View style={styles.presetsRow}>
          <Pressable
            onPress={() => setSpeedPct(30)}
            style={[styles.presetBtn, speedPct === 30 && styles.presetActive]}
          >
            <Text style={[styles.presetText, speedPct === 30 && styles.presetTextActive]}>
              {Strings.manualSpeedSlow} (30%)
            </Text>
          </Pressable>
          <Pressable
            onPress={() => setSpeedPct(60)}
            style={[styles.presetBtn, speedPct === 60 && styles.presetActive]}
          >
            <Text style={[styles.presetText, speedPct === 60 && styles.presetTextActive]}>
              {Strings.manualSpeedMedium} (60%)
            </Text>
          </Pressable>
          <Pressable
            onPress={() => setSpeedPct(100)}
            style={[styles.presetBtn, speedPct === 100 && styles.presetActive]}
          >
            <Text style={[styles.presetText, speedPct === 100 && styles.presetTextActive]}>
              {Strings.manualSpeedFast} (100%)
            </Text>
          </Pressable>
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    paddingVertical: Spacing.md,
  },
  dpadGrid: {
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'center',
  },
  middleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  btn: {
    width: 80,
    height: 80,
    borderRadius: 16,
    backgroundColor: Colors.surface,
    borderWidth: 2,
    borderColor: Colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    ...Shadow.md,
  },
  btnPressed: {
    backgroundColor: Colors.primaryLight,
    transform: [{ scale: 0.95 }],
  },
  btnIcon: {
    fontSize: 24,
    color: Colors.primaryDark,
  },
  btnLabel: {
    ...Typography.labelSmall,
    color: Colors.primaryDark,
    fontWeight: '700',
    marginTop: 2,
  },
  centerDot: {
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: Colors.surfaceVariant,
    alignItems: 'center',
    justifyContent: 'center',
  },
  centerIcon: {
    fontSize: 24,
  },
  speedSection: {
    width: '100%',
    marginTop: Spacing.xl,
    paddingHorizontal: Spacing.md,
  },
  speedTitle: {
    ...Typography.bodyMedium,
    color: Colors.textPrimary,
    fontWeight: '700',
    textAlign: 'center',
    marginBottom: Spacing.sm,
  },
  presetsRow: {
    flexDirection: 'row',
    gap: Spacing.sm,
  },
  presetBtn: {
    flex: 1,
    paddingVertical: 10,
    borderRadius: 8,
    backgroundColor: Colors.surfaceVariant,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: Colors.border,
  },
  presetActive: {
    backgroundColor: Colors.primary,
    borderColor: Colors.primaryDark,
  },
  presetText: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
    fontWeight: '600',
  },
  presetTextActive: {
    color: '#FFFFFF',
  },
});
