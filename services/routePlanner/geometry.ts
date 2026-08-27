import { GeoPoint } from '@/types';

const EARTH_RADIUS_M = 6371000;

export function latLonToXY(point: GeoPoint, origin: GeoPoint): { x: number; y: number } {
  const dLat = ((point.lat - origin.lat) * Math.PI) / 180;
  const dLon = ((point.lon - origin.lon) * Math.PI) / 180;
  const latMid = ((point.lat + origin.lat) / 2) * (Math.PI / 180);

  const y = dLat * EARTH_RADIUS_M;
  const x = dLon * EARTH_RADIUS_M * Math.cos(latMid);

  return { x, y };
}

export function xyToLatLon(xy: { x: number; y: number }, origin: GeoPoint): GeoPoint {
  const dLat = (xy.y / EARTH_RADIUS_M) * (180 / Math.PI);
  const latMid = (origin.lat + dLat / 2) * (Math.PI / 180);
  const dLon = (xy.x / (EARTH_RADIUS_M * Math.cos(latMid))) * (180 / Math.PI);

  return {
    lat: origin.lat + dLat,
    lon: origin.lon + dLon,
  };
}

export function polygonAreaM2(points: Array<{ x: number; y: number }>): number {
  if (points.length < 3) return 0;
  let area = 0;
  for (let i = 0; i < points.length; i++) {
    const j = (i + 1) % points.length;
    area += points[i].x * points[j].y;
    area -= points[j].x * points[i].y;
  }
  return Math.abs(area) / 2;
}

export function polygonPerimeterM(points: GeoPoint[]): number {
  if (points.length < 2) return 0;
  let perimeter = 0;
  for (let i = 0; i < points.length; i++) {
    const j = (i + 1) % points.length;
    perimeter += distanceM(points[i], points[j]);
  }
  return perimeter;
}

export function distanceM(a: GeoPoint, b: GeoPoint): number {
  const dLat = ((b.lat - a.lat) * Math.PI) / 180;
  const dLon = ((b.lon - a.lon) * Math.PI) / 180;
  const lat1 = (a.lat * Math.PI) / 180;
  const lat2 = (b.lat * Math.PI) / 180;

  const sinDLat = Math.sin(dLat / 2);
  const sinDLon = Math.sin(dLon / 2);

  const aHarv =
    sinDLat * sinDLat +
    Math.cos(lat1) * Math.cos(lat2) * sinDLon * sinDLon;

  const c = 2 * Math.atan2(Math.sqrt(aHarv), Math.sqrt(1 - aHarv));
  return EARTH_RADIUS_M * c;
}

export function rotatePoint(point: { x: number; y: number }, angleDeg: number): { x: number; y: number } {
  const rad = (angleDeg * Math.PI) / 180;
  const cos = Math.cos(rad);
  const sin = Math.sin(rad);
  return {
    x: point.x * cos - point.y * sin,
    y: point.x * sin + point.y * cos,
  };
}

export function boundingBox(points: Array<{ x: number; y: number }>): {
  minX: number;
  maxX: number;
  minY: number;
  maxY: number;
} {
  if (points.length === 0) return { minX: 0, maxX: 0, minY: 0, maxY: 0 };
  let minX = points[0].x;
  let maxX = points[0].x;
  let minY = points[0].y;
  let maxY = points[0].y;

  for (let i = 1; i < points.length; i++) {
    if (points[i].x < minX) minX = points[i].x;
    if (points[i].x > maxX) maxX = points[i].x;
    if (points[i].y < minY) minY = points[i].y;
    if (points[i].y > maxY) maxY = points[i].y;
  }

  return { minX, maxX, minY, maxY };
}

export function clipLineToPolygon(
  y: number,
  polygon: Array<{ x: number; y: number }>
): number[] {
  const intersections: number[] = [];
  const n = polygon.length;

  for (let i = 0; i < n; i++) {
    const p1 = polygon[i];
    const p2 = polygon[(i + 1) % n];

    // Check if horizontal line at y intersects segment p1-p2
    if ((p1.y <= y && p2.y > y) || (p2.y <= y && p1.y > y)) {
      // Calculate x coordinate of intersection
      const t = (y - p1.y) / (p2.y - p1.y);
      const x = p1.x + t * (p2.x - p1.x);
      intersections.push(x);
    }
  }

  return intersections.sort((a, b) => a - b);
}

export function shrinkPolygon(
  polygon: Array<{ x: number; y: number }>,
  offset: number
): Array<{ x: number; y: number }> {
  if (offset <= 0 || polygon.length < 3) return polygon;

  // Calculate centroid
  let cx = 0;
  let cy = 0;
  for (const p of polygon) {
    cx += p.x;
    cy += p.y;
  }
  cx /= polygon.length;
  cy /= polygon.length;

  // Simple scaling towards centroid
  const shrunken: Array<{ x: number; y: number }> = [];
  for (const p of polygon) {
    const dx = p.x - cx;
    const dy = p.y - cy;
    const dist = Math.sqrt(dx * dx + dy * dy);
    if (dist <= offset) {
      shrunken.push({ x: cx, y: cy });
    } else {
      const scale = (dist - offset) / dist;
      shrunken.push({
        x: cx + dx * scale,
        y: cy + dy * scale,
      });
    }
  }

  return shrunken;
}

export function calculateOptimalOrientation(boundary: GeoPoint[]): number {
  if (boundary.length < 2) return 0;
  let maxDist = 0;
  let optimalAngle = 0;

  for (let i = 0; i < boundary.length; i++) {
    const p1 = boundary[i];
    const p2 = boundary[(i + 1) % boundary.length];
    const dist = distanceM(p1, p2);
    if (dist > maxDist) {
      maxDist = dist;
      const dLat = p2.lat - p1.lat;
      const dLon = p2.lon - p1.lon;
      const rad = Math.atan2(dLon, dLat); // angle from North (0 deg)
      let deg = Math.round((rad * 180) / Math.PI);
      if (deg < 0) deg += 360;
      optimalAngle = deg % 180; // normalized to 0-180
    }
  }

  return optimalAngle;
}

