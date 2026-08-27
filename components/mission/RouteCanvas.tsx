import React from 'react';
import { View, StyleSheet, Text, Platform } from 'react-native';
import { GeoPoint, Waypoint } from '@/types';
import { Colors } from '@/constants/theme';
import * as Geom from '@/services/routePlanner/geometry';

interface RouteCanvasProps {
  boundary: GeoPoint[];
  waypoints: Waypoint[];
  machinePosition?: GeoPoint;
  width: number;
  height: number;
  completedLaneIndex?: number;
  headlandWidthM?: number;
  showLegend?: boolean;
}

export const RouteCanvas: React.FC<RouteCanvasProps> = ({
  boundary,
  waypoints,
  machinePosition,
  width,
  height,
  completedLaneIndex = -1,
  headlandWidthM = 1.5,
  showLegend = true,
}) => {
  if (!boundary || boundary.length < 3) {
    return (
      <View style={[styles.container, { width, height }]}>
        <Text style={styles.placeholder}>Belum ada data batas sawah</Text>
      </View>
    );
  }

  // Calculate bounding box strictly based on boundary points
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

  const padding = 36;
  const drawWidth = width - padding * 2;
  const drawHeight = height - padding * 2;

  const toScreen = (lat: number, lon: number) => ({
    x: padding + ((lon - minLon) / lonSpan) * drawWidth,
    y: padding + ((maxLat - lat) / latSpan) * drawHeight,
  });

  // 1. Boundary Screen Points
  const boundaryPoints = boundary.map((p) => toScreen(p.lat, p.lon));
  const svgBoundaryStr = boundaryPoints.map((p) => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ');

  // 2. Headland Inward Ring Points (Yellow boundary zone)
  const origin: GeoPoint = {
    lat: (minLat + maxLat) / 2,
    lon: (minLon + maxLon) / 2,
  };
  const xyBoundary = boundary.map((p) => Geom.latLonToXY(p, origin));
  const xyHeadland = Geom.shrinkPolygon(xyBoundary, Math.max(0.8, headlandWidthM));
  const headlandGeo = xyHeadland.map((p) => Geom.xyToLatLon(p, origin));
  const headlandScreenPoints = headlandGeo.map((p) => toScreen(p.lat, p.lon));
  const svgHeadlandStr = headlandScreenPoints.map((p) => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ');

  // 3. Clamped Screen Waypoints
  const screenWaypoints = waypoints.map((w) => {
    const pt = toScreen(w.lat, w.lon);
    return {
      x: Math.max(padding - 8, Math.min(width - padding + 8, pt.x)),
      y: Math.max(padding - 8, Math.min(height - padding + 8, pt.y)),
      laneIndex: w.laneIndex ?? 0,
      order: w.order,
      type: w.type,
    };
  });

  const machineScreenPos = machinePosition
    ? toScreen(machinePosition.lat, machinePosition.lon)
    : null;

  // Dynamic lane stroke width based on total waypoints
  const laneStrokeWidth =
    screenWaypoints.length > 100
      ? 1.5
      : screenWaypoints.length > 50
      ? 2.0
      : 2.8;

  // On Web: High-Def SVG Rendering
  if (Platform.OS === 'web') {
    return (
      <View style={[styles.container, { width, height }]}>
        {/* @ts-ignore */}
        <svg width={width} height={height} style={{ position: 'absolute', top: 0, left: 0 }}>
          <defs>
            {/* Subtle Agricultural Field Grid */}
            <pattern id="darkGrid" width="20" height="20" patternUnits="userSpaceOnUse">
              <path d="M 20 0 L 0 0 0 20" fill="none" stroke="rgba(255,255,255,0.05)" strokeWidth="1" />
            </pattern>
            {/* Directional arrow marker */}
            <marker id="laneArrow" viewBox="0 0 6 6" refX="3" refY="3" markerWidth="4" markerHeight="4" orient="auto-start-reverse">
              <path d="M 0 1 L 5 3 L 0 5 z" fill="#FFFFFF" opacity="0.8" />
            </marker>
          </defs>

          {/* Background Grid */}
          <rect width={width} height={height} fill="url(#darkGrid)" />

          {/* Outer Field Boundary (Dark Green field shaded polygon) */}
          <polygon
            points={svgBoundaryStr}
            fill="rgba(46, 125, 50, 0.22)"
            stroke="#66BB6A"
            strokeWidth="2.5"
            strokeLinejoin="round"
          />

          {/* Headland / Area Putar Ring (Yellow Dashed Line) */}
          {headlandScreenPoints.length >= 3 && (
            <polygon
              points={svgHeadlandStr}
              fill="none"
              stroke="#FBC02D"
              strokeWidth="2"
              strokeDasharray="5 3"
              strokeLinejoin="round"
            />
          )}

          {/* Route Lanes & Smooth Turning Connectors */}
          {screenWaypoints.map((p1, idx) => {
            if (idx === screenWaypoints.length - 1) return null;
            const p2 = screenWaypoints[idx + 1];

            const isTurn = p1.laneIndex !== p2.laneIndex;
            const isCompleted = p1.laneIndex < completedLaneIndex;
            const isCurrent = p1.laneIndex === completedLaneIndex;

            const strokeColor = isCompleted
              ? '#81C784' // soft green for completed
              : isCurrent
              ? '#FFD54F' // yellow for current
              : '#00E676'; // bright fluorescent green for planting track

            const strokeWidth = isTurn ? 1.4 : laneStrokeWidth;
            const strokeOpacity = isTurn ? 0.75 : 1.0;
            const strokeDash = isTurn ? '3 2' : 'none';

            // Show arrows every 3rd or 4th waypoint on straight segments
            const showArrow = !isTurn && (idx % 3 === 0);

            return (
              <line
                key={`lane-${idx}`}
                x1={p1.x.toFixed(1)}
                y1={p1.y.toFixed(1)}
                x2={p2.x.toFixed(1)}
                y2={p2.y.toFixed(1)}
                stroke={strokeColor}
                strokeWidth={strokeWidth}
                strokeOpacity={strokeOpacity}
                strokeDasharray={strokeDash}
                strokeLinecap="round"
                markerEnd={showArrow ? 'url(#laneArrow)' : undefined}
              />
            );
          })}

          {/* Start Point Marker (Green) */}
          {screenWaypoints.length > 0 && (
            <g>
              <circle
                cx={screenWaypoints[0].x}
                cy={screenWaypoints[0].y}
                r="7"
                fill="#00E676"
                stroke="#FFFFFF"
                strokeWidth="2"
              />
              <rect
                x={screenWaypoints[0].x - 18}
                y={screenWaypoints[0].y - 20}
                width="36"
                height="14"
                rx="4"
                fill="rgba(0, 230, 118, 0.9)"
              />
              <text
                x={screenWaypoints[0].x}
                y={screenWaypoints[0].y - 10}
                fontSize="8.5"
                fontWeight="bold"
                fill="#1B2E1B"
                textAnchor="middle"
              >
                START
              </text>
            </g>
          )}

          {/* End Point Marker (Red) */}
          {screenWaypoints.length > 1 && (
            <g>
              <circle
                cx={screenWaypoints[screenWaypoints.length - 1].x}
                cy={screenWaypoints[screenWaypoints.length - 1].y}
                r="7"
                fill="#FF1744"
                stroke="#FFFFFF"
                strokeWidth="2"
              />
              <rect
                x={screenWaypoints[screenWaypoints.length - 1].x - 14}
                y={screenWaypoints[screenWaypoints.length - 1].y - 20}
                width="28"
                height="14"
                rx="4"
                fill="rgba(255, 23, 68, 0.9)"
              />
              <text
                x={screenWaypoints[screenWaypoints.length - 1].x}
                y={screenWaypoints[screenWaypoints.length - 1].y - 10}
                fontSize="8.5"
                fontWeight="bold"
                fill="#FFFFFF"
                textAnchor="middle"
              >
                END
              </text>
            </g>
          )}

          {/* Live Machine Tractor Marker */}
          {machineScreenPos && (
            <g>
              <circle
                cx={machineScreenPos.x}
                cy={machineScreenPos.y}
                r="16"
                fill="rgba(255, 235, 59, 0.3)"
              />
              <circle
                cx={machineScreenPos.x}
                cy={machineScreenPos.y}
                r="8"
                fill="#FFEB3B"
                stroke="#000000"
                strokeWidth="1.5"
              />
              <text
                x={machineScreenPos.x}
                y={machineScreenPos.y + 4}
                fontSize="11"
                textAnchor="middle"
              >
                🚜
              </text>
            </g>
          )}
        </svg>

        {/* Legend Overlay in Canvas Bottom Left */}
        {showLegend && (
          <View style={styles.legendContainer}>
            <View style={styles.legendItem}>
              <View style={[styles.legendLine, { backgroundColor: '#00E676' }]} />
              <Text style={styles.legendText}>Jalur Tanam</Text>
            </View>
            <View style={styles.legendItem}>
              <View style={[styles.legendLine, { backgroundColor: '#FBC02D', height: 2 }]} />
              <Text style={styles.legendText}>Area Putar / Headland</Text>
            </View>
            <View style={styles.legendItem}>
              <View style={[styles.legendDot, { backgroundColor: '#00E676' }]} />
              <Text style={styles.legendText}>Start</Text>
              <View style={[styles.legendDot, { backgroundColor: '#FF1744', marginLeft: 6 }]} />
              <Text style={styles.legendText}>End</Text>
            </View>
          </View>
        )}
      </View>
    );
  }

  // On Native (Android):
  return (
    <View style={[styles.container, { width, height }]}>
      {/* Outer Boundary */}
      {boundaryPoints.map((p1, idx) => {
        const p2 = boundaryPoints[(idx + 1) % boundaryPoints.length];
        const dx = p2.x - p1.x;
        const dy = p2.y - p1.y;
        const length = Math.sqrt(dx * dx + dy * dy);
        const angle = Math.atan2(dy, dx);
        const mx = (p1.x + p2.x) / 2;
        const my = (p1.y + p2.y) / 2;

        return (
          <View
            key={`native-boundary-${idx}`}
            style={{
              position: 'absolute',
              left: mx - length / 2,
              top: my - 1.5,
              width: length,
              height: 3,
              backgroundColor: '#66BB6A',
              transform: [{ rotate: `${angle}rad` }],
            }}
          />
        );
      })}

      {/* Headland Zone Ring */}
      {headlandScreenPoints.map((p1, idx) => {
        const p2 = headlandScreenPoints[(idx + 1) % headlandScreenPoints.length];
        const dx = p2.x - p1.x;
        const dy = p2.y - p1.y;
        const length = Math.sqrt(dx * dx + dy * dy);
        const angle = Math.atan2(dy, dx);
        const mx = (p1.x + p2.x) / 2;
        const my = (p1.y + p2.y) / 2;

        return (
          <View
            key={`native-headland-${idx}`}
            style={{
              position: 'absolute',
              left: mx - length / 2,
              top: my - 1,
              width: length,
              height: 2,
              backgroundColor: '#FBC02D',
              opacity: 0.8,
              transform: [{ rotate: `${angle}rad` }],
            }}
          />
        );
      })}

      {/* Route Lanes */}
      {screenWaypoints.map((p1, idx) => {
        if (idx === screenWaypoints.length - 1) return null;
        const p2 = screenWaypoints[idx + 1];
        const dx = p2.x - p1.x;
        const dy = p2.y - p1.y;
        const length = Math.sqrt(dx * dx + dy * dy);
        const angle = Math.atan2(dy, dx);
        const mx = (p1.x + p2.x) / 2;
        const my = (p1.y + p2.y) / 2;

        const isTurn = p1.laneIndex !== p2.laneIndex;
        const isCompleted = p1.laneIndex < completedLaneIndex;
        const isCurrent = p1.laneIndex === completedLaneIndex;

        const color = isCompleted
          ? '#81C784'
          : isCurrent
          ? '#FFD54F'
          : '#00E676';

        return (
          <View
            key={`native-route-${idx}`}
            style={{
              position: 'absolute',
              left: mx - length / 2,
              top: my - 1,
              width: length,
              height: isTurn ? 1.2 : laneStrokeWidth,
              opacity: isTurn ? 0.7 : 1,
              backgroundColor: color,
              transform: [{ rotate: `${angle}rad` }],
            }}
          />
        );
      })}

      {/* Start Marker */}
      {screenWaypoints.length > 0 && (
        <View
          style={[
            styles.startMarker,
            {
              left: screenWaypoints[0].x - 6,
              top: screenWaypoints[0].y - 6,
            },
          ]}
        />
      )}

      {/* End Marker */}
      {screenWaypoints.length > 1 && (
        <View
          style={[
            styles.endMarker,
            {
              left: screenWaypoints[screenWaypoints.length - 1].x - 6,
              top: screenWaypoints[screenWaypoints.length - 1].y - 6,
            },
          ]}
        />
      )}

      {/* Tractor Marker */}
      {machineScreenPos && (
        <View
          style={[
            styles.machineMarker,
            {
              left: machineScreenPos.x - 12,
              top: machineScreenPos.y - 12,
            },
          ]}
        >
          <View style={styles.machinePulse} />
          <Text style={styles.machineIcon}>🚜</Text>
        </View>
      )}

      {/* Native Legend */}
      {showLegend && (
        <View style={styles.legendContainer}>
          <View style={styles.legendItem}>
            <View style={[styles.legendLine, { backgroundColor: '#00E676' }]} />
            <Text style={styles.legendText}>Jalur Tanam</Text>
          </View>
          <View style={styles.legendItem}>
            <View style={[styles.legendLine, { backgroundColor: '#FBC02D' }]} />
            <Text style={styles.legendText}>Area Putar</Text>
          </View>
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#1E2D1E', // Dark agricultural green field tone
    borderRadius: 16,
    borderWidth: 2,
    borderColor: '#335033',
    position: 'relative',
    overflow: 'hidden',
    alignItems: 'center',
    justifyContent: 'center',
  },
  placeholder: {
    color: '#A5D6A7',
    fontSize: 14,
  },
  startMarker: {
    position: 'absolute',
    width: 14,
    height: 14,
    borderRadius: 7,
    backgroundColor: '#00E676',
    borderWidth: 2,
    borderColor: '#FFFFFF',
    zIndex: 10,
  },
  endMarker: {
    position: 'absolute',
    width: 14,
    height: 14,
    borderRadius: 7,
    backgroundColor: '#FF1744',
    borderWidth: 2,
    borderColor: '#FFFFFF',
    zIndex: 10,
  },
  machineMarker: {
    position: 'absolute',
    width: 24,
    height: 24,
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 20,
  },
  machinePulse: {
    position: 'absolute',
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: 'rgba(255, 235, 59, 0.35)',
  },
  machineIcon: {
    fontSize: 13,
  },
  legendContainer: {
    position: 'absolute',
    bottom: 8,
    left: 8,
    backgroundColor: 'rgba(20, 32, 20, 0.85)',
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.15)',
    gap: 3,
  },
  legendItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  legendLine: {
    width: 14,
    height: 3,
    borderRadius: 1.5,
  },
  legendDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  legendText: {
    color: '#E8F5E9',
    fontSize: 9.5,
    fontWeight: '600',
  },
});
