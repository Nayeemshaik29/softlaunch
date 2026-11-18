"use client";

import { Sparkles } from "@react-three/drei";
import { Canvas, useFrame } from "@react-three/fiber";
import { useMemo, useRef } from "react";
import * as THREE from "three";
import type { palettes } from "@/lib/mood";

type Palette = (typeof palettes)[keyof typeof palettes];

/** Resting energy: a 3D ring that slowly refills as midnight gets closer, around a softly breathing core. */
function Ring({ fill, palette }: { fill: number; palette: Palette }) {
  const group = useRef<THREE.Group>(null);
  const core = useRef<THREE.Mesh>(null);
  const arc = Math.max(0.05, fill) * Math.PI * 2;
  const geo = useMemo(() => new THREE.TorusGeometry(1.25, 0.07, 24, 160, arc), [arc]);
  useFrame(({ clock }, d) => {
    const t = clock.elapsedTime;
    if (group.current) {
      group.current.rotation.z -= d * 0.08;
      group.current.rotation.x = 1.05 + Math.sin(t * 0.3) * 0.06;
    }
    if (core.current) core.current.scale.setScalar(0.42 + Math.sin(t * 0.9) * 0.025); // ~7s breath
  });
  return (
    <group>
      <group ref={group}>
        <mesh>
          <torusGeometry args={[1.25, 0.05, 16, 160]} />
          <meshStandardMaterial color="#2f2934" roughness={0.9} />
        </mesh>
        <mesh geometry={geo}>
          <meshStandardMaterial color={palette.a} emissive={palette.b} emissiveIntensity={0.9} roughness={0.4} />
        </mesh>
      </group>
      <mesh ref={core}>
        <sphereGeometry args={[1, 48, 48]} />
        <meshStandardMaterial color={palette.core} emissive={palette.core} emissiveIntensity={0.35} roughness={0.6} />
      </mesh>
      <Sparkles count={40} scale={4} size={2} speed={0.15} opacity={0.5} color="#f4d9c8" />
    </group>
  );
}

export default function RestScene({ fill, palette }: { fill: number; palette: Palette }) {
  return (
    <Canvas dpr={[1, 1.5]} camera={{ position: [0, 0, 4.4], fov: 40 }} gl={{ alpha: true }}>
      <ambientLight intensity={0.5} />
      <pointLight position={[2, 3, 4]} intensity={20} color="#ffd9c8" />
      <Ring fill={fill} palette={palette} />
    </Canvas>
  );
}
