/**
 * PadiBot Autonomous Field Mission Simulator v3.0
 * Multi-Pattern Smart Coverage Path Engine:
 * 1. Boustrophedon Standar (Vertical parallel sweep)
 * 2. Adaptif Kontur (Diagonal contour-aligned sweep)
 * 3. Spiral Obat Nyamuk (Concentric outward spiral)
 * 
 * Guaranteed 100% containment inside solid green boundary line.
 */

class FieldSimulator {
  constructor(canvasId) {
    this.canvas = document.getElementById(canvasId);
    if (!this.canvas) return;
    this.ctx = this.canvas.getContext('2d');
    
    // State
    this.points = []; // Boundary polygon vertices (Solid green line)
    this.waypoints = []; // Unified planned path waypoints (Dotted blue line)
    this.plantedSeedlings = []; // Planted spots
    this.isPolygonClosed = false;
    this.currentPattern = 'boustrophedon'; // 'boustrophedon', 'adaptive', 'spiral'
    
    // Rover state
    this.rover = {
      x: 0,
      y: 0,
      targetIdx: 0,
      heading: 0,
      speed: 1.8, // pixels per frame
      status: 'IDLE', // IDLE, RUNNING, PAUSED, COMPLETED, ESTOP
      distanceTraveled: 0,
      lastPlantPos: { x: 0, y: 0 },
      isDispenserActive: false
    };
    
    this.speedMultiplier = 1;
    this.rowSpacing = 28; // pixels between rows
    this.plantSpacing = 16; // pixels between seedlings
    this.safetyMargin = 22; // Safe inset distance from solid green boundary
    this.animationFrameId = null;
    
    this.initCanvas();
    this.bindEvents();
    this.loadPreset(0); // Default rectangular field
    this.startLoop();
  }

  initCanvas() {
    const rect = this.canvas.getBoundingClientRect();
    const dpr = window.devicePixelRatio || 1;
    this.canvas.width = rect.width * dpr;
    this.canvas.height = (rect.height || 420) * dpr;
    this.ctx.scale(dpr, dpr);
    this.width = rect.width;
    this.height = rect.height || 420;
  }

  bindEvents() {
    window.addEventListener('resize', () => {
      this.initCanvas();
      this.draw();
    });

    this.canvas.addEventListener('click', (e) => {
      if (this.rover.status === 'RUNNING') return;
      const rect = this.canvas.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;

      // Close polygon if clicked near first point
      if (this.points.length >= 3) {
        const first = this.points[0];
        const dist = Math.hypot(x - first.x, y - first.y);
        if (dist < 25) {
          this.isPolygonClosed = true;
          this.generatePath();
          this.draw();
          return;
        }
      }

      if (!this.isPolygonClosed) {
        this.points.push({ x, y });
        if (this.points.length >= 3) {
          this.generatePath();
        }
        this.draw();
      }
    });
  }

  setPattern(patternName) {
    if (['boustrophedon', 'adaptive', 'spiral'].includes(patternName)) {
      this.currentPattern = patternName;
      this.stopMission();
      this.generatePath();
      this.draw();
    }
  }

  loadPreset(presetIdx) {
    this.stopMission();
    this.plantedSeedlings = [];
    const w = this.width || 600;
    const h = this.height || 400;
    const marginX = w * 0.12;
    const marginY = h * 0.15;

    if (presetIdx === 0) {
      // 1. Standard Rectangular Sawah (20x30m)
      this.points = [
        { x: marginX, y: marginY },
        { x: w - marginX, y: marginY },
        { x: w - marginX, y: h - marginY },
        { x: marginX, y: h - marginY }
      ];
    } else {
      // 2. Realistic Contour Terrace Polygon (Sawah Terasering)
      this.points = [
        { x: marginX + 35, y: marginY + 25 },
        { x: w * 0.5, y: marginY },
        { x: w - marginX - 20, y: marginY + 45 },
        { x: w - marginX + 15, y: h * 0.55 },
        { x: w - marginX - 45, y: h - marginY },
        { x: w * 0.45, y: h - marginY - 15 },
        { x: marginX, y: h - marginY + 10 },
        { x: marginX - 15, y: h * 0.45 }
      ];
    }

    this.isPolygonClosed = true;
    this.generatePath();
    this.resetRoverPosition();
    this.updateHUD();
    this.draw();
  }

  clearField() {
    this.stopMission();
    this.points = [];
    this.waypoints = [];
    this.plantedSeedlings = [];
    this.isPolygonClosed = false;
    this.rover.status = 'IDLE';
    this.updateHUD();
    this.draw();
  }

  // Point in Polygon Test with distance buffer
  isPointInside(x, y, buffer = 0) {
    if (this.points.length < 3) return false;
    let inside = false;
    for (let i = 0, j = this.points.length - 1; i < this.points.length; j = i++) {
      const xi = this.points[i].x, yi = this.points[i].y;
      const xj = this.points[j].x, yj = this.points[j].y;
      const intersect = ((yi > y) !== (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
      if (intersect) inside = !inside;
    }
    if (!inside) return false;

    if (buffer > 0) {
      for (let i = 0, j = this.points.length - 1; i < this.points.length; j = i++) {
        const p1 = this.points[j];
        const p2 = this.points[i];
        const dist = this.distToSegment({ x, y }, p1, p2);
        if (dist < buffer) return false;
      }
    }
    return true;
  }

  distToSegment(p, v, w) {
    const l2 = Math.hypot(v.x - w.x, v.y - w.y) ** 2;
    if (l2 === 0) return Math.hypot(p.x - v.x, p.y - v.y);
    let t = ((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2;
    t = Math.max(0, Math.min(1, t));
    return Math.hypot(p.x - (v.x + t * (w.x - v.x)), p.y - (v.y + t * (w.y - v.y)));
  }

  // Check if line segment stays 100% inside polygon
  isLineClear(p1, p2, samples = 18, buffer = 5) {
    for (let s = 0; s <= samples; s++) {
      const t = s / samples;
      const x = p1.x + t * (p2.x - p1.x);
      const y = p1.y + t * (p2.y - p1.y);
      if (!this.isPointInside(x, y, buffer)) {
        return false;
      }
    }
    return true;
  }

  // Safe Navigation Graph around interior vertices
  findSafeTransitPath(pStart, pEnd) {
    if (this.isLineClear(pStart, pEnd, 18, 6)) {
      return [{ x: pEnd.x, y: pEnd.y, isPlanting: false }];
    }

    const n = this.points.length;
    let area = 0;
    for (let i = 0; i < n; i++) {
      const j = (i + 1) % n;
      area += this.points[i].x * this.points[j].y - this.points[j].x * this.points[i].y;
    }
    const isCCW = area > 0;

    const innerNodes = [];
    for (let i = 0; i < n; i++) {
      const p1 = this.points[(i - 1 + n) % n];
      const p = this.points[i];
      const p2 = this.points[(i + 1) % n];

      const v1x = p.x - p1.x, v1y = p.y - p1.y;
      const v2x = p2.x - p.x, v2y = p2.y - p.y;
      const l1 = Math.hypot(v1x, v1y) || 1;
      const l2 = Math.hypot(v2x, v2y) || 1;

      let nx1 = isCCW ? -v1y / l1 : v1y / l1;
      let ny1 = isCCW ? v1x / l1 : -v1x / l1;
      let nx2 = isCCW ? -v2y / l2 : v2y / l2;
      let ny2 = isCCW ? v2x / l2 : -v2x / l2;

      let nx = (nx1 + nx2) / 2;
      let ny = (ny1 + ny2) / 2;
      const nlen = Math.hypot(nx, ny) || 1;
      nx /= nlen; ny /= nlen;

      const node = { x: p.x + nx * (this.safetyMargin + 4), y: p.y + ny * (this.safetyMargin + 4) };
      if (this.isPointInside(node.x, node.y, 6)) {
        innerNodes.push(node);
      }
    }

    const nodes = [pStart, ...innerNodes, pEnd];
    const N = nodes.length;
    const adj = Array.from({ length: N }, () => []);

    for (let i = 0; i < N; i++) {
      for (let j = i + 1; j < N; j++) {
        if (this.isLineClear(nodes[i], nodes[j], 14, 6)) {
          const d = Math.hypot(nodes[i].x - nodes[j].x, nodes[i].y - nodes[j].y);
          adj[i].push({ node: j, weight: d });
          adj[j].push({ node: i, weight: d });
        }
      }
    }

    const dist = new Array(N).fill(Infinity);
    const prev = new Array(N).fill(null);
    const visited = new Array(N).fill(false);
    dist[0] = 0;

    for (let count = 0; count < N; count++) {
      let u = -1;
      let minDist = Infinity;
      for (let i = 0; i < N; i++) {
        if (!visited[i] && dist[i] < minDist) {
          minDist = dist[i];
          u = i;
        }
      }

      if (u === -1 || u === N - 1) break;
      visited[u] = true;

      for (const edge of adj[u]) {
        if (!visited[edge.node] && dist[u] + edge.weight < dist[edge.node]) {
          dist[edge.node] = dist[u] + edge.weight;
          prev[edge.node] = u;
        }
      }
    }

    if (dist[N - 1] === Infinity) {
      return [{ x: pEnd.x, y: pEnd.y, isPlanting: false }];
    }

    const path = [];
    let curr = N - 1;
    while (curr !== null) {
      path.push(nodes[curr]);
      curr = prev[curr];
    }
    path.reverse();

    return path.slice(1).map(pt => ({ x: pt.x, y: pt.y, isPlanting: false }));
  }

  // Master Path Generator Dispatcher
  generatePath() {
    if (this.points.length < 3) return;

    if (this.currentPattern === 'spiral') {
      this.generateSpiralPath();
    } else if (this.currentPattern === 'adaptive') {
      this.generateAdaptivePath();
    } else {
      this.generateBoustrophedonPath();
    }

    this.resetRoverPosition();
  }

  // 1. Pola Boustrophedon Standar (Vertical parallel sweep)
  generateBoustrophedonPath() {
    let minX = Infinity, maxX = -Infinity;
    this.points.forEach(p => {
      minX = Math.min(minX, p.x);
      maxX = Math.max(maxX, p.x);
    });

    const rawStrips = [];
    let stripId = 0;

    for (let x = minX + this.safetyMargin + 6; x <= maxX - this.safetyMargin - 6; x += this.rowSpacing) {
      const intersections = [];
      for (let i = 0, j = this.points.length - 1; i < this.points.length; j = i++) {
        const p1 = this.points[j];
        const p2 = this.points[i];
        if ((p1.x <= x && p2.x > x) || (p2.x <= x && p1.x > x)) {
          const y = p1.y + ((x - p1.x) / (p2.x - p1.x)) * (p2.y - p1.y);
          intersections.push(y);
        }
      }

      intersections.sort((a, b) => a - b);

      for (let k = 0; k < intersections.length; k += 2) {
        if (k + 1 < intersections.length) {
          const yStart = intersections[k] + this.safetyMargin;
          const yEnd = intersections[k + 1] - this.safetyMargin;

          if (yStart < yEnd) {
            const pA = { x, y: yStart };
            const pB = { x, y: yEnd };
            if (this.isLineClear(pA, pB, 12, 6)) {
              rawStrips.push({ id: stripId++, pA, pB });
            }
          }
        }
      }
    }

    if (rawStrips.length === 0) {
      this.waypoints = [];
      return;
    }

    // Topological cell clustering
    const cells = [];
    const visitedStrips = new Set();

    for (let i = 0; i < rawStrips.length; i++) {
      if (visitedStrips.has(i)) continue;

      const cell = [rawStrips[i]];
      visitedStrips.add(i);

      let changed = true;
      while (changed) {
        changed = false;
        for (let j = 0; j < rawStrips.length; j++) {
          if (visitedStrips.has(j)) continue;
          const cand = rawStrips[j];

          for (const inCell of cell) {
            const dx = Math.abs(cand.pA.x - inCell.pA.x);
            if (dx <= this.rowSpacing * 1.35) {
              const yDist = Math.min(
                Math.abs(cand.pA.y - inCell.pA.y),
                Math.abs(cand.pB.y - inCell.pB.y),
                Math.abs(cand.pA.y - inCell.pB.y),
                Math.abs(cand.pB.y - inCell.pA.y)
              );
              if (yDist < 120 && (this.isLineClear(cand.pA, inCell.pA, 8, 5) || this.isLineClear(cand.pB, inCell.pB, 8, 5))) {
                cell.push(cand);
                visitedStrips.add(j);
                changed = true;
                break;
              }
            }
          }
        }
      }

      cell.sort((a, b) => a.pA.x - b.pA.x);
      cells.push(cell);
    }

    cells.sort((a, b) => a[0].pA.x - b[0].pA.x);

    const waypoints = [];
    let currentPos = cells[0][0].pA;
    waypoints.push({ x: currentPos.x, y: currentPos.y, isPlanting: false });

    for (const cell of cells) {
      let isTopToBottom = true;

      const distToTop = Math.hypot(currentPos.x - cell[0].pA.x, currentPos.y - cell[0].pA.y);
      const distToBot = Math.hypot(currentPos.x - cell[0].pB.x, currentPos.y - cell[0].pB.y);

      let targetEntry = distToTop <= distToBot ? cell[0].pA : cell[0].pB;
      isTopToBottom = (targetEntry === cell[0].pA);

      if (Math.hypot(currentPos.x - targetEntry.x, currentPos.y - targetEntry.y) > 6) {
        const transits = this.findSafeTransitPath(currentPos, targetEntry);
        transits.forEach(wp => waypoints.push(wp));
      }

      for (let s = 0; s < cell.length; s++) {
        const strip = cell[s];
        const startPt = isTopToBottom ? strip.pA : strip.pB;
        const endPt = isTopToBottom ? strip.pB : strip.pA;

        if (Math.hypot(currentPos.x - startPt.x, currentPos.y - startPt.y) > 4) {
          if (this.isLineClear(currentPos, startPt, 10, 5)) {
            waypoints.push({ x: startPt.x, y: startPt.y, isPlanting: false });
          } else {
            const safeTransits = this.findSafeTransitPath(currentPos, startPt);
            safeTransits.forEach(wp => waypoints.push(wp));
          }
        }

        waypoints.push({ x: startPt.x, y: startPt.y, isPlanting: true });
        waypoints.push({ x: endPt.x, y: endPt.y, isPlanting: true });

        currentPos = endPt;
        isTopToBottom = !isTopToBottom;
      }
    }

    this.waypoints = waypoints;
  }

  // 2. Pola Adaptif Kontur (Diagonal contour sweep)
  generateAdaptivePath() {
    let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
    this.points.forEach(p => {
      minX = Math.min(minX, p.x);
      maxX = Math.max(maxX, p.x);
      minY = Math.min(minY, p.y);
      maxY = Math.max(maxY, p.y);
    });

    const angle = Math.PI / 6; // 30 deg slope
    const cosA = Math.cos(angle);
    const sinA = Math.sin(angle);
    const perpX = -sinA;
    const perpY = cosA;

    // Transform points to projection along perpendicular axis
    const projections = this.points.map(p => p.x * perpX + p.y * perpY);
    const minProj = Math.min(...projections);
    const maxProj = Math.max(...projections);

    const rawStrips = [];
    let stripId = 0;

    for (let proj = minProj + this.safetyMargin + 6; proj <= maxProj - this.safetyMargin - 6; proj += this.rowSpacing) {
      // Find intersections of line: x * perpX + y * perpY = proj with polygon edges
      const intersections = [];
      const n = this.points.length;

      for (let i = 0; i < n; i++) {
        const p1 = this.points[i];
        const p2 = this.points[(i + 1) % n];

        const pr1 = p1.x * perpX + p1.y * perpY;
        const pr2 = p2.x * perpX + p2.y * perpY;

        if ((pr1 <= proj && pr2 > proj) || (pr2 <= proj && pr1 > proj)) {
          const t = (proj - pr1) / (pr2 - pr1);
          const ix = p1.x + t * (p2.x - p1.x);
          const iy = p1.y + t * (p2.y - p1.y);
          const along = ix * cosA + iy * sinA;
          intersections.push({ x: ix, y: iy, along });
        }
      }

      intersections.sort((a, b) => a.along - b.along);

      for (let k = 0; k < intersections.length; k += 2) {
        if (k + 1 < intersections.length) {
          const i1 = intersections[k];
          const i2 = intersections[k + 1];

          // Inset endpoints along segment
          const dx = i2.x - i1.x;
          const dy = i2.y - i1.y;
          const len = Math.hypot(dx, dy);

          if (len > this.safetyMargin * 2) {
            const ux = dx / len;
            const uy = dy / len;
            const pA = { x: i1.x + ux * this.safetyMargin, y: i1.y + uy * this.safetyMargin };
            const pB = { x: i2.x - ux * this.safetyMargin, y: i2.y - uy * this.safetyMargin };

            if (this.isLineClear(pA, pB, 12, 6)) {
              rawStrips.push({ id: stripId++, pA, pB, proj });
            }
          }
        }
      }
    }

    if (rawStrips.length === 0) {
      this.generateBoustrophedonPath();
      return;
    }

    const waypoints = [];
    let currentPos = rawStrips[0].pA;
    waypoints.push({ x: currentPos.x, y: currentPos.y, isPlanting: false });

    let isForward = true;
    for (let s = 0; s < rawStrips.length; s++) {
      const strip = rawStrips[s];
      const startPt = isForward ? strip.pA : strip.pB;
      const endPt = isForward ? strip.pB : strip.pA;

      if (Math.hypot(currentPos.x - startPt.x, currentPos.y - startPt.y) > 4) {
        if (this.isLineClear(currentPos, startPt, 10, 5)) {
          waypoints.push({ x: startPt.x, y: startPt.y, isPlanting: false });
        } else {
          const transits = this.findSafeTransitPath(currentPos, startPt);
          transits.forEach(wp => waypoints.push(wp));
        }
      }

      waypoints.push({ x: startPt.x, y: startPt.y, isPlanting: true });
      waypoints.push({ x: endPt.x, y: endPt.y, isPlanting: true });

      currentPos = endPt;
      isForward = !isForward;
    }

    this.waypoints = waypoints;
  }

  // 3. Pola Spiral Obat Nyamuk (Concentric outward spiral)
  generateSpiralPath() {
    let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
    this.points.forEach(p => {
      minX = Math.min(minX, p.x);
      maxX = Math.max(maxX, p.x);
      minY = Math.min(minY, p.y);
      maxY = Math.max(maxY, p.y);
    });

    const cx = (minX + maxX) / 2;
    const cy = (minY + maxY) / 2;

    const wSpan = maxX - minX;
    const hSpan = maxY - minY;
    const maxRadius = Math.min(wSpan, hSpan) / 2 - this.safetyMargin;

    const step = this.rowSpacing;
    const numRings = Math.max(2, Math.floor(maxRadius / step));

    const waypoints = [];
    let lastPt = { x: cx, y: cy };
    waypoints.push({ x: cx, y: cy, isPlanting: false });

    for (let i = 1; i <= numRings; i++) {
      const rx = i * step * (wSpan / (hSpan || 1));
      const ry = i * step;

      const ringCorners = [
        { x: cx + rx, y: lastPt.y },
        { x: cx + rx, y: cy - ry },
        { x: cx - rx, y: cy - ry },
        { x: cx - rx, y: cy + ry },
        { x: cx + rx, y: cy + ry }
      ];

      for (const corner of ringCorners) {
        // Clamp and verify within polygon
        if (this.isPointInside(corner.x, corner.y, 6)) {
          if (this.isLineClear(lastPt, corner, 8, 4)) {
            waypoints.push({ x: corner.x, y: corner.y, isPlanting: true });
            lastPt = corner;
          }
        }
      }
    }

    this.waypoints = waypoints.length > 2 ? waypoints : [];
    if (this.waypoints.length === 0) {
      this.generateBoustrophedonPath();
    }
  }

  resetRoverPosition() {
    if (this.waypoints.length > 0) {
      this.rover.x = this.waypoints[0].x;
      this.rover.y = this.waypoints[0].y;
      this.rover.targetIdx = 1;
      this.rover.lastPlantPos = { x: this.rover.x, y: this.rover.y };
      this.rover.distanceTraveled = 0;
      this.rover.heading = Math.PI / 2;
      this.rover.isDispenserActive = false;
    }
  }

  startMission() {
    if (this.waypoints.length === 0) return;
    if (this.rover.status === 'COMPLETED' || this.rover.status === 'ESTOP') {
      this.resetRoverPosition();
      this.plantedSeedlings = [];
    }
    this.rover.status = 'RUNNING';
    this.updateHUD();
  }

  pauseMission() {
    if (this.rover.status === 'RUNNING') {
      this.rover.status = 'PAUSED';
      this.updateHUD();
    }
  }

  emergencyStop() {
    this.rover.status = 'ESTOP';
    this.updateHUD();
  }

  stopMission() {
    this.rover.status = 'IDLE';
    this.resetRoverPosition();
    this.plantedSeedlings = [];
    this.updateHUD();
  }

  setSpeedMultiplier(multiplier) {
    this.speedMultiplier = multiplier;
  }

  updateRover() {
    if (this.rover.status !== 'RUNNING') return;
    if (this.rover.targetIdx >= this.waypoints.length) {
      this.rover.status = 'COMPLETED';
      this.rover.isDispenserActive = false;
      this.updateHUD();
      return;
    }

    const target = this.waypoints[this.rover.targetIdx];
    const dx = target.x - this.rover.x;
    const dy = target.y - this.rover.y;
    const dist = Math.hypot(dx, dy);

    this.rover.heading = Math.atan2(dy, dx);
    const step = this.rover.speed * this.speedMultiplier;

    if (dist <= step) {
      this.rover.x = target.x;
      this.rover.y = target.y;
      this.rover.targetIdx++;
    } else {
      this.rover.x += (dx / dist) * step;
      this.rover.y += (dy / dist) * step;
    }

    this.rover.distanceTraveled += step;

    // Safety: ONLY plant seedlings on active planting segments strictly inside boundary
    const isPlantingSegment = target.isPlanting === true;
    const isSafeInside = this.isPointInside(this.rover.x, this.rover.y, 8);
    
    this.rover.isDispenserActive = isPlantingSegment && isSafeInside;

    if (this.rover.isDispenserActive) {
      const distSinceLastPlant = Math.hypot(
        this.rover.x - this.rover.lastPlantPos.x,
        this.rover.y - this.rover.lastPlantPos.y
      );

      if (distSinceLastPlant >= this.plantSpacing) {
        this.plantedSeedlings.push({ x: this.rover.x, y: this.rover.y });
        this.rover.lastPlantPos = { x: this.rover.x, y: this.rover.y };
        this.updateHUD();
      }
    }
  }

  updateHUD() {
    const statusEl = document.getElementById('simStatusBadge');
    const seedlingsCountEl = document.getElementById('simSeedlingCount');
    const progressEl = document.getElementById('simProgressPct');
    const areaEl = document.getElementById('simAreaCovered');
    const coordsEl = document.getElementById('simRoverCoords');
    const speedEl = document.getElementById('simRoverSpeed');

    if (statusEl) {
      statusEl.textContent = this.rover.status;
      if (this.rover.status === 'RUNNING') {
        statusEl.className = 'px-2.5 py-1 rounded text-xs font-mono bg-emerald-100 text-emerald-700 border border-emerald-300 font-semibold animate-pulse';
      } else if (this.rover.status === 'ESTOP') {
        statusEl.className = 'px-2.5 py-1 rounded text-xs font-mono bg-red-100 text-red-700 border border-red-300 font-bold';
      } else if (this.rover.status === 'COMPLETED') {
        statusEl.className = 'px-2.5 py-1 rounded text-xs font-mono bg-cyan-100 text-cyan-700 border border-cyan-300 font-semibold';
      } else {
        statusEl.className = 'px-2.5 py-1 rounded text-xs font-mono bg-slate-100 text-slate-600 border border-slate-300';
      }
    }

    if (seedlingsCountEl) seedlingsCountEl.textContent = this.plantedSeedlings.length.toLocaleString('id-ID');
    
    const pct = this.waypoints.length > 0 ? Math.min(100, Math.round((this.rover.targetIdx / this.waypoints.length) * 100)) : 0;
    if (progressEl) progressEl.textContent = `${pct}%`;
    
    const areaM2 = (this.plantedSeedlings.length * 0.04).toFixed(1);
    if (areaEl) areaEl.textContent = `${areaM2} m²`;

    if (coordsEl) {
      const lat = (-6.914744 + (this.rover.y * 0.000005)).toFixed(6);
      const lon = (107.609810 + (this.rover.x * 0.000005)).toFixed(6);
      coordsEl.textContent = `${lat}, ${lon}`;
    }

    if (speedEl) {
      const speedKmH = this.rover.status === 'RUNNING' ? (1.4 * this.speedMultiplier).toFixed(1) : '0.0';
      speedEl.textContent = `${speedKmH} km/h`;
    }
  }

  draw() {
    this.ctx.clearRect(0, 0, this.width, this.height);

    // Fresh soft background
    this.ctx.fillStyle = '#F8FCF9';
    this.ctx.fillRect(0, 0, this.width, this.height);

    // 1. Background Grid
    this.drawBackgroundGrid();

    // 2. Outer Field Boundary (Solid Green Line)
    if (this.points.length > 0) {
      this.ctx.beginPath();
      this.ctx.moveTo(this.points[0].x, this.points[0].y);
      for (let i = 1; i < this.points.length; i++) {
        this.ctx.lineTo(this.points[i].x, this.points[i].y);
      }
      if (this.isPolygonClosed) {
        this.ctx.closePath();
        this.ctx.fillStyle = 'rgba(16, 185, 129, 0.10)';
        this.ctx.fill();
      }

      this.ctx.strokeStyle = '#059669'; // Solid green boundary
      this.ctx.lineWidth = 2.5;
      this.ctx.stroke();

      // Draw Boundary Vertices
      this.points.forEach((p, idx) => {
        this.ctx.beginPath();
        this.ctx.arc(p.x, p.y, 6, 0, Math.PI * 2);
        this.ctx.fillStyle = idx === 0 ? '#0284C7' : '#10B981';
        this.ctx.fill();
        this.ctx.strokeStyle = '#FFFFFF';
        this.ctx.lineWidth = 2;
        this.ctx.stroke();

        this.ctx.fillStyle = '#1E293B';
        this.ctx.font = 'bold 11px JetBrains Mono';
        this.ctx.fillText(`P${idx + 1}`, p.x + 8, p.y - 6);
      });
    }

    // 3. Draw Unified Dotted Blue Path (Strictly Inside Solid Green Boundary)
    if (this.waypoints.length > 1) {
      this.ctx.beginPath();
      this.ctx.moveTo(this.waypoints[0].x, this.waypoints[0].y);
      for (let i = 1; i < this.waypoints.length; i++) {
        const wp1 = this.waypoints[i - 1];
        const wp2 = this.waypoints[i];
        if (this.isLineClear(wp1, wp2, 8, 2)) {
          this.ctx.lineTo(wp2.x, wp2.y);
        } else {
          this.ctx.moveTo(wp2.x, wp2.y);
        }
      }
      this.ctx.strokeStyle = '#0284C7'; // Blue/Cyan dotted line
      this.ctx.lineWidth = 2.0;
      this.ctx.setLineDash([3, 4]);
      this.ctx.stroke();
      this.ctx.setLineDash([]);
    }

    // 4. Planted Rice Seedlings (Vibrant Green Dots strictly along the path)
    this.plantedSeedlings.forEach(s => {
      this.ctx.beginPath();
      this.ctx.arc(s.x, s.y, 3, 0, Math.PI * 2);
      this.ctx.fillStyle = '#16A34A';
      this.ctx.fill();
    });

    // 5. Autonomous PadiBot Rover following the blue path
    if (this.rover.status !== 'IDLE' || this.waypoints.length > 0) {
      this.drawRover(this.rover.x, this.rover.y, this.rover.heading);
    }
  }

  drawBackgroundGrid() {
    this.ctx.strokeStyle = 'rgba(16, 185, 129, 0.08)';
    this.ctx.lineWidth = 1;
    const step = 30;
    for (let x = 0; x < this.width; x += step) {
      this.ctx.beginPath();
      this.ctx.moveTo(x, 0);
      this.ctx.lineTo(x, this.height);
      this.ctx.stroke();
    }
    for (let y = 0; y < this.height; y += step) {
      this.ctx.beginPath();
      this.ctx.moveTo(0, y);
      this.ctx.lineTo(this.width, y);
      this.ctx.stroke();
    }
  }

  drawRover(x, y, heading) {
    this.ctx.save();
    this.ctx.translate(x, y);
    this.ctx.rotate(heading);

    // Active LiDAR beam (front)
    if (this.rover.status === 'RUNNING') {
      const gradient = this.ctx.createRadialGradient(0, 0, 5, 25, 0, 48);
      gradient.addColorStop(0, 'rgba(2, 132, 199, 0.45)');
      gradient.addColorStop(1, 'rgba(2, 132, 199, 0)');
      this.ctx.beginPath();
      this.ctx.moveTo(0, 0);
      this.ctx.arc(0, 0, 48, -Math.PI / 4, Math.PI / 4);
      this.ctx.closePath();
      this.ctx.fillStyle = gradient;
      this.ctx.fill();
    }

    // Rover Chassis
    this.ctx.fillStyle = '#FFFFFF';
    this.ctx.strokeStyle = '#059669';
    this.ctx.lineWidth = 2;
    this.ctx.fillRect(-13, -10, 26, 20);
    this.ctx.strokeRect(-13, -10, 26, 20);

    // Tracks
    this.ctx.fillStyle = '#334155';
    this.ctx.fillRect(-15, -14, 30, 4);
    this.ctx.fillRect(-15, 10, 30, 4);

    // Status LED Beacon (Green = Active, Red = E-STOP)
    this.ctx.beginPath();
    this.ctx.arc(0, 0, 4, 0, Math.PI * 2);
    this.ctx.fillStyle = this.rover.status === 'ESTOP' ? '#EF4444' : '#10B981';
    this.ctx.fill();

    // Planting arm mechanism (back)
    this.ctx.strokeStyle = this.rover.isDispenserActive ? '#15803D' : '#94A3B8';
    this.ctx.lineWidth = 2.5;
    this.ctx.beginPath();
    this.ctx.moveTo(-13, 0);
    this.ctx.lineTo(-19, 0);
    this.ctx.stroke();

    this.ctx.restore();
  }

  startLoop() {
    const loop = () => {
      this.updateRover();
      this.draw();
      this.animationFrameId = requestAnimationFrame(loop);
    };
    loop();
  }
}

window.FieldSimulator = FieldSimulator;
