"use client";

import dynamic from "next/dynamic";
import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from "react";

export interface MatchMomentData {
  matchId: string;
  name: string;
  theirPhoto: string | null;
  myPhoto: string | null;
}

// The cinematic scene (three.js) is only downloaded when a match actually happens.
const MatchMoment = dynamic(() => import("@/components/match/MatchMoment"), { ssr: false });

interface Ctx {
  celebrate: (m: MatchMomentData) => void;
  alreadyCelebrated: (matchId: string) => boolean;
}
const C = createContext<Ctx | null>(null);
const SEEN_KEY = "softlaunch.celebrated";

function loadSeen(): Set<string> {
  try {
    return new Set(JSON.parse(localStorage.getItem(SEEN_KEY) ?? "[]"));
  } catch {
    return new Set();
  }
}

export function MatchMomentProvider({ children }: { children: ReactNode }) {
  const [current, setCurrent] = useState<MatchMomentData | null>(null);
  const seen = useRef<Set<string> | null>(null);

  const seenSet = () => (seen.current ??= loadSeen());

  const celebrate = useCallback((m: MatchMomentData) => {
    const s = seenSet();
    if (s.has(m.matchId)) return;
    s.add(m.matchId);
    try {
      localStorage.setItem(SEEN_KEY, JSON.stringify([...s].slice(-200)));
    } catch {
      /* per-device convenience only */
    }
    setCurrent(m);
  }, []);

  const value = useMemo<Ctx>(() => ({ celebrate, alreadyCelebrated: (id) => seenSet().has(id) }), [celebrate]);

  return (
    <C.Provider value={value}>
      {children}
      {current && <MatchMoment data={current} onClose={() => setCurrent(null)} />}
    </C.Provider>
  );
}

export function useMatchMoment() {
  const v = useContext(C);
  if (!v) throw new Error("useMatchMoment outside provider");
  return v;
}
