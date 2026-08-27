import React from 'react';
import { View, Text, StyleSheet, Platform } from 'react-native';
import { RoutePattern } from '@/types';

interface PatternPreviewDiagramProps {
  pattern: RoutePattern;
  width?: number;
  height?: number;
}

export const PatternPreviewDiagram: React.FC<PatternPreviewDiagramProps> = ({
  pattern,
  width = 340,
  height = 190,
}) => {
  return (
    <View style={[styles.container, { width: '100%', maxWidth: width, height }]}>
      {/* SVG Canvas */}
      {Platform.OS === 'web' ? (
        // @ts-ignore
        <svg width="100%" height="100%" viewBox={`0 0 ${width} ${height}`} style={{ position: 'absolute', top: 0, left: 0 }}>
          <defs>
            <pattern id="diagGrid" width="16" height="16" patternUnits="userSpaceOnUse">
              <path d="M 16 0 L 0 0 0 16" fill="none" stroke="rgba(255,255,255,0.06)" strokeWidth="1" />
            </pattern>
            {/* Arrowhead marker */}
            <marker id="whiteArrow" viewBox="0 0 8 8" refX="4" refY="4" markerWidth="5" markerHeight="5" orient="auto-start-reverse">
              <path d="M 0 1.5 L 6 4 L 0 6.5 z" fill="#FFFFFF" />
            </marker>
          </defs>

          {/* Dark agricultural field background */}
          <rect width={width} height={height} rx="12" fill="#1B2B1B" />
          <rect width={width} height={height} rx="12" fill="url(#diagGrid)" />

          {/* ── 1. PATTERN 1: PERSEGI PANJANG (BOUSTROPHEDON) ── */}
          {pattern === 'BOUSTROPHEDON' && (
            <g>
              {/* Outer boundary */}
              <rect x="24" y="20" width={width - 48} height={height - 52} rx="6" fill="rgba(46, 125, 50, 0.25)" stroke="#66BB6A" strokeWidth="2" />
              
              {/* Headland Zone (Yellow dashed) */}
              <rect x="36" y="30" width={width - 72} height={height - 72} rx="4" fill="none" stroke="#FBC02D" strokeWidth="1.5" strokeDasharray="4 2" />

              {/* Planting Lanes with U-turns */}
              <path d={`M 44 42 L ${width - 44} 42`} stroke="#00E676" strokeWidth="2.5" strokeLinecap="round" />
              <path d={`M ${width - 44} 42 Q ${width - 32} 55 ${width - 44} 68`} fill="none" stroke="#00E676" strokeWidth="1.8" strokeDasharray="3 2" />
              <path d={`M ${width - 44} 68 L 44 68`} stroke="#00E676" strokeWidth="2.5" strokeLinecap="round" />
              <path d={`M 44 68 Q 32 81 44 94`} fill="none" stroke="#00E676" strokeWidth="1.8" strokeDasharray="3 2" />
              <path d={`M 44 94 L ${width - 44} 94`} stroke="#00E676" strokeWidth="2.5" strokeLinecap="round" />
              <path d={`M ${width - 44} 94 Q ${width - 32} 107 ${width - 44} 120`} fill="none" stroke="#00E676" strokeWidth="1.8" strokeDasharray="3 2" />
              <path d={`M ${width - 44} 120 L 44 120`} stroke="#00E676" strokeWidth="2.5" strokeLinecap="round" />

              {/* White Motion Arrows */}
              <path d={`M ${width / 2 - 10} 42 L ${width / 2 + 10} 42`} stroke="#FFFFFF" strokeWidth="1.5" markerEnd="url(#whiteArrow)" />
              <path d={`M ${width / 2 + 10} 68 L ${width / 2 - 10} 68`} stroke="#FFFFFF" strokeWidth="1.5" markerEnd="url(#whiteArrow)" />
              <path d={`M ${width / 2 - 10} 94 L ${width / 2 + 10} 94`} stroke="#FFFFFF" strokeWidth="1.5" markerEnd="url(#whiteArrow)" />
              <path d={`M ${width / 2 + 10} 120 L ${width / 2 - 10} 120`} stroke="#FFFFFF" strokeWidth="1.5" markerEnd="url(#whiteArrow)" />

              {/* Start Badge */}
              <circle cx="44" cy="42" r="6" fill="#00E676" stroke="#FFFFFF" strokeWidth="2" />
              <rect x="28" y="22" width="56" height="15" rx="3" fill="#00E676" />
              <text x="56" y="32.5" fontSize="8" fontWeight="bold" fill="#1B2E1B" textAnchor="middle">START (Masuk)</text>

              {/* End Badge */}
              <circle cx="44" cy="120" r="6" fill="#FF1744" stroke="#FFFFFF" strokeWidth="2" />
              <rect x="28" y="126" width="56" height="15" rx="3" fill="#FF1744" />
              <text x="56" y="136.5" fontSize="8" fontWeight="bold" fill="#FFFFFF" textAnchor="middle">END (Keluar)</text>

              <text x={width - 62} y="73" fontSize="14">🚜</text>
            </g>
          )}

          {/* ── 2. PATTERN 2: DENGAN HEADLAND ── */}
          {pattern === 'HEADLAND_INNER' && (
            <g>
              <rect x="24" y="20" width={width - 48} height={height - 52} rx="6" fill="rgba(46, 125, 50, 0.25)" stroke="#66BB6A" strokeWidth="2" />
              <rect x="34" y="28" width={width - 68} height={height - 68} rx="5" fill="none" stroke="#FBC02D" strokeWidth="2" strokeDasharray="5 3" />
              <rect x="46" y="38" width={width - 92} height={height - 88} rx="3" fill="rgba(0,230,118,0.08)" stroke="rgba(255,255,255,0.2)" strokeWidth="1" />

              <path d={`M 54 50 L ${width - 54} 50`} stroke="#00E676" strokeWidth="2.5" strokeLinecap="round" />
              <path d={`M ${width - 54} 50 Q ${width - 42} 62 ${width - 54} 74`} fill="none" stroke="#00E676" strokeWidth="1.8" strokeDasharray="3 2" />
              <path d={`M ${width - 54} 74 L 54 74`} stroke="#00E676" strokeWidth="2.5" strokeLinecap="round" />
              <path d={`M 54 74 Q 42 86 54 98`} fill="none" stroke="#00E676" strokeWidth="1.8" strokeDasharray="3 2" />
              <path d={`M 54 98 L ${width - 54} 98`} stroke="#00E676" strokeWidth="2.5" strokeLinecap="round" />
              <path d={`M ${width - 54} 98 Q ${width - 42} 110 ${width - 54} 114`} fill="none" stroke="#00E676" strokeWidth="1.8" strokeDasharray="3 2" />
              <path d={`M ${width - 54} 114 L 54 114`} stroke="#00E676" strokeWidth="2.5" strokeLinecap="round" />

              <path d={`M 34 ${height / 2 - 10} L 34 ${height / 2 + 10}`} stroke="#FBC02D" strokeWidth="1.5" markerEnd="url(#whiteArrow)" />
              <path d={`M ${width - 34} ${height / 2 + 10} L ${width - 34} ${height / 2 - 10}`} stroke="#FBC02D" strokeWidth="1.5" markerEnd="url(#whiteArrow)" />

              <circle cx="34" cy="28" r="6" fill="#00E676" stroke="#FFFFFF" strokeWidth="2" />
              <rect x="20" y="12" width="64" height="15" rx="3" fill="#00E676" />
              <text x="52" y="22.5" fontSize="8" fontWeight="bold" fill="#1B2E1B" textAnchor="middle">1. MASUK (Start)</text>

              <circle cx="34" cy="46" r="6" fill="#FF1744" stroke="#FFFFFF" strokeWidth="2" />
              <rect x="20" y="48" width="64" height="15" rx="3" fill="#FF1744" />
              <text x="52" y="58.5" fontSize="8" fontWeight="bold" fill="#FFFFFF" textAnchor="middle">3. KELUAR (End)</text>

              <text x={width / 2} y="102" fontSize="14">🚜</text>
            </g>
          )}

          {/* ── 3. PATTERN 3: OVAL / SPIRAL DARI PINGGIR KE TENGAH ── */}
          {pattern === 'SPIRAL_INWARD' && (
            <g>
              {/* Outer boundary */}
              <rect x="24" y="20" width={width - 48} height={height - 52} rx="6" fill="rgba(46, 125, 50, 0.25)" stroke="#66BB6A" strokeWidth="2" />
              <rect x="34" y="28" width={width - 68} height={height - 68} rx="4" fill="none" stroke="#FBC02D" strokeWidth="1.5" strokeDasharray="4 2" />

              {/* Concentric / Inward Spiral Loops */}
              {/* Loop 1 (Outer) */}
              <rect x="42" y="36" width={width - 84} height={height - 84} rx="4" fill="none" stroke="#00E676" strokeWidth="2.5" />
              {/* Loop 2 (Middle) */}
              <rect x="58" y="48" width={width - 116} height={height - 108} rx="4" fill="none" stroke="#00E676" strokeWidth="2.5" />
              {/* Loop 3 (Inner Center) */}
              <rect x="74" y="60" width={width - 148} height={height - 132} rx="3" fill="rgba(0, 230, 118, 0.2)" stroke="#00E676" strokeWidth="2.5" />

              {/* Inward direction arrows */}
              <path d={`M ${width / 2 - 15} 36 L ${width / 2 + 15} 36`} stroke="#FFFFFF" strokeWidth="1.5" markerEnd="url(#whiteArrow)" />
              <path d={`M ${width - 42} ${height / 2 - 10} L ${width - 42} ${height / 2 + 10}`} stroke="#FFFFFF" strokeWidth="1.5" markerEnd="url(#whiteArrow)" />
              <path d={`M ${width / 2 + 15} ${height - 48} L ${width / 2 - 15} ${height - 48}`} stroke="#FFFFFF" strokeWidth="1.5" markerEnd="url(#whiteArrow)" />
              <path d={`M 58 ${height / 2 + 10} L 58 ${height / 2 - 10}`} stroke="#FFFFFF" strokeWidth="1.5" markerEnd="url(#whiteArrow)" />

              {/* Start Badge (Outer Edge) */}
              <circle cx="42" cy="36" r="6" fill="#00E676" stroke="#FFFFFF" strokeWidth="2" />
              <rect x="24" y="16" width="68" height="15" rx="3" fill="#00E676" />
              <text x="58" y="26.5" fontSize="8" fontWeight="bold" fill="#1B2E1B" textAnchor="middle">START (Pinggir)</text>

              {/* End Badge (Center) */}
              <circle cx={width / 2} cy={height / 2 - 6} r="6" fill="#FF1744" stroke="#FFFFFF" strokeWidth="2" />
              <rect x={width / 2 - 32} y={height / 2 + 2} width="64" height="15" rx="3" fill="#FF1744" />
              <text x={width / 2} y={height / 2 + 12.5} fontSize="8" fontWeight="bold" fill="#FFFFFF" textAnchor="middle">END (Tengah)</text>

              <text x={width - 56} y={height / 2 - 10} fontSize="14">🚜</text>
            </g>
          )}

          {/* ── 4. PATTERN 4: OVAL / SPIRAL DARI TENGAH KE PINGGIR ── */}
          {pattern === 'SPIRAL_OUTWARD' && (
            <g>
              {/* Outer boundary */}
              <rect x="24" y="20" width={width - 48} height={height - 52} rx="6" fill="rgba(46, 125, 50, 0.25)" stroke="#66BB6A" strokeWidth="2" />
              <rect x="34" y="28" width={width - 68} height={height - 68} rx="4" fill="none" stroke="#FBC02D" strokeWidth="1.5" strokeDasharray="4 2" />

              {/* Concentric / Outward Spiral Loops */}
              <rect x="74" y="60" width={width - 148} height={height - 132} rx="3" fill="rgba(0, 230, 118, 0.2)" stroke="#00E676" strokeWidth="2.5" />
              <rect x="58" y="48" width={width - 116} height={height - 108} rx="4" fill="none" stroke="#00E676" strokeWidth="2.5" />
              <rect x="42" y="36" width={width - 84} height={height - 84} rx="4" fill="none" stroke="#00E676" strokeWidth="2.5" />

              {/* Outward direction arrows */}
              <path d={`M ${width / 2 - 10} 60 L ${width / 2 + 10} 60`} stroke="#FFFFFF" strokeWidth="1.5" markerEnd="url(#whiteArrow)" />
              <path d={`M ${width - 58} ${height / 2 - 10} L ${width - 58} ${height / 2 + 10}`} stroke="#FFFFFF" strokeWidth="1.5" markerEnd="url(#whiteArrow)" />
              <path d={`M ${width / 2 + 15} ${height - 36} L ${width / 2 - 15} ${height - 36}`} stroke="#FFFFFF" strokeWidth="1.5" markerEnd="url(#whiteArrow)" />

              {/* Start Badge (Center) */}
              <circle cx={width / 2} cy={height / 2 - 6} r="6" fill="#00E676" stroke="#FFFFFF" strokeWidth="2" />
              <rect x={width / 2 - 32} y={height / 2 - 25} width="64" height="15" rx="3" fill="#00E676" />
              <text x={width / 2} y={height / 2 - 14.5} fontSize="8" fontWeight="bold" fill="#1B2E1B" textAnchor="middle">START (Tengah)</text>

              {/* End Badge (Outer Edge) */}
              <circle cx="42" cy={height - 48} r="6" fill="#FF1744" stroke="#FFFFFF" strokeWidth="2" />
              <rect x="24" y={height - 40} width="68" height="15" rx="3" fill="#FF1744" />
              <text x="58" y={height - 29.5} fontSize="8" fontWeight="bold" fill="#FFFFFF" textAnchor="middle">END (Pinggir)</text>

              <text x={width / 2 + 10} y={height / 2 + 2} fontSize="14">🚜</text>
            </g>
          )}
        </svg>
      ) : (
        <View style={styles.nativeFallback}>
          <Text style={styles.fallbackTitle}>
            {pattern === 'BOUSTROPHEDON'
              ? 'Pola Persegi Panjang / Zig-Zag'
              : pattern === 'HEADLAND_INNER'
              ? 'Pola Dengan Headland'
              : pattern === 'SPIRAL_INWARD'
              ? 'Pola Oval / Spiral (Pinggir ke Tengah)'
              : 'Pola Oval / Spiral (Tengah ke Pinggir)'}
          </Text>
        </View>
      )}

      {/* Diagram Legend Strip */}
      <View style={styles.legendRow}>
        <View style={styles.legendItem}>
          <View style={[styles.legendLine, { backgroundColor: '#00E676' }]} />
          <Text style={styles.legendText}>Jalur Tanam</Text>
        </View>
        <View style={styles.legendItem}>
          <View style={[styles.legendLine, { backgroundColor: '#FBC02D', height: 2 }]} />
          <Text style={styles.legendText}>Area Putar</Text>
        </View>
        <View style={styles.legendItem}>
          <View style={[styles.legendDot, { backgroundColor: '#00E676' }]} />
          <Text style={styles.legendText}>Start</Text>
          <View style={[styles.legendDot, { backgroundColor: '#FF1744', marginLeft: 4 }]} />
          <Text style={styles.legendText}>End</Text>
        </View>
        <View style={styles.legendItem}>
          <Text style={{ color: '#FFFFFF', fontSize: 10 }}>➡️</Text>
          <Text style={styles.legendText}>Arah Gerak</Text>
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#1B2B1B',
    borderRadius: 14,
    borderWidth: 1.5,
    borderColor: '#385538',
    position: 'relative',
    overflow: 'hidden',
    marginTop: 8,
    marginBottom: 4,
    alignSelf: 'center',
  },
  nativeFallback: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 16,
  },
  fallbackTitle: {
    color: '#00E676',
    fontWeight: '700',
    fontSize: 14,
    marginBottom: 4,
  },
  legendRow: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: 'rgba(15, 25, 15, 0.92)',
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
    paddingVertical: 5,
    paddingHorizontal: 8,
    borderTopWidth: 1,
    borderTopColor: 'rgba(255, 255, 255, 0.1)',
  },
  legendItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  legendLine: {
    width: 12,
    height: 3,
    borderRadius: 1.5,
  },
  legendDot: {
    width: 7,
    height: 7,
    borderRadius: 3.5,
  },
  legendText: {
    color: '#E8F5E9',
    fontSize: 9,
    fontWeight: '600',
  },
});
