import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  ScrollView,
  useWindowDimensions,
  Alert,
  Platform,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useFieldStore } from '@/stores/fieldStore';
import { GeoPoint } from '@/types';
import { validateField } from '@/services/routePlanner';
import { FieldBoundaryCanvas } from '@/components/field/FieldBoundaryCanvas';
import { CoordinateInput } from '@/components/field/CoordinateInput';
import { Button } from '@/components/ui/Button';
import { Colors, Typography, Spacing } from '@/constants/theme';
import { Strings } from '@/constants/strings';

export default function CreateFieldScreen() {
  const router = useRouter();
  const { width } = useWindowDimensions();
  const addField = useFieldStore((state) => state.addField);

  const [name, setName] = useState('');
  const [boundary, setBoundary] = useState<GeoPoint[]>([
    { lat: -6.9234, lon: 107.6100 },
    { lat: -6.9240, lon: 107.6108 },
    { lat: -6.9228, lon: 107.6115 },
    { lat: -6.9222, lon: 107.6105 },
  ]);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  const validation = validateField(boundary);

  const handleUpdatePoint = (index: number, newPoint: GeoPoint) => {
    const updated = [...boundary];
    updated[index] = newPoint;
    setBoundary(updated);
  };

  const handleAddPoint = () => {
    const last = boundary[boundary.length - 1] || { lat: -6.9234, lon: 107.6100 };
    setBoundary([...boundary, { lat: last.lat + 0.0005, lon: last.lon + 0.0005 }]);
  };

  const handleDeletePoint = (index: number) => {
    if (boundary.length <= 3) {
      setErrorMsg(Strings.errorFieldMinPoints);
      return;
    }
    setBoundary(boundary.filter((_, i) => i !== index));
  };

  const handleSave = async () => {
    if (!name.trim()) {
      setErrorMsg(Strings.errorFieldName);
      return;
    }

    if (!validation.valid) {
      setErrorMsg(validation.error || 'Batas sawah tidak valid');
      return;
    }

    setErrorMsg(null);
    setIsSaving(true);

    try {
      await addField({
        name: name.trim(),
        boundary,
        areaM2: Math.round(validation.areaM2),
        perimeterM: Math.round(validation.perimeterM),
      });

      if (Platform.OS === 'web') {
        alert(Strings.successFieldSaved);
      }
      router.back();
    } catch (e) {
      console.error('Save field error:', e);
      setErrorMsg(Strings.errorSave);
    } finally {
      setIsSaving(false);
    }
  };

  const canvasWidth = Math.min(width - 32, 440);

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Field Name Input */}
      <View style={styles.section}>
        <Text style={styles.label}>{Strings.fieldNameLabel} *</Text>
        <TextInput
          style={styles.input}
          placeholder={Strings.fieldNamePlaceholder}
          placeholderTextColor={Colors.textDisabled}
          value={name}
          onChangeText={(text) => {
            setName(text);
            if (errorMsg) setErrorMsg(null);
          }}
        />
      </View>

      {/* Boundary Polygon Preview Canvas */}
      <View style={styles.section}>
        <Text style={styles.label}>{Strings.fieldBoundaryLabel} *</Text>
        <Text style={styles.hint}>{Strings.fieldBoundaryHint}</Text>

        <FieldBoundaryCanvas
          boundary={boundary}
          width={canvasWidth}
          height={200}
          showPoints={true}
        />

        {/* Real-time Calculation Banner */}
        <View style={styles.statsCard}>
          <View style={styles.statItem}>
            <Text style={styles.statLabel}>{Strings.fieldAreaLabel}</Text>
            <Text style={styles.statValue}>
              {Math.round(validation.areaM2)} m²
            </Text>
          </View>
          <View style={styles.statDivider} />
          <View style={styles.statItem}>
            <Text style={styles.statLabel}>{Strings.fieldPerimeterLabel}</Text>
            <Text style={styles.statValue}>
              {Math.round(validation.perimeterM)} m
            </Text>
          </View>
        </View>
      </View>

      {/* Coordinate Input List */}
      <View style={styles.section}>
        <View style={styles.pointsHeader}>
          <Text style={styles.label}>Daftar Titik Koordinat</Text>
          <Button
            label={Strings.fieldAddPoint}
            onPress={handleAddPoint}
            variant="ghost"
            size="sm"
          />
        </View>

        {boundary.map((p, idx) => (
          <CoordinateInput
            key={`coord-${idx}`}
            index={idx}
            point={p}
            onUpdate={(pt) => handleUpdatePoint(idx, pt)}
            onDelete={() => handleDeletePoint(idx)}
            canDelete={boundary.length > 3}
          />
        ))}
      </View>

      {/* Error Message */}
      {errorMsg && <Text style={styles.errorText}>⚠️ {errorMsg}</Text>}

      {/* Submit Button */}
      <Button
        label={Strings.fieldSave}
        onPress={handleSave}
        loading={isSaving}
        size="lg"
        style={styles.saveButton}
      />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  content: {
    padding: Spacing.lg,
    paddingBottom: Spacing.xxxl,
  },
  section: {
    marginBottom: Spacing.lg,
  },
  label: {
    ...Typography.titleSmall,
    color: Colors.textPrimary,
    fontWeight: '700',
    marginBottom: 4,
  },
  hint: {
    ...Typography.bodyMedium,
    color: Colors.textSecondary,
    marginBottom: Spacing.sm,
  },
  input: {
    height: 48,
    backgroundColor: Colors.surface,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: Colors.border,
    paddingHorizontal: Spacing.md,
    fontSize: 16,
    color: Colors.textPrimary,
  },
  statsCard: {
    flexDirection: 'row',
    backgroundColor: Colors.surface,
    borderRadius: 8,
    padding: Spacing.md,
    marginTop: Spacing.sm,
    borderWidth: 1,
    borderColor: Colors.border,
    justifyContent: 'space-around',
  },
  statItem: {
    alignItems: 'center',
  },
  statLabel: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
  },
  statValue: {
    ...Typography.titleMedium,
    color: Colors.textPrimary,
    fontWeight: '700',
    marginTop: 2,
  },
  statDivider: {
    width: 1,
    height: 24,
    backgroundColor: Colors.border,
  },
  pointsHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.xs,
  },
  errorText: {
    ...Typography.bodyMedium,
    color: Colors.error,
    fontWeight: '600',
    marginBottom: Spacing.md,
    textAlign: 'center',
  },
  saveButton: {
    marginTop: Spacing.md,
  },
});
