"use client";

import dynamic from "next/dynamic";
import { useEffect, useState, type ReactNode } from "react";
import { canRun3D } from "@/lib/device";
import type { TrinketKind } from "@/components/three/Trinket";

const Trinket = dynamic(() => import("@/components/three/Trinket"), { ssr: false });

/** Static stand-ins (no WebGL / reduced motion / weak device). */
function StaticArt({ kind }: { kind: TrinketKind }) {
  if (kind === "rings")
    return (
      <svg viewBox="0 0 160 160" className="h-full w-full" aria-hidden>
        <circle cx="64" cy="80" r="34" fill="none" stroke="#ff8a6b" strokeWidth="12" />
        <circle cx="96" cy="80" r="34" fill="none" stroke="#b79bff" strokeWidth="12" />
      </svg>
    );
  if (kind === "moon")
    return (
      <svg viewBox="0 0 160 160" className="h-full w-full" aria-hidden>
        <circle cx="80" cy="80" r="44" fill="#f4d9c8" />
        <circle cx="98" cy="68" r="36" fill="var(--bg)" />
      </svg>
    );
  return (
    <svg viewBox="0 0 160 160" className="h-full w-full" aria-hidden>
      <rect x="34" y="34" width="60" height="84" rx="10" fill="#b79bff" transform="rotate(-10 64 76)" />
      <rect x="66" y="40" width="60" height="84" rx="10" fill="#ff8a6b" transform="rotate(8 96 82)" />
    </svg>
  );
}

export function Illustration({ kind, size = 160 }: { kind: TrinketKind; size?: number }) {
  const [three, setThree] = useState(false);
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- device check needs the browser
    setThree(canRun3D());
  }, []);
  return (
    <div style={{ width: size, height: size }} aria-hidden>
      {three ? <Trinket kind={kind} /> : <StaticArt kind={kind} />}
    </div>
  );
}

export function EmptyState({ kind, title, body, action }: { kind: TrinketKind; title: string; body?: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center px-6 py-12 text-center">
      <Illustration kind={kind} />
      <h2 className="mt-6 font-display text-xl font-normal">{title}</h2>
      {body && <p className="mt-2 max-w-sm text-muted">{body}</p>}
      {action && <div className="mt-6">{action}</div>}
    </div>
  );
}
