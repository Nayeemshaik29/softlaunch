"use client";
/* eslint-disable react-hooks/purity, react-hooks/immutability --
   three.js scene: particle seeds use Math.random once (inside useMemo), and buffers/materials are mutated every
   frame in useFrame, outside React's render. That is how R3F is meant to be used. */

import { Canvas, useFrame, useThree } from "@react-three/fiber";
import { Bloom, DepthOfField, EffectComposer } from "@react-three/postprocessing";
import { useEffect, useMemo, useRef, useState } from "react";
import * as THREE from "three";
import { portraitTexture } from "@/lib/textures";

/* ------------------------------------------------------------------ helpers */

/** Rounded-rectangle plane with 0..1 UVs, so a texture fills it like a photo. */
function roundedCardGeometry(w: number, h: number, r: number) {
  const s = new THREE.Shape();
  const x = -w / 2;
  const y = -h / 2;
  s.moveTo(x + r, y);
  s.lineTo(x + w - r, y);
  s.quadraticCurveTo(x + w, y, x + w, y + r);
  s.lineTo(x + w, y + h - r);
  s.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
  s.lineTo(x + r, y + h);
  s.quadraticCurveTo(x, y + h, x, y + h - r);
  s.lineTo(x, y + r);
  s.quadraticCurveTo(x, y, x + r, y);
  const geo = new THREE.ShapeGeometry(s, 8);
  const pos = geo.attributes.position;
  const uv = new Float32Array(pos.count * 2);
  for (let i = 0; i < pos.count; i++) {
    uv[i * 2] = (pos.getX(i) - x) / w;
    uv[i * 2 + 1] = (pos.getY(i) - y) / h;
  }
  geo.setAttribute("uv", new THREE.BufferAttribute(uv, 2));
  return geo;
}

/** Soft round sprite for glows and particles. */
function glowTexture() {
  const c = document.createElement("canvas");
  c.width = c.height = 128;
  const g = c.getContext("2d")!;
  const grad = g.createRadialGradient(64, 64, 0, 64, 64, 64);
  grad.addColorStop(0, "rgba(255,255,255,1)");
  grad.addColorStop(0.35, "rgba(255,255,255,0.45)");
  grad.addColorStop(1, "rgba(255,255,255,0)");
  g.fillStyle = grad;
  g.fillRect(0, 0, 128, 128);
  const t = new THREE.CanvasTexture(c);
  t.colorSpace = THREE.SRGBColorSpace;
  return t;
}

const easeInOut = (t: number) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);
const easeOut = (t: number) => 1 - Math.pow(1 - t, 3);
const clamp01 = (t: number) => Math.min(1, Math.max(0, t));

const CARD_W = 1.15;
const CARD_H = 1.6;
const LOOP = 6; // seconds: two cards meet and fuse every 6s

/* ------------------------------------------------------------------ pieces */

function Card({
  geo,
  texture,
  glow,
  glowColor,
  cardRef,
}: {
  geo: THREE.BufferGeometry;
  texture: THREE.Texture;
  glow: THREE.Texture;
  glowColor: string;
  cardRef: (g: THREE.Group | null) => void;
}) {
  return (
    <group ref={cardRef}>
      {/* the soft glow behind each card is what the bloom picks up */}
      <sprite scale={[CARD_W * 2.2, CARD_H * 1.9, 1]} position={[0, 0, -0.05]}>
        <spriteMaterial map={glow} color={glowColor} transparent opacity={0.22} depthWrite={false} blending={THREE.AdditiveBlending} />
      </sprite>
      <mesh geometry={geo}>
        <meshBasicMaterial map={texture} transparent toneMapped={false} side={THREE.DoubleSide} />
      </mesh>
    </group>
  );
}

/** Points that start in the middle and spread out into a heart, then fade. */
function HeartBurst({ glow, progress }: { glow: THREE.Texture; progress: React.MutableRefObject<number> }) {
  const COUNT = 420;
  const { targets, colors } = useMemo(() => {
    const targets = new Float32Array(COUNT * 3);
    const colors = new Float32Array(COUNT * 3);
    const a = new THREE.Color("#ff8a6b");
    const b = new THREE.Color("#b79bff");
    for (let i = 0; i < COUNT; i++) {
      const t = Math.random() * Math.PI * 2;
      const fill = Math.sqrt(Math.random()) * 0.25 + 0.75; // mostly on the outline
      const x = 16 * Math.pow(Math.sin(t), 3);
      const y = 13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t);
      targets[i * 3] = x * 0.075 * fill;
      targets[i * 3 + 1] = y * 0.075 * fill + 0.15;
      targets[i * 3 + 2] = (Math.random() - 0.5) * 0.4;
      const c = a.clone().lerp(b, (x + 16) / 32);
      colors.set([c.r, c.g, c.b], i * 3);
    }
    return { targets, colors };
  }, []);
  const positions = useMemo(() => new Float32Array(COUNT * 3), []);
  const geo = useRef<THREE.BufferGeometry>(null);
  const mat = useRef<THREE.PointsMaterial>(null);

  useFrame(() => {
    const p = progress.current; // 0..1 while bursting, -1 otherwise
    if (!geo.current || !mat.current) return;
    if (p < 0) {
      mat.current.opacity = 0;
      return;
    }
    const spread = easeOut(clamp01(p / 0.55));
    for (let i = 0; i < COUNT * 3; i++) positions[i] = targets[i] * spread;
    geo.current.attributes.position.needsUpdate = true;
    mat.current.opacity = p < 0.55 ? 1 : 1 - easeInOut((p - 0.55) / 0.45);
    mat.current.size = 0.09 + 0.05 * (1 - spread);
  });

  return (
    <points>
      <bufferGeometry ref={geo}>
        <bufferAttribute attach="attributes-position" args={[positions, 3]} />
        <bufferAttribute attach="attributes-color" args={[colors, 3]} />
      </bufferGeometry>
      <pointsMaterial
        ref={mat}
        map={glow}
        vertexColors
        transparent
        depthWrite={false}
        blending={THREE.AdditiveBlending}
        sizeAttenuation
        toneMapped={false}
      />
    </points>
  );
}

function Scene() {
  const geo = useMemo(() => roundedCardGeometry(CARD_W, CARD_H, 0.12), []);
  const glow = useMemo(() => glowTexture(), []);
  const textures = useMemo(() => Array.from({ length: 8 }, (_, i) => portraitTexture(i)), []);
  const glowColors = ["#ff8a6b", "#b79bff", "#f66f9a", "#6cb8ff", "#ffb36b", "#4fd69c", "#ff8a6b", "#b79bff"];

  const ORBIT = 6;
  const orbitRefs = useRef<(THREE.Group | null)[]>([]);
  const pairRefs = useRef<(THREE.Group | null)[]>([]);
  const world = useRef<THREE.Group>(null);
  const burst = useRef(-1);
  const pointer = useRef({ x: 0, y: 0 });
  const { camera, size } = useThree();
  const stage = useRef<THREE.Group>(null);

  useEffect(() => {
    const onMove = (e: PointerEvent) => {
      pointer.current.x = (e.clientX / window.innerWidth) * 2 - 1;
      pointer.current.y = (e.clientY / window.innerHeight) * 2 - 1;
    };
    window.addEventListener("pointermove", onMove, { passive: true });
    return () => window.removeEventListener("pointermove", onMove);
  }, []);

  useFrame((state, delta) => {
    const t = state.clock.elapsedTime;
    const scroll = Math.min(1.5, window.scrollY / window.innerHeight);

    // mouse parallax + scroll: camera eases toward the pointer and backs off as you scroll
    camera.position.x += (pointer.current.x * 0.7 - camera.position.x) * Math.min(1, delta * 2.5);
    camera.position.y += (-pointer.current.y * 0.45 + scroll * 0.8 - camera.position.y) * Math.min(1, delta * 2.5);
    camera.position.z = 6.2 + scroll * 1.6;
    camera.lookAt(0, scroll * 0.4, 0);
    if (world.current) world.current.rotation.y = scroll * 0.6;
    // wide screen: composition sits right of the headline; narrow: above it, smaller
    const wide = size.width / size.height > 1.1;
    if (stage.current) {
      stage.current.position.set(wide ? 2.5 : 0.4, wide ? 0 : 1.1, 0);
      stage.current.scale.setScalar(wide ? 1 : 0.72);
    }

    // orbiting cards on a tilted ring, gently bobbing
    orbitRefs.current.forEach((g, i) => {
      if (!g) return;
      const a = t * 0.12 + (i / ORBIT) * Math.PI * 2;
      g.position.set(Math.cos(a) * 2.8, Math.sin(a * 2 + i) * 0.25 + Math.sin(a) * 0.8, Math.sin(a) * 2.2 - 1.6);
      g.rotation.y = -a + Math.PI / 2 + Math.sin(t * 0.4 + i) * 0.15;
      g.rotation.z = Math.sin(t * 0.3 + i) * 0.08;
    });

    // the pair: drift together, fuse into a heart of light, then fade back in at the sides
    const p = (t % LOOP) / LOOP;
    const meet = easeInOut(clamp01(p / 0.42));
    const fuse = clamp01((p - 0.42) / 0.08);
    const back = clamp01((p - 0.8) / 0.2);
    burst.current = p >= 0.44 && p < 0.86 ? (p - 0.44) / 0.42 : -1;
    pairRefs.current.forEach((g, i) => {
      if (!g) return;
      const side = i === 0 ? -1 : 1;
      const x = p < 0.5 ? side * (1.3 - 0.9 * meet) : side * 1.3; // meet in the middle, reappear at the sides
      g.position.set(x, 0.15 + Math.sin(t * 0.8 + i) * 0.06, 1.2);
      g.rotation.y = side * -0.35 * (1 - meet);
      g.rotation.z = side * -0.06;
      const visible = p < 0.42 ? 1 : p < 0.5 ? 1 - fuse : back;
      g.scale.setScalar(0.85 * (p < 0.5 ? 1 - fuse * 0.35 : 0.8 + 0.2 * back));
      g.traverse((o) => {
        const m = (o as THREE.Mesh).material as THREE.Material & { opacity: number };
        if (m && "opacity" in m) m.opacity = o.type === "Sprite" ? 0.22 * visible : visible;
      });
    });
  });

  return (
    <group ref={stage}>
      <group ref={world}>
        {Array.from({ length: ORBIT }, (_, i) => (
          <Card key={i} geo={geo} texture={textures[i + 2]} glow={glow} glowColor={glowColors[i + 2]} cardRef={(g) => (orbitRefs.current[i] = g)} />
        ))}
      </group>
      {[0, 1].map((i) => (
        <Card key={`pair-${i}`} geo={geo} texture={textures[i]} glow={glow} glowColor={glowColors[i]} cardRef={(g) => (pairRefs.current[i] = g)} />
      ))}
      <group position={[0, 0.15, 1.2]}>
        <HeartBurst glow={glow} progress={burst} />
      </group>
    </group>
  );
}

/* ------------------------------------------------------------------ canvas */

export default function HeroScene({ onReady }: { onReady?: () => void }) {
  const [active, setActive] = useState(true);
  const wrap = useRef<HTMLDivElement>(null);

  // stop rendering when the hero is off-screen (saves battery and keeps scrolling smooth)
  useEffect(() => {
    const el = wrap.current;
    if (!el) return;
    const io = new IntersectionObserver(([e]) => setActive(e.isIntersecting), { threshold: 0 });
    io.observe(el);
    return () => io.disconnect();
  }, []);

  return (
    <div ref={wrap} className="absolute inset-0" aria-hidden>
      <Canvas
        dpr={[1, 1.5]}
        frameloop={active ? "always" : "never"}
        camera={{ position: [0, 0, 6.2], fov: 42 }}
        gl={{ antialias: false, alpha: true, powerPreference: "high-performance" }}
        onCreated={() => onReady?.()}
      >
        <Scene />
        <EffectComposer multisampling={0}>
          <DepthOfField focusDistance={0.012} focalLength={0.03} bokehScale={2.2} height={480} />
          <Bloom intensity={0.6} luminanceThreshold={0.3} luminanceSmoothing={0.4} mipmapBlur />
        </EffectComposer>
      </Canvas>
    </div>
  );
}
