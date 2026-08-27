import React from 'react';
import { View, Text, TextInput, StyleSheet, Pressable } from 'react-native';
import { GeoPoint } from '@/types';
import { Colors, Typography, Spacing } from '@/constants/theme';

interface CoordinateInputProps {
  index: number;
  point: GeoPoint;
  onUpdate: (point: GeoPoint) => void;
  onDelete: () => void;
  canDelete?: boolean;
}

export const CoordinateInput: React.FC<CoordinateInputProps> = ({
  index,
  point,
  onUpdate,
  onDelete,
  canDelete = true,
}) => {
  const handleLatChange = (text: string) => {
    const val = parseFloat(text);
    onUpdate({ ...point, lat: isNaN(val) ? 0 : val });
  };

  const handleLonChange = (text: string) => {
    const val = parseFloat(text);
    onUpdate({ ...point, lon: isNaN(val) ? 0 : val });
  };

  return (
    <View style={styles.container}>
      <View style={styles.badge}>
        <Text style={styles.badgeText}>{index + 1}</Text>
      </View>

      <View style={styles.inputsRow}>
        <View style={styles.inputGroup}>
          <Text style={styles.inputLabel}>Latitude</Text>
          <TextInput
            style={styles.input}
            defaultValue={point.lat !== 0 ? point.lat.toString() : ''}
            onChangeText={handleLatChange}
            placeholder="-6.9234"
            placeholderTextColor={Colors.textDisabled}
            keyboardType="numeric"
          />
        </View>

        <View style={styles.inputGroup}>
          <Text style={styles.inputLabel}>Longitude</Text>
          <TextInput
            style={styles.input}
            defaultValue={point.lon !== 0 ? point.lon.toString() : ''}
            onChangeText={handleLonChange}
            placeholder="107.6100"
            placeholderTextColor={Colors.textDisabled}
            keyboardType="numeric"
          />
        </View>
      </View>

      {canDelete && (
        <Pressable onPress={onDelete} style={styles.deleteButton}>
          <Text style={styles.deleteText}>✕</Text>
        </Pressable>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: Spacing.sm,
    backgroundColor: Colors.surface,
    padding: Spacing.sm,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: Colors.border,
    gap: 8,
  },
  badge: {
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: Colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badgeText: {
    fontSize: 12,
    fontWeight: '700',
    color: Colors.primaryDark,
  },
  inputsRow: {
    flex: 1,
    flexDirection: 'row',
    gap: 8,
  },
  inputGroup: {
    flex: 1,
  },
  inputLabel: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
    fontSize: 10,
    marginBottom: 2,
  },
  input: {
    height: 38,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 6,
    paddingHorizontal: 8,
    fontSize: 13,
    color: Colors.textPrimary,
    backgroundColor: Colors.surfaceVariant,
  },
  deleteButton: {
    width: 28,
    height: 28,
    alignItems: 'center',
    justifyContent: 'center',
  },
  deleteText: {
    fontSize: 16,
    color: Colors.error,
    fontWeight: '700',
  },
});
