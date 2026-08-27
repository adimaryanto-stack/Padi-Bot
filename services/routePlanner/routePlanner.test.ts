import { generateCoverageRoute, validateField } from './index';
import { polygonAreaM2, latLonToXY, xyToLatLon, distanceM } from './geometry';
import { GeoPoint } from '@/types';

describe('Route Planner Geometry', () => {
  const origin: GeoPoint = { lat: -6.9234, lon: 107.6100 };

  test('latLonToXY and xyToLatLon inverse conversion', () => {
    const point: GeoPoint = { lat: -6.9240, lon: 107.6108 };
    const xy = latLonToXY(point, origin);
    const convertedBack = xyToLatLon(xy, origin);

    expect(convertedBack.lat).toBeCloseTo(point.lat, 4);
    expect(convertedBack.lon).toBeCloseTo(point.lon, 4);
  });

  test('polygonAreaM2 calculates rectangle area correctly', () => {
    // 50m x 20m rectangle = 1000 m²
    const rect = [
      { x: 0, y: 0 },
      { x: 50, y: 0 },
      { x: 50, y: 20 },
      { x: 0, y: 20 },
    ];
    const area = polygonAreaM2(rect);
    expect(area).toBe(1000);
  });

  test('distanceM calculates distance accurately', () => {
    const p1: GeoPoint = { lat: -6.9234, lon: 107.6100 };
    const p2: GeoPoint = { lat: -6.9234, lon: 107.6109 }; // ~100m east
    const dist = distanceM(p1, p2);
    expect(dist).toBeGreaterThan(90);
    expect(dist).toBeLessThan(110);
  });
});

describe('Route Generation', () => {
  const sampleBoundary: GeoPoint[] = [
    { lat: -6.9234, lon: 107.6100 },
    { lat: -6.9240, lon: 107.6108 },
    { lat: -6.9228, lon: 107.6115 },
    { lat: -6.9222, lon: 107.6105 },
  ];

  test('validateField accepts valid field with >3 points and >100m² area', () => {
    const res = validateField(sampleBoundary);
    expect(res.valid).toBe(true);
    expect(res.areaM2).toBeGreaterThan(100);
  });

  test('validateField rejects field with less than 3 points', () => {
    const invalid = [{ lat: -6.9234, lon: 107.6100 }, { lat: -6.9240, lon: 107.6108 }];
    const res = validateField(invalid);
    expect(res.valid).toBe(false);
  });

  test('generateCoverageRoute produces waypoints and estimated coverage', () => {
    const result = generateCoverageRoute({
      fieldBoundary: sampleBoundary,
      machineWidthM: 1.5,
      headlandWidthM: 3.0,
      orientationDeg: 0,
    });

    expect(result.waypoints.length).toBeGreaterThan(0);
    expect(result.totalLanes).toBeGreaterThan(0);
    expect(result.totalDistanceM).toBeGreaterThan(0);
    expect(result.estimatedCoveragePct).toBeGreaterThanOrEqual(60);
    expect(result.estimatedCoveragePct).toBeLessThanOrEqual(100);
  });
});
