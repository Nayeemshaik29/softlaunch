"use client";

import { OrbitControls } from "@react-three/drei";
import { Canvas, useFrame } from "@react-three/fiber";
import { Bloom, EffectComposer } from "@react-three/postprocessing";
import { useMemo, useRef, useState } from "react";
import * as THREE from "three";
import { DESIRES, type Level } from "@/lib/desires";
import type { palettes } from "@/lib/mood";

type Palette = (typeof palettes)[keyof typeof palettes];

/** Even spread of n points on a sphere (Fibonacci lattice). */
function sphere(n: number, r: number) {
  const pts: THREE.Vector3[] = [];
  const golden = Math.PI * (3 - Math.sqrt(5));
  for (let i = 0; i < n; i++) {
    const y = 1 - (i / (n - 1)) * 2;
    const rad = Math.sqrt(1 - y * y);
    const t = golden * i;
    pts.push(new THREE.Vector3(Math.cos(t) * rad * r, y * r * 0.82, Math.sin(t) * rad * r));
  }
  return pts;
}

/** Text drawn into a small canvas texture: crisp, cheap, and lives inside the 3D scene (no DOM per label). */
function Label({ text, level }: { text: string; level: Level }) {
  const { texture, aspect } = useMemo(() => {
    const scale = 2;
    const c = document.createElement("canvas");
    const g = c.getContext("2d")!;
    const font = `600 ${22 * scale}px ui-sans-serif, system-ui, sans-serif`;
    g.font = font;
    const w = Math.ceil(g.measureText(text).width) + 28 * scale;
    const h = 40 * scale;
    c.width = w;
    c.height = h;
    g.font = font;
    g.fillStyle = "rgba(0,0,0,0.42)";
    g.beginPath();
    g.roundRect(0, 0, w, h, h / 2);
    g.fill();
    g.fillStyle = level === "notInto" ? "rgba(255,255,255,0.45)" : "#ffffff";
    g.textBaseline = "middle";
    g.textAlign = "center";
    g.fillText(text, w / 2, h / 2 + 1);
    if (level === "notInto") {
      g.strokeStyle = "rgba(255,255,255,0.6)";
      g.lineWidth = 2 * scale;
      g.beginPath();
      g.moveTo(14 * scale, h / 2);
      g.lineTo(w - 14 * scale, h / 2);
      g.stroke();
    }
    const t = new THREE.CanvasTexture(c);
    t.colorSpace = THREE.SRGBColorSpace;
    return { texture: t, aspect: w / h };
  }, [text, level]);
  const height = 0.28;
  return (
    <sprite position={[0, -1.7, 0]} scale={[height * aspect / 0.3, height / 0.3, 1]}>
      <spriteMaterial map={texture} transparent depthWrite={false} toneMapped={false} />
    </sprite>
  );
}

function Orb({ home, label, level, palette, onToggle }: {
  home: THREE.Vector3; label: string; level: Level; palette: Palette; onToggle: () => void;
}) {
  const mesh = useRef<THREE.Mesh>(null);
  const mat = useRef<THREE.MeshStandardMaterial>(null);
  const [hover, setHover] = useState(false);
  const target = useMemo(() => {
    // "into" floats toward you, "not into" sinks back
    const k = level === "into" ? 1.18 : level === "notInto" ? 0.78 : 1;
    return home.clone().multiplyScalar(k);
  }, [home, level]);
  const color = level === "into" ? palette.a : level === "openTo" ? palette.c : level === "notInto" ? "#3d2a33" : "#8f7f88";

  useFrame(({ clock }, dt) => {
    const m = mesh.current;
    if (!m || !mat.current) return;
    m.position.lerp(target, Math.min(1, dt * 4));
    const breathe = level === "into" ? 1 + Math.sin(clock.elapsedTime * 2 + home.x) * 0.06 : 1;
    const size = (level === "into" ? 0.31 : level === "openTo" ? 0.28 : level === "notInto" ? 0.2 : 0.26) * breathe * (hover ? 1.12 : 1);
    m.scale.setScalar(THREE.MathUtils.lerp(m.scale.x, size, Math.min(1, dt * 6)));
    mat.current.color.lerp(new THREE.Color(color), Math.min(1, dt * 5));
    mat.current.emissive.lerp(new THREE.Color(level === "none" || level === "notInto" ? "#000000" : color), Math.min(1, dt * 5));
    mat.current.emissiveIntensity = level === "into" ? 0.75 : level === "openTo" ? 0.55 : 0;
    mat.current.opacity = level === "notInto" ? 0.35 : 1;
  });

  return (
    <mesh
      ref={mesh}
      position={home}
      onClick={(e) => { e.stopPropagation(); onToggle(); }}
      onPointerOver={(e) => { e.stopPropagation(); setHover(true); document.body.style.cursor = "pointer"; }}
      onPointerOut={() => { setHover(false); document.body.style.cursor = ""; }}
    >
      <sphereGeometry args={[1, 32, 32]} />
      <meshStandardMaterial ref={mat} transparent roughness={0.35} metalness={0.1} toneMapped={false} />
      <Label text={label} level={level} />
    </mesh>
  );
}

export default function DesireScene({ levels, palette, onToggle }: {
  levels: Record<string, Level>; palette: Palette; onToggle: (id: string) => void;
}) {
  const points = useMemo(() => sphere(DESIRES.length, 2.6), []);
  return (
    <Canvas dpr={[1, 1.5]} camera={{ position: [0, 0, 6.4], fov: 45 }} gl={{ alpha: true, antialias: true }}>
      <ambientLight intensity={0.45} />
      <pointLight position={[3, 4, 5]} intensity={40} color="#ffd9c8" />
      <pointLight position={[-4, -2, 3]} intensity={20} color={palette.c} />
      {DESIRES.map((d, i) => (
        <Orb key={d.id} home={points[i]} label={d.label} level={levels[d.id] ?? "none"} palette={palette} onToggle={() => onToggle(d.id)} />
      ))}
      <OrbitControls enableZoom={false} enablePan={false} autoRotate autoRotateSpeed={0.6} rotateSpeed={0.5} />
      <EffectComposer multisampling={0}>
        <Bloom intensity={0.45} luminanceThreshold={0.55} mipmapBlur />
      </EffectComposer>
    </Canvas>
  );
}
