import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Field } from '@/types';
import { Card } from '@/components/ui/Card';
import { Colors, Typography, Spacing } from '@/constants/theme';
import { Strings } from '@/constants/strings';

interface FieldSummaryCardProps {
  field: Field | null;
  onChangeField: () => void;
}

export const FieldSummaryCard: React.FC<FieldSummaryCardProps> = ({
  field,
  onChangeField,
}) => {
  return (
    <Card style={styles.card} onPress={onChangeField}>
      <View style={styles.header}>
        <View style={styles.titleRow}>
          <View style={styles.iconBg}>
            <Text style={styles.icon}>🌾</Text>
          </View>
          <View>
            <Text style={styles.sectionLabel}>{Strings.dashboardFieldSection}</Text>
            <Text style={styles.fieldName}>
              {field ? field.name : Strings.dashboardNoField}
            </Text>
          </View>
        </View>
        <Text style={styles.actionLink}>
          {field ? Strings.dashboardChangeField : Strings.dashboardSelectField} →
        </Text>
      </View>

      {field && (
        <View style={styles.detailsRow}>
          <View style={styles.detailItem}>
            <Text style={styles.detailValue}>{Math.round(field.areaM2)} m²</Text>
            <Text style={styles.detailLabel}>{Strings.fieldAreaLabel}</Text>
          </View>
          <View style={styles.detailDivider} />
          <View style={styles.detailItem}>
            <Text style={styles.detailValue}>{field.boundary.length} titik</Text>
            <Text style={styles.detailLabel}>{Strings.fieldBoundaryLabel}</Text>
          </View>
          <View style={styles.detailDivider} />
          <View style={styles.detailItem}>
            <Text style={styles.detailValue}>{Math.round(field.perimeterM)} m</Text>
            <Text style={styles.detailLabel}>{Strings.fieldPerimeterLabel}</Text>
          </View>
        </View>
      )}
    </Card>
  );
};

const styles = StyleSheet.create({
  card: {
    marginBottom: Spacing.md,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  iconBg: {
    width: 44,
    height: 44,
    borderRadius: 10,
    backgroundColor: Colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
  },
  icon: {
    fontSize: 22,
  },
  sectionLabel: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
  },
  fieldName: {
    ...Typography.titleMedium,
    color: Colors.textPrimary,
    fontWeight: '700',
  },
  actionLink: {
    ...Typography.labelMedium,
    color: Colors.primary,
    fontWeight: '600',
  },
  detailsRow: {
    flexDirection: 'row',
    marginTop: Spacing.md,
    paddingTop: Spacing.md,
    borderTopWidth: 1,
    borderTopColor: Colors.border,
    justifyContent: 'space-around',
  },
  detailItem: {
    alignItems: 'center',
  },
  detailValue: {
    ...Typography.bodyMedium,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  detailLabel: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
    marginTop: 2,
  },
  detailDivider: {
    width: 1,
    height: 24,
    backgroundColor: Colors.border,
  },
});
