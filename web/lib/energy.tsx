"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { api } from "./api";
import type { SwipeQuota } from "./types";

// "Energy" = the daily swipe allowance from match-service (GET /api/matches/swipes/quota).
// The meter drains optimistically on each swipe and is corrected by the server's answers.
interface Energy {
  quota: SwipeQuota | null;
  spend: () => void;
  refund: () => void;
  exhausted: (resetsAt?: string) => void;
  refresh: () => Promise<void>;
}

const Ctx = createContext<Energy | null>(null);

export function EnergyProvider({ children }: { children: ReactNode }) {
  const [quota, setQuota] = useState<SwipeQuota | null>(null);

  const refresh = useCallback(async () => {
    try {
      setQuota(await api<SwipeQuota>("/api/matches/swipes/quota"));
    } catch {
      /* the meter is a nicety: keep the last value */
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- loading the quota once on mount is the point
    void refresh();
  }, [refresh]);

  // refill at midnight (the server's resetsAt), without a page reload
  useEffect(() => {
    if (!quota) return;
    const ms = new Date(quota.resetsAt).getTime() - Date.now() + 1500;
    if (ms <= 0 || ms > 2 ** 31 - 1) return;
    const id = window.setTimeout(() => void refresh(), ms);
    return () => window.clearTimeout(id);
  }, [quota, refresh]);

  const value = useMemo<Energy>(
    () => ({
      quota,
      spend: () => setQuota((q) => (q ? { ...q, remaining: Math.max(0, q.remaining - 1) } : q)),
      refund: () => setQuota((q) => (q ? { ...q, remaining: Math.min(q.limit, q.remaining + 1) } : q)),
      exhausted: (resetsAt) => setQuota((q) => ({ limit: q?.limit ?? 50, remaining: 0, resetsAt: resetsAt ?? q?.resetsAt ?? new Date(Date.now() + 864e5).toISOString() })),
      refresh,
    }),
    [quota, refresh],
  );
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useEnergy() {
  const v = useContext(Ctx);
  if (!v) throw new Error("useEnergy outside EnergyProvider");
  return v;
}

/** "5h 12m" / "12m 03s" / "45s" */
export function untilText(ms: number) {
  const s = Math.max(0, Math.floor(ms / 1000));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;
  if (h > 0) return `${h}h ${String(m).padStart(2, "0")}m`;
  if (m > 0) return `${m}m ${String(sec).padStart(2, "0")}s`;
  return `${sec}s`;
}
