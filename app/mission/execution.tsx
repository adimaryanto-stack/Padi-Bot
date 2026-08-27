import React, { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  useWindowDimensions,
  Alert,
  Platform,
  Pressable,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useMissionStore } from '@/stores/missionStore';
import { useFieldStore } from '@/stores/fieldStore';
import { useMachineStore } from '@/stores/machineStore';
import { createMachineConnection, MachineConnection } from '@/services/machineProtocol';
import { MissionStatus } from '@/types';
import { RouteCanvas } from '@/components/mission/RouteCanvas';
import { TelemetryStrip } from '@/components/mission/TelemetryStrip';
import { ProgressBar } from '@/components/ui/ProgressBar';
import { EmergencyStopButton } from '@/components/ui/EmergencyStopButton';
import { Button } from '@/components/ui/Button';
import { Colors, Typography, Spacing } from '@/constants/theme';
import { Strings } from '@/constants/strings';

export default function MissionExecutionScreen() {
  const router = useRouter();
  const { width } = useWindowDimensions();

  const activeMission = useMissionStore((state) => state.activeMission);
  const updateMissionStatus = useMissionStore((state) => state.updateMissionStatus);
  const liveTelemetry = useMissionStore((state) => state.liveTelemetry);
  const updateTelemetry = useMissionStore((state) => state.updateTelemetry);
  const addMissionEvent = useMissionStore((state) => state.addMissionEvent);

  const getActiveField = useFieldStore((state) => state.getActiveField);
  const activeField = getActiveField();

  const connectionConfig = useMachineStore((state) => state.connectionConfig);

  const [status, setStatus] = useState<MissionStatus>(activeMission?.status || 'READY');
  const [elapsedSec, setElapsedSec] = useState<number>(0);
  const [isCompletedModal, setIsCompletedModal] = useState<boolean>(false);

  const machineRef = useRef<MachineConnection | null>(null);

  // 1. Setup machine connection
  useEffect(() => {
    const conn = createMachineConnection(connectionConfig);
    machineRef.current = conn;

    conn.connect().then(() => {
      if (activeMission) {
        conn.uploadMission(activeMission);
      }
    });

    const unsubTelemetry = conn.onTelemetry((t) => {
      updateTelemetry(t);
      if (t.missionProgressPct >= 100 && status === 'RUNNING') {
        handleMissionCompleted();
      }
    });

    return () => {
      unsubTelemetry();
      conn.disconnect();
    };
  }, []);

  // 2. Timer loop
  useEffect(() => {
    let timer: NodeJS.Timeout | null = null;
    if (status === 'RUNNING') {
      timer = setInterval(() => {
        setElapsedSec((prev) => prev + 1);
      }, 1000);
    }
    return () => {
      if (timer) clearInterval(timer);
    };
  }, [status]);

  if (!activeMission || !activeField) {
    return (
      <View style={styles.emptyContainer}>
        <Text style={styles.emptyText}>Tidak ada misi aktif</Text>
        <Button label="Kembali ke Beranda" onPress={() => router.replace('/(tabs)')} style={{ marginTop: 16 }} />
      </View>
    );
  }

  const handleStart = async () => {
    if (!machineRef.current) return;
    try {
      await machineRef.current.startMission();
      setStatus('RUNNING');
      await updateMissionStatus(activeMission.id, 'RUNNING', { startedAt: Date.now() });
      await addMissionEvent('START', 'Misi dimulai oleh operator', 'INFO');
    } catch (e) {
      console.error('Start error:', e);
    }
  };

  const handlePause = async () => {
    if (!machineRef.current) return;
    try {
      await machineRef.current.pauseMission();
      setStatus('PAUSED');
      await updateMissionStatus(activeMission.id, 'PAUSED');
      await addMissionEvent('PAUSE', 'Misi dijeda oleh operator', 'WARNING');
    } catch (e) {
      console.error('Pause error:', e);
    }
  };

  const handleResume = async () => {
    if (!machineRef.current) return;
    try {
      await machineRef.current.resumeMission();
      setStatus('RUNNING');
      await updateMissionStatus(activeMission.id, 'RUNNING');
      await addMissionEvent('RESUME', 'Misi dilanjutkan kembali', 'INFO');
    } catch (e) {
      console.error('Resume error:', e);
    }
  };

  const handleStop = async () => {
    if (!machineRef.current) return;
    try {
      await machineRef.current.stopMission();
      setStatus('STOPPED');
      await updateMissionStatus(activeMission.id, 'STOPPED', { completedAt: Date.now() });
      await addMissionEvent('STOP', 'Misi dihentikan oleh operator', 'WARNING');
    } catch (e) {
      console.error('Stop error:', e);
    }
  };

  const handleEmergencyStop = async () => {
    if (!machineRef.current) return;
    try {
      await machineRef.current.emergencyStop();
      setStatus('STOPPED');
      await updateMissionStatus(activeMission.id, 'STOPPED', { completedAt: Date.now() });
      await addMissionEvent('STOP', '⛔ BERHENTI DARURAT DIAKTIFKAN', 'CRITICAL');
    } catch (e) {
      console.error('Emergency stop error:', e);
    }
  };

  const handleMissionCompleted = async () => {
    setStatus('COMPLETED');
    setIsCompletedModal(true);
    await updateMissionStatus(activeMission.id, 'COMPLETED', {
      completedAt: Date.now(),
      actualCoveragePct: activeMission.estimatedCoveragePct,
    });
    await addMissionEvent('COMPLETED', 'Misi tanam selesai secara sempurna', 'INFO');
  };

  const formatTimer = (sec: number) => {
    const mins = Math.floor(sec / 60);
    const secs = sec % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const canvasWidth = Math.min(width - 32, 440);
  const progressPct = liveTelemetry?.missionProgressPct || (status === 'COMPLETED' ? 100 : 0);
  const currentLane = liveTelemetry ? liveTelemetry.currentLaneIndex : 0;
  const machinePos = liveTelemetry
    ? { lat: liveTelemetry.positionLat, lon: liveTelemetry.positionLon }
    : undefined;

  // Real-time calculation of area planted and remaining area
  const totalFieldArea = Math.round(activeField.areaM2);
  const completedArea = Math.round((totalFieldArea * progressPct) / 100);
  const remainingArea = Math.max(0, totalFieldArea - completedArea);
  const speed = liveTelemetry?.speedMps ?? (status === 'RUNNING' ? 0.8 : 0);

  return (
    <View style={styles.container}>
      {/* Top Status Header */}
      <View style={styles.topHeader}>
        <View style={styles.statusBadgeRow}>
          <View style={[styles.runningDot, status === 'RUNNING' && styles.runningDotActive]} />
          <Text style={styles.statusBadgeText}>
            {status === 'RUNNING'
              ? 'AUTO RUNNING'
              : status === 'PAUSED'
              ? 'PAUSED'
              : status === 'COMPLETED'
              ? 'SELESAI'
              : 'READY'}
          </Text>
        </View>

        <Text style={styles.timerText}>⏱ {formatTimer(elapsedSec)}</Text>

        <Pressable
          onPress={() => router.replace('/(tabs)')}
          style={styles.exitBtn}
        >
          <Text style={styles.exitBtnText}>✕ Keluar</Text>
        </Pressable>
      </View>

      {/* Progress Metric Box */}
      <View style={styles.progressCard}>
        <View style={styles.progressHeaderRow}>
          <Text style={styles.progressLabel}>Progres Penanaman</Text>
          <Text style={styles.progressPctText}>{progressPct}%</Text>
        </View>
        <ProgressBar progress={progressPct} showPercent={false} height={10} color="#00E676" />

        {/* 3 Metrics (Area Selesai, Sisa Area, Kecepatan) */}
        <View style={styles.metricsRow}>
          <View style={styles.metricItem}>
            <Text style={styles.metricVal}>{completedArea.toLocaleString('id-ID')} m²</Text>
            <Text style={styles.metricLbl}>Area Selesai</Text>
          </View>
          <View style={styles.metricDivider} />
          <View style={styles.metricItem}>
            <Text style={styles.metricVal}>{remainingArea.toLocaleString('id-ID')} m²</Text>
            <Text style={styles.metricLbl}>Sisa Area</Text>
          </View>
          <View style={styles.metricDivider} />
          <View style={styles.metricItem}>
            <Text style={styles.metricVal}>{speed.toFixed(1)} m/s</Text>
            <Text style={styles.metricLbl}>Kecepatan</Text>
          </View>
        </View>
      </View>

      {/* Main Map View */}
      <View style={styles.mapContainer}>
        <RouteCanvas
          boundary={activeField.boundary}
          waypoints={activeMission.route}
          machinePosition={machinePos}
          width={canvasWidth}
          height={260}
          completedLaneIndex={currentLane}
          headlandWidthM={activeMission.headlandWidthM}
          showLegend={true}
        />
      </View>

      {/* Telemetry Strip (Battery, GPS, Lane) */}
      <View style={styles.telemetryWrapper}>
        <TelemetryStrip telemetry={liveTelemetry} />
      </View>

      {/* Control Buttons (Pause / Resume / Stop / Start) */}
      <View style={styles.controlsWrapper}>
        {status === 'READY' && (
          <Button
            label="▶  MULAI MENANAM OTOMATIS"
            onPress={handleStart}
            size="lg"
            style={styles.fullButton}
          />
        )}

        {status === 'RUNNING' && (
          <View style={styles.btnRow}>
            <Button
              label="⏸  PAUSE"
              onPress={handlePause}
              variant="secondary"
              size="lg"
              style={styles.halfBtn}
            />
            <Button
              label="⏹  STOP"
              onPress={handleStop}
              variant="danger"
              size="lg"
              style={styles.halfBtn}
            />
          </View>
        )}

        {status === 'PAUSED' && (
          <View style={styles.btnRow}>
            <Button
              label="▶  RESUME"
              onPress={handleResume}
              variant="primary"
              size="lg"
              style={styles.halfBtn}
            />
            <Button
              label="⏹  STOP"
              onPress={handleStop}
              variant="danger"
              size="lg"
              style={styles.halfBtn}
            />
          </View>
        )}

        {/* Emergency Stop Button */}
        <EmergencyStopButton
          onEmergencyStop={handleEmergencyStop}
          compact={true}
          style={styles.emergencyBtn}
        />
      </View>

      {/* Mission Complete Overlay */}
      {isCompletedModal && (
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalIcon}>🎉</Text>
            <Text style={styles.modalTitle}>{Strings.missionCompleteTitle}</Text>
            <Text style={styles.modalDesc}>
              Seluruh sawah ({activeField.name}) telah selesai ditanam!
            </Text>
            <Text style={styles.modalSubDesc}>
              Total Area: {totalFieldArea} m² • Durasi: {formatTimer(elapsedSec)}
            </Text>
            <Button
              label="Lihat Riwayat & Laporan"
              onPress={() => router.replace('/(tabs)/history')}
              size="lg"
              style={{ width: '100%', marginTop: 16 }}
            />
          </View>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
    paddingTop: 36,
    paddingHorizontal: Spacing.md,
    justifyContent: 'space-between',
    paddingBottom: Spacing.md,
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
  topHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.xs,
  },
  statusBadgeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#1E2D1E',
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 8,
    gap: 6,
  },
  runningDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#FFD54F',
  },
  runningDotActive: {
    backgroundColor: '#00E676',
  },
  statusBadgeText: {
    color: '#E8F5E9',
    fontSize: 11,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  timerText: {
    ...Typography.titleMedium,
    color: Colors.textPrimary,
    fontWeight: '800',
  },
  exitBtn: {
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  exitBtnText: {
    color: Colors.textSecondary,
    fontSize: 12,
    fontWeight: '600',
  },
  progressCard: {
    backgroundColor: Colors.surface,
    padding: Spacing.md,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: Colors.border,
    marginVertical: 4,
  },
  progressHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 6,
  },
  progressLabel: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
    fontWeight: '600',
  },
  progressPctText: {
    ...Typography.bodyLarge,
    fontWeight: '800',
    color: Colors.primaryDark,
  },
  metricsRow: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    marginTop: Spacing.sm,
    paddingTop: Spacing.xs,
    borderTopWidth: 1,
    borderTopColor: Colors.border,
  },
  metricItem: {
    alignItems: 'center',
  },
  metricVal: {
    ...Typography.bodyLarge,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  metricLbl: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
    fontSize: 10,
    marginTop: 2,
  },
  metricDivider: {
    width: 1,
    height: 24,
    backgroundColor: Colors.border,
  },
  mapContainer: {
    alignItems: 'center',
    marginVertical: 4,
  },
  telemetryWrapper: {
    marginVertical: 2,
  },
  controlsWrapper: {
    gap: 8,
    marginTop: 2,
  },
  fullButton: {
    width: '100%',
  },
  btnRow: {
    flexDirection: 'row',
    gap: Spacing.sm,
  },
  halfBtn: {
    flex: 1,
  },
  emergencyBtn: {
    marginTop: 2,
  },
  modalOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    alignItems: 'center',
    justifyContent: 'center',
    padding: Spacing.xl,
    zIndex: 100,
  },
  modalContent: {
    backgroundColor: Colors.surface,
    borderRadius: 20,
    padding: Spacing.xl,
    alignItems: 'center',
    width: '100%',
    maxWidth: 360,
  },
  modalIcon: {
    fontSize: 56,
    marginBottom: Spacing.sm,
  },
  modalTitle: {
    ...Typography.headlineMedium,
    fontWeight: '800',
    color: Colors.textPrimary,
    textAlign: 'center',
  },
  modalDesc: {
    ...Typography.bodyMedium,
    color: Colors.textPrimary,
    fontWeight: '600',
    textAlign: 'center',
    marginTop: Spacing.xs,
  },
  modalSubDesc: {
    ...Typography.bodyMedium,
    color: Colors.textSecondary,
    textAlign: 'center',
    marginTop: 4,
  },
});
