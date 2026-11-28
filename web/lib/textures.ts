"use client";

import * as THREE from "three";

// Generated "portrait" textures for the landing hero: abstract warm shapes, no real people's faces.
// (Using photos of real-looking people we don't have rights to is something the design skill forbids.)
const PALETTES = [
  ["#f28c28", "#7a2f4f", "#f6c6a8"],
  ["#f3d9c6", "#2b2147", "#ffd2c2"],
  ["#f6b26b", "#3a1830", "#ffe0b8"],
  ["#6cb8ff", "#16263f", "#ffd9e4"],
  ["#ffb36b", "#3b2414", "#c9b8ff"],
  ["#4fd69c", "#12302a", "#ffe6c9"],
];

export function portraitTexture(i: number, w = 320, h = 448): THREE.CanvasTexture {
  const [a, b, c] = PALETTES[i % PALETTES.length];
  const canvas = document.createElement("canvas");
  canvas.width = w;
  canvas.height = h;
  const g = canvas.getContext("2d")!;
  const bg = g.createLinearGradient(0, 0, w, h);
  bg.addColorStop(0, b);
  bg.addColorStop(1, a);
  g.fillStyle = bg;
  g.fillRect(0, 0, w, h);
  // soft light from the top
  const light = g.createRadialGradient(w * 0.3, h * 0.15, 10, w * 0.3, h * 0.15, w);
  light.addColorStop(0, "rgba(255,255,255,0.35)");
  light.addColorStop(1, "rgba(255,255,255,0)");
  g.fillStyle = light;
  g.fillRect(0, 0, w, h);
  // head + shoulders silhouette
  g.fillStyle = c;
  g.globalAlpha = 0.9;
  g.beginPath();
  g.ellipse(w / 2, h * 0.42, w * 0.17, w * 0.2, 0, 0, Math.PI * 2);
  g.fill();
  g.beginPath();
  g.ellipse(w / 2, h * 0.95, w * 0.42, h * 0.28, 0, Math.PI, 0);
  g.fill();
  g.globalAlpha = 1;
  // bottom scrim + name bar, like a real card
  const scrim = g.createLinearGradient(0, h * 0.6, 0, h);
  scrim.addColorStop(0, "rgba(0,0,0,0)");
  scrim.addColorStop(1, "rgba(0,0,0,0.55)");
  g.fillStyle = scrim;
  g.fillRect(0, 0, w, h);
  g.fillStyle = "rgba(255,255,255,0.9)";
  g.fillRect(w * 0.1, h * 0.86, w * 0.38, 10);
  g.fillStyle = "rgba(255,255,255,0.5)";
  g.fillRect(w * 0.1, h * 0.9, w * 0.22, 7);
  const tex = new THREE.CanvasTexture(canvas);
  tex.colorSpace = THREE.SRGBColorSpace;
  tex.anisotropy = 4;
  return tex;
}
