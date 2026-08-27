import React from 'react';
import { View, StyleSheet, Text, Platform } from 'react-native';
import { GeoPoint } from '@/types';
import { Colors } from '@/constants/theme';

interface FieldBoundaryCanvasProps {
  boundary: GeoPoint[];
  width: number;
  height: number;
  showPoints?: boolean;
}

export const FieldBoundaryCanvas: React.FC<FieldBoundaryCanvasProps> = ({
  boundary,
  width,
  height,
  showPoints = true,
}) => {
  if (!boundary || boundary.length < 3) {
    return (
      <View style={[styles.container, { width, height }]}>
        <Text style={styles.placeholder}>Masukkan minimal 3 titik batas</Text>
      </View>
    );
  }

  // Calculate bounding box for normalization
  let minLat = boundary[0].lat;
  let maxLat = boundary[0].lat;
  let minLon = boundary[0].lon;
  let maxLon = boundary[0].lon;

  for (const p of boundary) {
    if (p.lat < minLat) minLat = p.lat;
    if (p.lat > maxLat) maxLat = p.lat;
    if (p.lon < minLon) minLon = p.lon;
    if (p.lon > maxLon) maxLon = p.lon;
  }

  const latSpan = Math.max(0.00001, maxLat - minLat);
  const lonSpan = Math.max(0.00001, maxLon - minLon);

  const padding = 28;
  const drawWidth = width - padding * 2;
  const drawHeight = height - padding * 2;

  // Project lat/lon to pixel coordinates
  const points = boundary.map((p) => {
    const x = padding + ((p.lon - minLon) / lonSpan) * drawWidth;
    const y = padding + ((maxLat - p.lat) / latSpan) * drawHeight;
    return { x, y };
  });

  const svgPointsString = points.map((p) => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ');

  // On Web: Render standard SVG for crystal-clear vector graphics
  if (Platform.OS === 'web') {
    return (
      <View style={[styles.container, { width, height }]}>
        {/* @ts-ignore */}
        <svg width={width} height={height} style={{ position: 'absolute', top: 0, left: 0 }}>
          {/* Shaded polygon background */}
          <polygon
            points={svgPointsString}
            fill="rgba(124, 179, 66, 0.25)"
            stroke="#558B2F"
            strokeWidth="3"
            strokeLinejoin="round"
          />
          {/* Point markers */}
          {showPoints &&
            points.map((p, idx) => (
              <g key={`pt-${idx}`}>
                <circle cx={p.x} cy={p.y} r="8" fill="#7CB342" stroke="#FFFFFF" strokeWidth="2.5" />
                <text
                  x={p.x}
                  y={p.y + 3.5}
                  fontSize="9"
                  fontWeight="bold"
                  fill="#FFFFFF"
                  textAnchor="middle"
                >
                  {idx + 1}
                </text>
              </g>
            ))}
        </svg>
      </View>
    );
  }

  // On Native: Render using exact midpoint-rotated line segments
  return (
    <View style={[styles.container, { width, height }]}>
      {points.map((p1, idx) => {
        const p2 = points[(idx + 1) % points.length];
        const dx = p2.x - p1.x;
        const dy = p2.y - p1.y;
        const length = Math.sqrt(dx * dx + dy * dy);
        const angle = Math.atan2(dy, dx);
        const mx = (p1.x + p2.x) / 2;
        const my = (p1.y + p2.y) / 2;

        return (
          <View
            key={`native-line-${idx}`}
            style={{
              position: 'absolute',
              left: mx - length / 2,
              top: my - 1.5,
              width: length,
              height: 3,
              backgroundColor: Colors.primaryDark,
              transform: [{ rotate: `${angle}rad` }],
            }}
          />
        );
      })}

      {showPoints &&
        points.map((p, idx) => (
          <View
            key={`native-pt-${idx}`}
            style={[
              styles.pointMarker,
              {
                left: p.x - 9,
                top: p.y - 9,
              },
            ]}
          >
            <Text style={styles.pointNumber}>{idx + 1}</Text>
          </View>
        ))}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#F1F8E9',
    borderRadius: 12,
    borderWidth: 1.5,
    borderColor: '#C5E1A5',
    position: 'relative',
    overflow: 'hidden',
    alignItems: 'center',
    justifyContent: 'center',
  },
  placeholder: {
    color: Colors.textSecondary,
    fontSize: 14,
  },
  pointMarker: {
    position: 'absolute',
    width: 18,
    height: 18,
    borderRadius: 9,
    backgroundColor: Colors.primary,
    borderWidth: 2,
    borderColor: '#FFFFFF',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 10,
  },
  pointNumber: {
    fontSize: 9,
    color: '#FFFFFF',
    fontWeight: '700',
  },
});
