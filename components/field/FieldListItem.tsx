import React from 'react';
import { View, Text, StyleSheet, Pressable } from 'react-native';
import { Field } from '@/types';
import { Colors, Typography, Spacing } from '@/constants/theme';

interface FieldListItemProps {
  field: Field;
  onPress: () => void;
  onDelete?: () => void;
  isActive?: boolean;
}

export const FieldListItem: React.FC<FieldListItemProps> = ({
  field,
  onPress,
  onDelete,
  isActive = false,
}) => {
  const formattedDate = new Date(field.updatedAt).toLocaleDateString('id-ID', {
    day: 'numeric',
    month: 'short',
  });

  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [
        styles.container,
        isActive && styles.activeContainer,
        pressed && styles.pressed,
      ]}
    >
      <View style={styles.left}>
        <View style={[styles.iconBg, isActive && styles.activeIconBg]}>
          <Text style={styles.icon}>🌾</Text>
        </View>
        <View style={styles.textContainer}>
          <View style={styles.titleRow}>
            <Text style={styles.name}>{field.name}</Text>
            {isActive && <Text style={styles.activeBadge}>Aktif</Text>}
          </View>
          <Text style={styles.details}>
            {Math.round(field.areaM2)} m² • {field.boundary.length} titik
          </Text>
          <Text style={styles.date}>Diubah {formattedDate}</Text>
        </View>
      </View>

      <View style={styles.right}>
        {onDelete && (
          <Pressable
            onPress={(e) => {
              e.stopPropagation();
              onDelete();
            }}
            style={styles.deleteButton}
          >
            <Text style={styles.deleteIcon}>🗑️</Text>
          </Pressable>
        )}
        <Text style={styles.chevron}>›</Text>
      </View>
    </Pressable>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: Spacing.md,
    backgroundColor: Colors.surface,
    borderRadius: 12,
    marginBottom: Spacing.sm,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  activeContainer: {
    borderColor: Colors.primary,
    backgroundColor: '#F9FCF5',
  },
  pressed: {
    opacity: 0.9,
  },
  left: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
    gap: 12,
  },
  iconBg: {
    width: 44,
    height: 44,
    borderRadius: 10,
    backgroundColor: Colors.surfaceVariant,
    alignItems: 'center',
    justifyContent: 'center',
  },
  activeIconBg: {
    backgroundColor: Colors.primaryLight,
  },
  icon: {
    fontSize: 22,
  },
  textContainer: {
    flex: 1,
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  name: {
    ...Typography.titleMedium,
    color: Colors.textPrimary,
    fontWeight: '700',
  },
  activeBadge: {
    ...Typography.labelSmall,
    color: Colors.primaryDark,
    backgroundColor: Colors.primaryLight,
    paddingHorizontal: 6,
    paddingVertical: 1,
    borderRadius: 4,
    fontSize: 10,
    fontWeight: '700',
  },
  details: {
    ...Typography.bodyMedium,
    color: Colors.textSecondary,
    marginTop: 2,
  },
  date: {
    ...Typography.labelSmall,
    color: Colors.textDisabled,
    marginTop: 2,
  },
  right: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  deleteButton: {
    padding: 6,
  },
  deleteIcon: {
    fontSize: 16,
  },
  chevron: {
    fontSize: 24,
    color: Colors.textDisabled,
    fontWeight: '300',
  },
});
