import { MissionStatus } from '@/types';

// ─────────────────────────────────────────────────────────────
// PadiBot Design Tokens — from design.md
// ─────────────────────────────────────────────────────────────

export const Colors = {
  // Primary — Hijau Sawah
  primary:         '#7CB342',
  primaryDark:     '#558B2F',
  primaryLight:    '#DCEDC8',
  primaryBg:       '#F1F8E9',

  // Semantic
  success:         '#4CAF50',
  warning:         '#FF9800',
  error:           '#F44336',
  info:            '#2196F3',

  // Background / Surface
  background:      '#FAFAFA',
  surface:         '#FFFFFF',
  surfaceVariant:  '#F5F5F5',

  // Text
  textPrimary:     '#212121',
  textSecondary:   '#757575',
  textDisabled:    '#BDBDBD',
  textOnPrimary:   '#FFFFFF',
  textOnDark:      '#FFFFFF',

  // Border
  border:          '#EEEEEE',
  borderStrong:    '#BDBDBD',

  // Mission status colors
  statusDraft:         '#9E9E9E',
  statusDraftBg:       '#E0E0E0',
  statusReady:         '#1565C0',
  statusReadyBg:       '#BBDEFB',
  statusRunning:       '#2E7D32',
  statusRunningBg:     '#C8E6C9',
  statusPaused:        '#E65100',
  statusPausedBg:      '#FFE0B2',
  statusCompleted:     '#33691E',
  statusCompletedBg:   '#DCEDC8',
  statusStopped:       '#C62828',
  statusStoppedBg:     '#FFCDD2',
  statusError:         '#6A1B9A',
  statusErrorBg:       '#E1BEE7',

  // Emergency — highest contrast
  emergency:       '#B71C1C',
  emergencyAccent: '#FF1744',
  emergencyText:   '#FFFFFF',
} as const;

/** Returns text + background colors for a given mission status */
export function getMissionStatusColors(status: MissionStatus) {
  const map: Record<MissionStatus, { text: string; bg: string }> = {
    DRAFT:     { text: Colors.statusDraft,     bg: Colors.statusDraftBg },
    READY:     { text: Colors.statusReady,     bg: Colors.statusReadyBg },
    RUNNING:   { text: Colors.statusRunning,   bg: Colors.statusRunningBg },
    PAUSED:    { text: Colors.statusPaused,    bg: Colors.statusPausedBg },
    COMPLETED: { text: Colors.statusCompleted, bg: Colors.statusCompletedBg },
    STOPPED:   { text: Colors.statusStopped,   bg: Colors.statusStoppedBg },
    ERROR:     { text: Colors.statusError,     bg: Colors.statusErrorBg },
  };
  return map[status];
}

export const Spacing = {
  xs:   4,
  sm:   8,
  md:   12,
  lg:   16,
  xl:   24,
  xxl:  32,
  xxxl: 48,
} as const;

export const BorderRadius = {
  sm:   8,
  md:   12,
  lg:   16,
  xl:   20,
  pill: 999,
} as const;

export const Typography = {
  headlineLarge:  { fontSize: 28, fontWeight: '700' as const, lineHeight: 36 },
  headlineMedium: { fontSize: 24, fontWeight: '600' as const, lineHeight: 32 },
  headlineSmall:  { fontSize: 20, fontWeight: '600' as const, lineHeight: 28 },
  titleLarge:     { fontSize: 18, fontWeight: '500' as const, lineHeight: 24 },
  titleMedium:    { fontSize: 16, fontWeight: '500' as const, lineHeight: 22 },
  titleSmall:     { fontSize: 14, fontWeight: '600' as const, lineHeight: 20 },
  bodyLarge:      { fontSize: 16, fontWeight: '400' as const, lineHeight: 24 },
  bodyMedium:     { fontSize: 14, fontWeight: '400' as const, lineHeight: 20 },
  labelLarge:     { fontSize: 14, fontWeight: '500' as const, lineHeight: 20 },
  labelMedium:    { fontSize: 12, fontWeight: '500' as const, lineHeight: 16 },
  labelSmall:     { fontSize: 11, fontWeight: '500' as const, lineHeight: 14 },
  emergency:      { fontSize: 20, fontWeight: '700' as const, lineHeight: 28 },
  telemetry:      { fontSize: 22, fontWeight: '700' as const, lineHeight: 28 },
} as const;

export const Shadow = {
  sm: {
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.06,
    shadowRadius: 2,
    elevation: 2,
  },
  md: {
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.10,
    shadowRadius: 6,
    elevation: 4,
  },
  emergency: {
    shadowColor: '#B71C1C',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.40,
    shadowRadius: 12,
    elevation: 8,
  },
} as const;
