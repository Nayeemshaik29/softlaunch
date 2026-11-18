"use client";
/* eslint-disable react-hooks/purity, react-hooks/immutability --
   three.js scene: particle seeds use Math.random once (inside useMemo), and buffers/materials are mutated every
   frame in useFrame, outside React's render. That is how R3F is meant to be used. */

import { Environment, Lightformer } from "@react-three/drei";
import { Canvas, useFrame, useLoader, useThree } from "@react-three/fiber";
import { Bloom, EffectComposer } from "@react-three/postprocessing";
import { Suspense, useEffect, useMemo, useRef } from "react";
import * as THREE from "three";
import { portraitTexture } from "@/lib/textures";
import type { palettes } from "@/lib/mood";

type Palette = (typeof palettes)[keyof typeof palettes];

// Timeline (seconds)
export const IMPACT = 0.9; // cards collide
export const SETTLED = 1.9; // side by side, slow orbit from here

const W = 1.1;
const H = 1.5;

function rounded(w: number, h: number, r: number) {
  const s = new THREE.Shape();
  const x = -w / 2, y = -h / 2;
  s.moveTo(x + r, y); s.lineTo(x + w - r, y); s.quadraticCurveTo(x + w, y, x + w, y + r);
  s.lineTo(x + w, y + h - r); s.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
  s.lineTo(x + r, y + h); s.quadraticCurveTo(x, y + h, x, y + h - r);
  s.lineTo(x, y + r); s.quadraticCurveTo(x, y, x + r, y);
  const g = new THREE.ShapeGeometry(s, 8);
  const p = g.attributes.position;
  const uv = new Float32Array(p.count * 2);
  for (let i = 0; i < p.count; i++) { uv[i * 2] = (p.getX(i) - x) / w; uv[i * 2 + 1] = (p.getY(i) - y) / h; }
  g.setAttribute("uv", new THREE.BufferAttribute(uv, 2));
  return g;
}

/** "object-fit: cover" for a texture on a W×H card. */
function cover(t: THREE.Texture) {
  const img = t.image as { width: number; height: number } | undefined;
  if (!img?.width) return t;
  const imgAspect = img.width / img.height;
  const cardAspect = W / H;
  t.wrapS = t.wrapT = THREE.ClampToEdgeWrapping;
  if (imgAspect > cardAspect) { t.repeat.set(cardAspect / imgAspect, 1); t.offset.set((1 - t.repeat.x) / 2, 0); }
  else { t.repeat.set(1, imgAspect / cardAspect); t.offset.set(0, (1 - t.repeat.y) / 2); }
  t.colorSpace = THREE.SRGBColorSpace;
  return t;
}

function usePhoto(url: string | null, fallbackIndex: number) {
  // photos come from our storage with CORS (bucket rule), so WebGL may use them as textures
  const loaded = useLoader(THREE.TextureLoader, url ?? "data:image/gif;base64,R0lGODlhAQABAAAAACw=");
  return useMemo(() => (url ? cover(loaded) : portraitTexture(fallbackIndex)), [url, loaded, fallbackIndex]);
}

const ease = (t: number) => t * t * t; // accelerate into the hit
const clamp01 = (t: number) => Math.min(1, Math.max(0, t));

function GlassCard({ side, photo, time }: { side: -1 | 1; photo: THREE.Texture; time: React.MutableRefObject<number> }) {
  const g = useRef<THREE.Group>(null);
  const geo = useMemo(() => rounded(W, H, 0.1), []);
  const glassGeo = useMemo(() => rounded(W + 0.08, H + 0.08, 0.13), []);
  useFrame(() => {
    const t = time.current;
    if (!g.current) return;
    let x: number;
    if (t < IMPACT) {
      x = side * (4.2 - (4.2 - W / 2) * ease(t / IMPACT)); // slide in from the side, faster and faster
    } else {
      // bounce apart a little and settle (damped spring)
      const k = t - IMPACT;
      const rest = W / 2 + 0.16;
      x = side * (rest - (rest - W / 2) * Math.exp(-5 * k) * Math.cos(9 * k));
    }
    g.current.position.set(x, 0, 0);
    const settle = clamp01((t - IMPACT) / 1);
    g.current.rotation.y = side * (-0.5 * (1 - clamp01(t / IMPACT)) + 0.12 * settle);
    g.current.rotation.z = side * -0.04 * settle;
  });
  return (
    <group ref={g}>
      <mesh geometry={geo}>
        <meshBasicMaterial map={photo} toneMapped={false} />
      </mesh>
      {/* glass pane in front: catches the light, slightly tinted */}
      <mesh geometry={glassGeo} position={[0, 0, 0.04]}>
        <meshPhysicalMaterial transparent opacity={0.28} roughness={0.08} metalness={0} clearcoat={1} clearcoatRoughness={0.05} color="#ffffff" envMapIntensity={1.4} />
      </mesh>
    </group>
  );
}

function Shockwave({ time, color }: { time: React.MutableRefObject<number>; color: string }) {
  const m = useRef<THREE.Mesh>(null);
  const mat = useRef<THREE.MeshBasicMaterial>(null);
  useFrame(() => {
    const k = (time.current - IMPACT) / 0.9;
    if (!m.current || !mat.current) return;
    m.current.visible = k >= 0 && k <= 1;
    const e = 1 - Math.pow(1 - clamp01(k), 3);
    m.current.scale.setScalar(0.2 + e * 5.5);
    mat.current.opacity = (1 - clamp01(k)) * 0.9;
  });
  return (
    <mesh ref={m} position={[0, 0, -0.1]} visible={false}>
      <ringGeometry args={[0.48, 0.5, 96]} />
      <meshBasicMaterial ref={mat} color={color} transparent toneMapped={false} depthWrite={false} blending={THREE.AdditiveBlending} />
    </mesh>
  );
}

function Burst({ time, from, to }: { time: React.MutableRefObject<number>; from: string; to: string }) {
  const N = 260;
  const { dirs, colors } = useMemo(() => {
    const dirs = new Float32Array(N * 3);
    const colors = new Float32Array(N * 3);
    const a = new THREE.Color(from), b = new THREE.Color(to);
    for (let i = 0; i < N; i++) {
      const v = new THREE.Vector3().randomDirection().multiply(new THREE.Vector3(1, 0.8, 0.5)).multiplyScalar(1.2 + Math.random() * 2.2);
      dirs.set([v.x, v.y, v.z], i * 3);
      const c = a.clone().lerp(b, Math.random());
      colors.set([c.r, c.g, c.b], i * 3);
    }
    return { dirs, colors };
  }, [from, to]);
  const pos = useMemo(() => new Float32Array(N * 3), []);
  const geo = useRef<THREE.BufferGeometry>(null);
  const mat = useRef<THREE.PointsMaterial>(null);
  useFrame(() => {
    const k = time.current - IMPACT;
    if (!geo.current || !mat.current) return;
    if (k < 0 || k > 1.6) { mat.current.opacity = 0; return; }
    const travel = 1 - Math.exp(-3.2 * k); // fast out, slows down (drag)
    for (let i = 0; i < N * 3; i++) pos[i] = dirs[i] * travel;
    geo.current.attributes.position.needsUpdate = true;
    mat.current.opacity = 1 - k / 1.6;
  });
  return (
    <points>
      <bufferGeometry ref={geo}>
        <bufferAttribute attach="attributes-position" args={[pos, 3]} />
        <bufferAttribute attach="attributes-color" args={[colors, 3]} />
      </bufferGeometry>
      <pointsMaterial ref={mat} size={0.05} vertexColors transparent depthWrite={false} blending={THREE.AdditiveBlending} toneMapped={false} />
    </points>
  );
}

function Rig({ time, startAt, onStart }: { time: React.MutableRefObject<number>; startAt: React.MutableRefObject<number | null>; onStart: () => void }) {
  const { camera, clock } = useThree();
  useFrame(() => {
    if (startAt.current === null) {
      startAt.current = clock.elapsedTime; // first frame AFTER the photos loaded (we're inside Suspense)
      onStart();
    }
    time.current = clock.elapsedTime - startAt.current;
    const orbit = Math.max(0, time.current - SETTLED);
    const a = Math.sin(orbit * 0.25) * 0.45; // slow orbit, back and forth
    camera.position.set(Math.sin(a) * 4.2, 0.25 + Math.sin(orbit * 0.3) * 0.15, Math.cos(a) * 4.2);
    camera.lookAt(0, 0, 0);
  });
  return null;
}

function Cards({ mine, theirs, time }: { mine: string | null; theirs: string | null; time: React.MutableRefObject<number> }) {
  const a = usePhoto(mine, 1);
  const b = usePhoto(theirs, 0);
  return (
    <>
      <GlassCard side={-1} photo={a} time={time} />
      <GlassCard side={1} photo={b} time={time} />
    </>
  );
}

export default function MatchScene({
  mine, theirs, skipRef, onStart, palette,
}: { mine: string | null; theirs: string | null; skipRef: React.MutableRefObject<() => void>; onStart: () => void; palette: Palette }) {
  const time = useRef(0);
  const startAt = useRef<number | null>(null);
  // "Skip": jump the timeline to the settled pose
  useEffect(() => {
    skipRef.current = () => { if (startAt.current !== null) startAt.current -= Math.max(0, SETTLED - time.current); };
  }, [skipRef]);
  return (
    <Canvas dpr={[1, 1.5]} camera={{ position: [0, 0.25, 4.2], fov: 40 }} gl={{ alpha: true, antialias: true }}>
      {/* solid backdrop = the page colour: the bloom pass makes the canvas opaque, so match it exactly */}
      <color attach="background" args={[palette.bg]} />
      <Environment resolution={64}>
        <Lightformer intensity={2} position={[0, 2, 3]} scale={[6, 1, 1]} color="#ffd9c8" />
        <Lightformer intensity={1.2} position={[-3, 0, 2]} scale={[1, 4, 1]} color={palette.c} />
        <Lightformer intensity={1.2} position={[3, 0, 2]} scale={[1, 4, 1]} color={palette.a} />
      </Environment>
      <Suspense fallback={null}>
        <Cards mine={mine} theirs={theirs} time={time} />
        <Rig time={time} startAt={startAt} onStart={onStart} />
        <Shockwave time={time} color={palette.glow} />
        <Burst time={time} from={palette.a} to={palette.c} />
      </Suspense>
      <EffectComposer multisampling={0}>
        <Bloom intensity={0.9} luminanceThreshold={0.55} mipmapBlur />
      </EffectComposer>
    </Canvas>
  );
}
