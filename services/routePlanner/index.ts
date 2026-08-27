import { RouteGenerationInput, RouteResult, Waypoint, GeoPoint, RoutePattern } from '@/types';
import { MIN_FIELD_AREA_M2, MIN_FIELD_POINTS } from '@/constants/defaults';
import * as Geom from './geometry';

export { calculateOptimalOrientation } from './geometry';

export function validateField(boundary: GeoPoint[]): {
  valid: boolean;
  error?: string;
  areaM2: number;
  perimeterM: number;
} {
  if (boundary.length < MIN_FIELD_POINTS) {
    return {
      valid: false,
      error: `Minimal ${MIN_FIELD_POINTS} titik batas sawah`,
      areaM2: 0,
      perimeterM: 0,
    };
  }

  const origin = boundary[0];
  const xyPoints = boundary.map((p) => Geom.latLonToXY(p, origin));
  const areaM2 = Geom.polygonAreaM2(xyPoints);
  const perimeterM = Geom.polygonPerimeterM(boundary);

  if (areaM2 < MIN_FIELD_AREA_M2) {
    return {
      valid: false,
      error: `Luas sawah terlalu kecil (${Math.round(areaM2)} m², min ${MIN_FIELD_AREA_M2} m²)`,
      areaM2,
      perimeterM,
    };
  }

  return { valid: true, areaM2, perimeterM };
}

export function generateCoverageRoute(input: RouteGenerationInput): RouteResult {
  const {
    fieldBoundary,
    machineWidthM,
    headlandWidthM,
    orientationDeg,
    pattern = 'BOUSTROPHEDON',
  } = input;

  if (fieldBoundary.length < MIN_FIELD_POINTS) {
    return {
      waypoints: [],
      totalLanes: 0,
      totalDistanceM: 0,
      estimatedCoveragePct: 0,
      pattern,
    };
  }

  // 1. Origin at polygon centroid
  let sumLat = 0;
  let sumLon = 0;
  for (const p of fieldBoundary) {
    sumLat += p.lat;
    sumLon += p.lon;
  }
  const origin: GeoPoint = {
    lat: sumLat / fieldBoundary.length,
    lon: sumLon / fieldBoundary.length,
  };

  const xyBoundary = fieldBoundary.map((p) => Geom.latLonToXY(p, origin));
  const totalFieldArea = Geom.polygonAreaM2(xyBoundary);

  let waypoints: Waypoint[] = [];
  let totalLanes = 0;
  let totalDistanceM = 0;

  const effectiveLaneWidth = Math.max(0.8, machineWidthM);

  // ── PATTERN 1: BOUSTROPHEDON (ZIG-ZAG PARALEL) ──────────────────────────
  if (pattern === 'BOUSTROPHEDON') {
    const rotatedBoundary = xyBoundary.map((p) => Geom.rotatePoint(p, -orientationDeg));
    let workingBoundary = Geom.shrinkPolygon(rotatedBoundary, headlandWidthM);
    let bbox = Geom.boundingBox(workingBoundary);

    if (bbox.maxY - bbox.minY < effectiveLaneWidth * 2 || bbox.maxX - bbox.minX < effectiveLaneWidth * 2) {
      workingBoundary = Geom.shrinkPolygon(rotatedBoundary, 0.5);
      bbox = Geom.boundingBox(workingBoundary);
    }

    let currentOrder = 0;
    let laneIndex = 0;
    let previousPoint: GeoPoint | null = null;
    let direction = 1;

    const yStart = bbox.minY + effectiveLaneWidth / 2;
    const yEnd = bbox.maxY - effectiveLaneWidth / 2;

    for (let y = yStart; y <= yEnd; y += effectiveLaneWidth) {
      const intersections = Geom.clipLineToPolygon(y, workingBoundary);

      if (intersections.length >= 2) {
        for (let i = 0; i < intersections.length - 1; i += 2) {
          const xLeft = intersections[i];
          const xRight = intersections[i + 1];

          if (Math.abs(xRight - xLeft) < 1.0) continue;

          const xStart = direction === 1 ? xLeft : xRight;
          const xStop = direction === 1 ? xRight : xLeft;

          const p1Rot = Geom.rotatePoint({ x: xStart, y }, orientationDeg);
          const p2Rot = Geom.rotatePoint({ x: xStop, y }, orientationDeg);

          const geo1 = Geom.xyToLatLon(p1Rot, origin);
          const geo2 = Geom.xyToLatLon(p2Rot, origin);

          if (previousPoint) {
            totalDistanceM += Geom.distanceM(previousPoint, geo1);
          }

          waypoints.push({
            lat: geo1.lat,
            lon: geo1.lon,
            order: currentOrder++,
            type: 'lane',
            laneIndex,
          });

          totalDistanceM += Geom.distanceM(geo1, geo2);

          waypoints.push({
            lat: geo2.lat,
            lon: geo2.lon,
            order: currentOrder++,
            type: 'lane',
            laneIndex,
          });

          previousPoint = geo2;
          laneIndex++;
          direction *= -1;
        }
      }
    }
    totalLanes = Math.max(1, laneIndex);
  }

  // ── PATTERN 2: HEADLAND FIRST + INNER LANES ─────────────────────────────
  else if (pattern === 'HEADLAND_INNER') {
    let currentOrder = 0;
    let laneIndex = 0;
    let previousPoint: GeoPoint | null = null;

    // 1. Outer headland circuit
    const headlandCircuit = Geom.shrinkPolygon(xyBoundary, Math.max(0.5, headlandWidthM / 2));
    for (let i = 0; i <= headlandCircuit.length; i++) {
      const p = headlandCircuit[i % headlandCircuit.length];
      const geo = Geom.xyToLatLon(p, origin);

      if (previousPoint) {
        totalDistanceM += Geom.distanceM(previousPoint, geo);
      }
      waypoints.push({
        lat: geo.lat,
        lon: geo.lon,
        order: currentOrder++,
        type: 'headland',
        laneIndex: 0,
      });
      previousPoint = geo;
    }
    laneIndex = 1;

    // 2. Inner parallel lanes
    const rotatedBoundary = xyBoundary.map((p) => Geom.rotatePoint(p, -orientationDeg));
    const innerBoundary = Geom.shrinkPolygon(rotatedBoundary, headlandWidthM + 1.0);
    const bbox = Geom.boundingBox(innerBoundary);

    let direction = 1;
    const yStart = bbox.minY + effectiveLaneWidth / 2;
    const yEnd = bbox.maxY - effectiveLaneWidth / 2;

    for (let y = yStart; y <= yEnd; y += effectiveLaneWidth) {
      const intersections = Geom.clipLineToPolygon(y, innerBoundary);
      if (intersections.length >= 2) {
        for (let i = 0; i < intersections.length - 1; i += 2) {
          const xLeft = intersections[i];
          const xRight = intersections[i + 1];
          if (Math.abs(xRight - xLeft) < 1.0) continue;

          const xStart = direction === 1 ? xLeft : xRight;
          const xStop = direction === 1 ? xRight : xLeft;

          const p1Rot = Geom.rotatePoint({ x: xStart, y }, orientationDeg);
          const p2Rot = Geom.rotatePoint({ x: xStop, y }, orientationDeg);

          const geo1 = Geom.xyToLatLon(p1Rot, origin);
          const geo2 = Geom.xyToLatLon(p2Rot, origin);

          if (previousPoint) {
            totalDistanceM += Geom.distanceM(previousPoint, geo1);
          }

          waypoints.push({
            lat: geo1.lat,
            lon: geo1.lon,
            order: currentOrder++,
            type: 'lane',
            laneIndex,
          });

          totalDistanceM += Geom.distanceM(geo1, geo2);

          waypoints.push({
            lat: geo2.lat,
            lon: geo2.lon,
            order: currentOrder++,
            type: 'lane',
            laneIndex,
          });

          previousPoint = geo2;
          laneIndex++;
          direction *= -1;
        }
      }
    }
    totalLanes = Math.max(1, laneIndex);
  }

  // ── PATTERN 3 & 4: OVAL / SPIRAL (INWARD / OUTWARD) ──────────────────────
  else if (pattern === 'SPIRAL_INWARD' || pattern === 'SPIRAL_OUTWARD') {
    let currentOrder = 0;
    let previousPoint: GeoPoint | null = null;

    // Centroid of local XY polygon
    let cx = 0;
    let cy = 0;
    for (const p of xyBoundary) {
      cx += p.x;
      cy += p.y;
    }
    cx /= xyBoundary.length;
    cy /= xyBoundary.length;

    // Calculate maximum radius to vertices
    let maxDist = 0;
    for (const p of xyBoundary) {
      const d = Math.sqrt((p.x - cx) ** 2 + (p.y - cy) ** 2);
      if (d > maxDist) maxDist = d;
    }

    const minOffset = Math.max(0.5, headlandWidthM);
    const maxOffset = Math.max(minOffset + effectiveLaneWidth, maxDist - 2);

    // Generate concentric polygon loops
    const rings: Array<Array<{ x: number; y: number }>> = [];

    // Optional rotation by orientationDeg
    const rotatedBoundary = xyBoundary.map((p) => Geom.rotatePoint(p, -orientationDeg));

    for (let offset = minOffset; offset < maxOffset; offset += effectiveLaneWidth) {
      const ring = Geom.shrinkPolygon(rotatedBoundary, offset);
      if (ring.length < 3) break;
      const ringArea = Geom.polygonAreaM2(ring);
      if (ringArea < 20) break;

      // Rotate back by +orientationDeg
      const unrotatedRing = ring.map((p) => Geom.rotatePoint(p, orientationDeg));
      rings.push(unrotatedRing);
    }

    if (rings.length === 0) {
      rings.push(xyBoundary);
    }

    // If OUTWARD: reverse rings so we go from inside center to outside perimeter
    const orderedRings = pattern === 'SPIRAL_INWARD' ? rings : [...rings].reverse();

    orderedRings.forEach((ring, ringIdx) => {
      // Loop through all points in order around perimeter
      for (let i = 0; i <= ring.length; i++) {
        const pt = ring[i % ring.length];
        const geo = Geom.xyToLatLon(pt, origin);

        if (previousPoint) {
          totalDistanceM += Geom.distanceM(previousPoint, geo);
        }

        waypoints.push({
          lat: geo.lat,
          lon: geo.lon,
          order: currentOrder++,
          type: 'lane',
          laneIndex: ringIdx,
        });

        previousPoint = geo;
      }
    });

    totalLanes = Math.max(1, orderedRings.length);
  }

  // Fallback if needed
  if (waypoints.length === 0 && fieldBoundary.length >= 3) {
    for (let i = 0; i <= fieldBoundary.length; i++) {
      const p = fieldBoundary[i % fieldBoundary.length];
      waypoints.push({
        lat: p.lat,
        lon: p.lon,
        order: i,
        type: 'lane',
        laneIndex: 0,
      });
    }
    totalLanes = 1;
    totalDistanceM = Geom.polygonPerimeterM(fieldBoundary);
  }

  const coveredAreaM2 = totalDistanceM * machineWidthM;
  const estimatedCoveragePct = totalFieldArea > 0
    ? Math.min(99, Math.max(70, Math.round((coveredAreaM2 / totalFieldArea) * 100)))
    : 94;

  return {
    waypoints,
    totalLanes,
    totalDistanceM: Math.round(totalDistanceM),
    estimatedCoveragePct,
    pattern,
  };
}
