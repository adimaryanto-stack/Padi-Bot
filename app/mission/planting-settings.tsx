import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  ScrollView,
  Pressable,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useFieldStore } from '@/stores/fieldStore';
import { useMissionStore } from '@/stores/missionStore';
import { useMachineStore } from '@/stores/machineStore';
import { generateCoverageRoute, calculateOptimalOrientation } from '@/services/routePlanner';
import { RouteResult, Field, RoutePattern } from '@/types';
import { Button } from '@/components/ui/Button';
import { PatternPreviewDiagram } from '@/components/mission/PatternPreviewDiagram';
import { Colors, Typography, Spacing } from '@/constants/theme';
import { Strings } from '@/constants/strings';

export default function PlantingSettingsScreen() {
  const router = useRouter();

  const fields = useFieldStore((state) => state.fields);
  const activeFieldId = useFieldStore((state) => state.activeFieldId);
  const setActiveField = useFieldStore((state) => state.setActiveField);

  // Selected field
  const [selectedFieldId, setSelectedFieldId] = useState<string>(
    activeFieldId || (fields[0]?.id ?? '')
  );

  const selectedField: Field | undefined =
    fields.find((f) => f.id === selectedFieldId) || fields[0];

  const defaultWidth = useMachineStore((state) => state.defaultMachineWidthM);
  const defaultHeadland = useMachineStore((state) => state.defaultHeadlandWidthM);
  const setPendingRoute = useMissionStore((state) => state.setPendingRoute);

  // Reference infographic parameters
  const [rowSpacingCm, setRowSpacingCm] = useState<number>(30); // Jarak antar baris
  const [plantSpacingCm, setPlantSpacingCm] = useState<number>(20); // Jarak antar tanaman
  const [machineWidthCm, setMachineWidthCm] = useState<number>(Math.round(defaultWidth * 100)); // Lebar kerja mesin
  const [speedMps, setSpeedMps] = useState<number>(0.8); // Kecepatan mesin
  const [headlandWidthM, setHeadlandWidthM] = useState<number>(defaultHeadland); // Area putar
  const [orientationDeg, setOrientationDeg] = useState<number>(0);
  const [routePattern, setRoutePattern] = useState<RoutePattern>('BOUSTROPHEDON');
  const [estimate, setEstimate] = useState<RouteResult | null>(null);

  const optimalAngle = selectedField ? calculateOptimalOrientation(selectedField.boundary) : 0;

  // Real-time calculation
  useEffect(() => {
    if (selectedField && selectedField.boundary.length >= 3) {
      const result = generateCoverageRoute({
        fieldBoundary: selectedField.boundary,
        machineWidthM: machineWidthCm / 100,
        headlandWidthM: headlandWidthM,
        orientationDeg,
        pattern: routePattern,
      });
      setEstimate(result);
    }
  }, [selectedField, machineWidthCm, headlandWidthM, orientationDeg, routePattern]);

  if (!fields || fields.length === 0) {
    return (
      <View style={styles.emptyContainer}>
        <Text style={styles.emptyText}>Belum ada sawah tersimpan</Text>
        <Button
          label="+ Buat Sawah Baru"
          onPress={() => router.push('/(tabs)/fields/create')}
          style={{ marginTop: 16 }}
        />
      </View>
    );
  }

  const handleSelectField = (fieldId: string) => {
    setSelectedFieldId(fieldId);
    setActiveField(fieldId);
  };

  const handleGenerateRoute = () => {
    if (!estimate || !selectedField) return;
    setActiveField(selectedField.id);
    setPendingRoute(estimate);
    router.push({
      pathname: '/mission/route-preview',
      params: {
        machineWidth: (machineWidthCm / 100).toString(),
        headlandWidth: headlandWidthM.toString(),
        orientationDeg: orientationDeg.toString(),
        pattern: routePattern,
        rowSpacing: rowSpacingCm.toString(),
        plantSpacing: plantSpacingCm.toString(),
        speed: speedMps.toString(),
      },
    });
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* 1. Field Selector */}
      <View style={styles.section}>
        <View style={styles.sectionHeaderRow}>
          <Text style={styles.sectionTitle}>🌾 {Strings.plantingFieldLabel}</Text>
          <Pressable onPress={() => router.push('/(tabs)/fields/create')}>
            <Text style={styles.addFieldLink}>+ Sawah Baru</Text>
          </Pressable>
        </View>

        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.fieldSelectorScroll}
        >
          {fields.map((f) => {
            const isSelected = f.id === (selectedField?.id ?? '');
            return (
              <Pressable
                key={f.id}
                onPress={() => handleSelectField(f.id)}
                style={[
                  styles.fieldCardItem,
                  isSelected && styles.fieldCardItemSelected,
                ]}
              >
                <View style={styles.fieldCardTop}>
                  <Text style={styles.fieldCardIcon}>🌾</Text>
                  {isSelected && <Text style={styles.selectedBadge}>✓ Aktif</Text>}
                </View>
                <Text style={[styles.fieldCardName, isSelected && styles.fieldCardNameSelected]}>
                  {f.name}
                </Text>
                <Text style={styles.fieldCardMetrics}>
                  {Math.round(f.areaM2)} m² • {f.boundary.length} titik
                </Text>
              </Pressable>
            );
          })}
        </ScrollView>
      </View>

      {/* 2. Route Pattern Selector */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>🛣️ Pola Jalur Tanam (Coverage Path Planning)</Text>
        <View style={styles.patternGrid}>
          {/* Pattern 1: Boustrophedon */}
          <Pressable
            onPress={() => setRoutePattern('BOUSTROPHEDON')}
            style={[
              styles.patternCard,
              routePattern === 'BOUSTROPHEDON' && styles.patternCardSelected,
            ]}
          >
            <Text style={styles.patternIcon}>⚡</Text>
            <View style={styles.patternTextCol}>
              <Text style={[styles.patternName, routePattern === 'BOUSTROPHEDON' && styles.patternNameSelected]}>
                1. Persegi Panjang / Zig-Zag (Boustrophedon)
              </Text>
              <Text style={styles.patternDesc}>Pola bolak-balik paralel standar paling efisien</Text>
            </View>
            {routePattern === 'BOUSTROPHEDON' && <Text style={styles.patternCheck}>✓</Text>}
          </Pressable>

          {/* Pattern 2: Headland + Inner */}
          <Pressable
            onPress={() => setRoutePattern('HEADLAND_INNER')}
            style={[
              styles.patternCard,
              routePattern === 'HEADLAND_INNER' && styles.patternCardSelected,
            ]}
          >
            <Text style={styles.patternIcon}>🔄</Text>
            <View style={styles.patternTextCol}>
              <Text style={[styles.patternName, routePattern === 'HEADLAND_INNER' && styles.patternNameSelected]}>
                2. Dengan Headland (Area Putar Keliling)
              </Text>
              <Text style={styles.patternDesc}>Tanam area tengah terlebih dahulu, sisakan putaran di ujung</Text>
            </View>
            {routePattern === 'HEADLAND_INNER' && <Text style={styles.patternCheck}>✓</Text>}
          </Pressable>

          {/* Pattern 3: Spiral Inward (Pinggir ke Tengah) */}
          <Pressable
            onPress={() => setRoutePattern('SPIRAL_INWARD')}
            style={[
              styles.patternCard,
              routePattern === 'SPIRAL_INWARD' && styles.patternCardSelected,
            ]}
          >
            <Text style={styles.patternIcon}>🌀</Text>
            <View style={styles.patternTextCol}>
              <Text style={[styles.patternName, routePattern === 'SPIRAL_INWARD' && styles.patternNameSelected]}>
                3. Oval / Spiral (Pinggir ke Tengah)
              </Text>
              <Text style={styles.patternDesc}>Memutar mengelilingi batas sawah dari luar menuju titik tengah</Text>
            </View>
            {routePattern === 'SPIRAL_INWARD' && <Text style={styles.patternCheck}>✓</Text>}
          </Pressable>

          {/* Pattern 4: Spiral Outward (Tengah ke Pinggir) */}
          <Pressable
            onPress={() => setRoutePattern('SPIRAL_OUTWARD')}
            style={[
              styles.patternCard,
              routePattern === 'SPIRAL_OUTWARD' && styles.patternCardSelected,
            ]}
          >
            <Text style={styles.patternIcon}>💫</Text>
            <View style={styles.patternTextCol}>
              <Text style={[styles.patternName, routePattern === 'SPIRAL_OUTWARD' && styles.patternNameSelected]}>
                4. Oval / Spiral (Tengah ke Pinggir)
              </Text>
              <Text style={styles.patternDesc}>Mulai dari pusat tengah sawah memutar membesar ke arah luar</Text>
            </View>
            {routePattern === 'SPIRAL_OUTWARD' && <Text style={styles.patternCheck}>✓</Text>}
          </Pressable>
        </View>

        {/* Dynamic Pattern Visual Diagram (Matching preview.webp) */}
        <View style={styles.diagramWrapper}>
          <Text style={styles.diagramHeader}>
            📷 Skema Alur Jalur ({
              routePattern === 'BOUSTROPHEDON'
                ? 'Persegi Panjang'
                : routePattern === 'HEADLAND_INNER'
                ? 'Dengan Headland'
                : routePattern === 'SPIRAL_INWARD'
                ? 'Oval/Spiral (Pinggir ke Tengah)'
                : 'Oval/Spiral (Tengah ke Pinggir)'
            }):
          </Text>
          <PatternPreviewDiagram pattern={routePattern} height={180} />
          <Text style={styles.diagramCaption}>
            {routePattern === 'BOUSTROPHEDON' &&
              '💡 Mesin masuk melalui titik Start (Jalur Masuk), menanam secara paralel zig-zag di area tengah, dan mengakhiri penanaman di titik End (Jalur Keluar).'}
            {routePattern === 'HEADLAND_INNER' &&
              '💡 Strategi 3 Tahap: 1. Tanam area tengah terlebih dahulu, 2. Sisakan area putar di ujung batas, 3. Selesaikan area keliling ujung terakhir.'}
            {routePattern === 'SPIRAL_INWARD' &&
              '💡 Mesin masuk dari sisi terluar batas sawah, berputar mengitari keliling sawah secara kontinyu menuju titik tengah sawah.'}
            {routePattern === 'SPIRAL_OUTWARD' &&
              '💡 Mesin mulai menanam dari titik pusat tengah sawah, berputar membesar ke arah perimeter luar hingga selesai di pinggir sawah.'}
          </Text>
        </View>
      </View>

      {/* 3. Rice Planting Parameters (From Infographic) */}
      <View style={styles.paramCard}>
        <Text style={styles.paramCardTitle}>⚙️ Parameter Mesin & Tanam</Text>

        {/* Row 1: Jarak Antar Baris & Jarak Antar Tanaman */}
        <View style={styles.rowTwoCols}>
          <View style={[styles.inputSubSection, { flex: 1 }]}>
            <Text style={styles.label}>Jarak Antar Baris</Text>
            <TextInput
              style={styles.input}
              value={rowSpacingCm.toString()}
              onChangeText={(t) => setRowSpacingCm(parseFloat(t) || 30)}
              keyboardType="numeric"
            />
            <Text style={styles.unitHint}>cm (standar: 30 cm)</Text>
          </View>

          <View style={[styles.inputSubSection, { flex: 1 }]}>
            <Text style={styles.label}>Jarak Antar Tanaman</Text>
            <TextInput
              style={styles.input}
              value={plantSpacingCm.toString()}
              onChangeText={(t) => setPlantSpacingCm(parseFloat(t) || 20)}
              keyboardType="numeric"
            />
            <Text style={styles.unitHint}>cm (standar: 20 cm)</Text>
          </View>
        </View>

        {/* Row 2: Lebar Mesin & Kecepatan */}
        <View style={styles.rowTwoCols}>
          <View style={[styles.inputSubSection, { flex: 1 }]}>
            <Text style={styles.label}>Lebar Mesin (Kerja Efektif)</Text>
            <TextInput
              style={styles.input}
              value={machineWidthCm.toString()}
              onChangeText={(t) => setMachineWidthCm(parseFloat(t) || 120)}
              keyboardType="numeric"
            />
            <Text style={styles.unitHint}>cm ({machineWidthCm / 100} m)</Text>
          </View>

          <View style={[styles.inputSubSection, { flex: 1 }]}>
            <Text style={styles.label}>Kecepatan Mesin</Text>
            <TextInput
              style={styles.input}
              value={speedMps.toString()}
              onChangeText={(t) => setSpeedMps(parseFloat(t) || 0.8)}
              keyboardType="numeric"
            />
            <Text style={styles.unitHint}>m/s (maks 1.5 m/s)</Text>
          </View>
        </View>

        {/* Row 3: Area Putar (Headland) */}
        <View style={styles.inputSubSection}>
          <Text style={styles.label}>Area Putar (Headland)</Text>
          <TextInput
            style={styles.input}
            value={headlandWidthM.toString()}
            onChangeText={(t) => setHeadlandWidthM(parseFloat(t) || 1.5)}
            keyboardType="numeric"
          />
          <Text style={styles.unitHint}>meter (area mesin berputar di ujung jalur)</Text>
        </View>
      </View>

      {/* 4. Orientation Selection */}
      <View style={styles.section}>
        <View style={styles.sectionHeaderRow}>
          <Text style={styles.label}>
            Orientasi Jalur: <Text style={{ color: Colors.primaryDark }}>{orientationDeg}°</Text>
          </Text>
          <Pressable onPress={() => setOrientationDeg(optimalAngle)} style={styles.optimalBtn}>
            <Text style={styles.optimalBtnText}>⚡ Otomatis ({optimalAngle}°)</Text>
          </Pressable>
        </View>

        <View style={styles.angleRow}>
          {[0, 45, 90, 135, 180].map((deg) => (
            <Pressable
              key={deg}
              onPress={() => setOrientationDeg(deg)}
              style={[
                styles.angleBtn,
                orientationDeg === deg && styles.angleBtnActive,
              ]}
            >
              <Text
                style={[
                  styles.angleBtnText,
                  orientationDeg === deg && styles.angleBtnTextActive,
                ]}
              >
                {deg}°
              </Text>
            </Pressable>
          ))}
        </View>
      </View>

      {/* 5. Live Estimation Card */}
      {estimate && (
        <View style={styles.estimateCard}>
          <Text style={styles.estimateTitle}>
            📊 Estimasi Hasil Jalur ({selectedField?.name})
          </Text>
          <View style={styles.estimateGrid}>
            <View style={styles.estimateItem}>
              <Text style={styles.estimateVal}>{estimate.totalLanes}</Text>
              <Text style={styles.estimateLbl}>{Strings.plantingLanes}</Text>
            </View>
            <View style={styles.estimateDivider} />
            <View style={styles.estimateItem}>
              <Text style={styles.estimateVal}>{estimate.totalDistanceM} m</Text>
              <Text style={styles.estimateLbl}>{Strings.plantingDistance}</Text>
            </View>
            <View style={styles.estimateDivider} />
            <View style={styles.estimateItem}>
              <Text style={styles.estimateVal}>{estimate.estimatedCoveragePct}%</Text>
              <Text style={styles.estimateLbl}>{Strings.plantingCoverage}</Text>
            </View>
          </View>
        </View>
      )}

      {/* 6. Big Generate Button */}
      <Button
        label="GENERATE JALUR"
        onPress={handleGenerateRoute}
        size="lg"
        style={styles.submitBtn}
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
  emptyContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: Spacing.xl,
  },
  emptyText: {
    ...Typography.titleLarge,
    color: Colors.textPrimary,
  },
  section: {
    marginBottom: Spacing.md,
  },
  sectionHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.sm,
  },
  sectionTitle: {
    ...Typography.titleSmall,
    color: Colors.textPrimary,
    fontWeight: '700',
  },
  addFieldLink: {
    ...Typography.labelSmall,
    color: Colors.primaryDark,
    fontWeight: '700',
  },
  fieldSelectorScroll: {
    gap: Spacing.sm,
    paddingVertical: 4,
  },
  fieldCardItem: {
    width: 170,
    backgroundColor: Colors.surface,
    padding: Spacing.md,
    borderRadius: 12,
    borderWidth: 1.5,
    borderColor: Colors.border,
  },
  fieldCardItemSelected: {
    borderColor: Colors.primary,
    backgroundColor: '#F9FCF5',
  },
  fieldCardTop: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 6,
  },
  fieldCardIcon: {
    fontSize: 22,
  },
  selectedBadge: {
    ...Typography.labelSmall,
    color: Colors.primaryDark,
    backgroundColor: Colors.primaryLight,
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 6,
    fontSize: 10,
    fontWeight: '700',
  },
  fieldCardName: {
    ...Typography.bodyLarge,
    fontWeight: '700',
    color: Colors.textPrimary,
    marginBottom: 2,
  },
  fieldCardNameSelected: {
    color: Colors.primaryDark,
  },
  fieldCardMetrics: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
    fontSize: 11,
  },
  patternGrid: {
    gap: 8,
    marginTop: 4,
  },
  patternCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    padding: Spacing.md,
    borderRadius: 12,
    borderWidth: 1.5,
    borderColor: Colors.border,
    gap: 12,
  },
  patternCardSelected: {
    borderColor: Colors.primary,
    backgroundColor: '#F9FCF5',
  },
  patternIcon: {
    fontSize: 22,
  },
  patternTextCol: {
    flex: 1,
  },
  patternName: {
    ...Typography.bodyLarge,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  patternNameSelected: {
    color: Colors.primaryDark,
  },
  patternDesc: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
    fontSize: 11,
    marginTop: 2,
  },
  patternCheck: {
    fontSize: 18,
    fontWeight: '800',
    color: Colors.primary,
  },
  diagramWrapper: {
    marginTop: Spacing.sm,
    backgroundColor: '#F9FCF5',
    padding: Spacing.md,
    borderRadius: 14,
    borderWidth: 1.5,
    borderColor: '#C5E1A5',
  },
  diagramHeader: {
    ...Typography.labelSmall,
    color: Colors.primaryDark,
    fontWeight: '700',
    marginBottom: 4,
  },
  diagramCaption: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
    fontSize: 11,
    marginTop: 6,
    lineHeight: 16,
  },
  paramCard: {
    backgroundColor: Colors.surface,
    padding: Spacing.md,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: Colors.border,
    marginBottom: Spacing.md,
  },
  paramCardTitle: {
    ...Typography.titleSmall,
    color: Colors.textPrimary,
    fontWeight: '700',
    marginBottom: Spacing.sm,
  },
  inputSubSection: {
    marginBottom: Spacing.sm,
  },
  rowTwoCols: {
    flexDirection: 'row',
    gap: Spacing.md,
  },
  label: {
    ...Typography.titleSmall,
    color: Colors.textPrimary,
    fontWeight: '600',
    marginBottom: 4,
    fontSize: 13,
  },
  unitHint: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
    marginTop: 2,
    fontSize: 11,
  },
  input: {
    height: 44,
    backgroundColor: Colors.surfaceVariant,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: Colors.border,
    paddingHorizontal: Spacing.md,
    fontSize: 15,
    color: Colors.textPrimary,
  },
  optimalBtn: {
    backgroundColor: Colors.primaryLight,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
  },
  optimalBtnText: {
    ...Typography.labelSmall,
    color: Colors.primaryDark,
    fontWeight: '700',
    fontSize: 11,
  },
  angleRow: {
    flexDirection: 'row',
    gap: 8,
    marginTop: 4,
  },
  angleBtn: {
    flex: 1,
    paddingVertical: 10,
    backgroundColor: Colors.surface,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: Colors.border,
    alignItems: 'center',
  },
  angleBtnActive: {
    backgroundColor: Colors.primary,
    borderColor: Colors.primaryDark,
  },
  angleBtnText: {
    ...Typography.labelMedium,
    color: Colors.textSecondary,
    fontWeight: '600',
  },
  angleBtnTextActive: {
    color: '#FFFFFF',
  },
  estimateCard: {
    backgroundColor: '#F9FCF5',
    borderWidth: 1.5,
    borderColor: Colors.primaryLight,
    borderRadius: 12,
    padding: Spacing.md,
    marginTop: Spacing.xs,
    marginBottom: Spacing.lg,
  },
  estimateTitle: {
    ...Typography.labelMedium,
    color: Colors.primaryDark,
    fontWeight: '700',
    marginBottom: Spacing.sm,
  },
  estimateGrid: {
    flexDirection: 'row',
    justifyContent: 'space-around',
  },
  estimateItem: {
    alignItems: 'center',
  },
  estimateVal: {
    ...Typography.titleLarge,
    color: Colors.textPrimary,
    fontWeight: '800',
  },
  estimateLbl: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
    marginTop: 2,
  },
  estimateDivider: {
    width: 1,
    height: 32,
    backgroundColor: Colors.border,
  },
  submitBtn: {
    marginTop: Spacing.xs,
  },
});
