"use client";

import { Float } from "@react-three/drei";
import { Canvas, useFrame } from "@react-three/fiber";
import { useRef } from "react";
import * as THREE from "three";

export type TrinketKind = "cards" | "rings" | "moon";

/** Small, slow 3D objects for empty states (≤160px). Loaded lazily by <Illustration>. */
function Cards() {
  return (
    <group rotation={[0.2, -0.4, 0]}>
      <Float speed={1.4} rotationIntensity={0.3} floatIntensity={0.5}>
        <mesh position={[-0.35, 0, -0.2]} rotation={[0, 0, 0.18]}>
          <boxGeometry args={[1.1, 1.5, 0.05]} />
          <meshStandardMaterial color="#b79bff" roughness={0.35} metalness={0.1} />
        </mesh>
        <mesh position={[0.3, -0.05, 0.15]} rotation={[0, 0, -0.12]}>
          <boxGeometry args={[1.1, 1.5, 0.05]} />
          <meshStandardMaterial color="#ff8a6b" roughness={0.35} metalness={0.1} />
        </mesh>
      </Float>
    </group>
  );
}

function Rings() {
  const g = useRef<THREE.Group>(null);
  useFrame((_, d) => g.current && (g.current.rotation.y += d * 0.35));
  return (
    <group ref={g} rotation={[0.5, 0, 0]}>
      <mesh position={[-0.35, 0, 0]} rotation={[0, 0.5, 0]}>
        <torusGeometry args={[0.6, 0.12, 24, 64]} />
        <meshStandardMaterial color="#ff8a6b" roughness={0.3} metalness={0.2} />
      </mesh>
      <mesh position={[0.35, 0, 0]} rotation={[Math.PI / 2, 0.5, 0]}>
        <torusGeometry args={[0.6, 0.12, 24, 64]} />
        <meshStandardMaterial color="#b79bff" roughness={0.3} metalness={0.2} />
      </mesh>
    </group>
  );
}

function Moon() {
  const m = useRef<THREE.Mesh>(null);
  useFrame(({ clock }) => {
    if (m.current) m.current.scale.setScalar(1 + Math.sin(clock.elapsedTime * 0.8) * 0.03);
  });
  return (
    <Float speed={1} floatIntensity={0.6} rotationIntensity={0.1}>
      <mesh ref={m}>
        <sphereGeometry args={[0.8, 48, 48]} />
        <meshStandardMaterial color="#f4d9c8" emissive="#ff8a6b" emissiveIntensity={0.25} roughness={0.8} />
      </mesh>
      <mesh position={[0.32, 0.2, 0.55]}>
        <sphereGeometry args={[0.62, 48, 48]} />
        <meshStandardMaterial color="#17141a" roughness={1} />
      </mesh>
    </Float>
  );
}

export default function Trinket({ kind }: { kind: TrinketKind }) {
  return (
    <Canvas dpr={[1, 1.5]} camera={{ position: [0, 0, 3.6], fov: 40 }} gl={{ alpha: true, antialias: true }}>
      <ambientLight intensity={0.6} />
      <directionalLight position={[2, 3, 4]} intensity={1.6} />
      <pointLight position={[-3, -1, 2]} intensity={6} color="#b79bff" />
      {kind === "cards" ? <Cards /> : kind === "rings" ? <Rings /> : <Moon />}
    </Canvas>
  );
}
