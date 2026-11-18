"use client";

import { motion, useReducedMotion } from "framer-motion";
import { useEffect, useState } from "react";
import { untilText, useEnergy } from "@/lib/energy";
import { spring } from "@/lib/motion";

const R = 17;
const C = 2 * Math.PI * R;

/** Ring in the header: drains with each swipe, refills at midnight. A design choice, not an error. */
export function EnergyMeter() {
  const { quota } = useEnergy();
  const reduce = useReducedMotion();
  const [open, setOpen] = useState(false);
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    if (!open) return;
    const id = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(id);
  }, [open]);

  const level = quota ? quota.remaining / quota.limit : 1;
  const label = quota ? `${quota.remaining} of ${quota.limit} swipes left today` : "Loading today's energy";

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        onBlur={() => setOpen(false)}
        aria-label={label}
        aria-expanded={open}
        className="flex min-h-12 items-center gap-2 rounded-full px-2 text-sm text-muted hover:text-text"
      >
        <svg width="40" height="40" viewBox="0 0 40 40" aria-hidden className="-rotate-90">
          <defs>
            <linearGradient id="energy-grad" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0" stopColor="var(--grad-a)" />
              <stop offset="0.5" stopColor="var(--grad-b)" />
              <stop offset="1" stopColor="var(--grad-c)" />
            </linearGradient>
          </defs>
          <circle cx="20" cy="20" r={R} fill="none" stroke="var(--surface-2)" strokeWidth="4" />
          <motion.circle
            cx="20"
            cy="20"
            r={R}
            fill="none"
            stroke="url(#energy-grad)"
            strokeWidth="4"
            strokeLinecap="round"
            strokeDasharray={C}
            initial={false}
            animate={{ strokeDashoffset: C * (1 - level) }}
            transition={reduce ? { duration: 0 } : spring.gentle}
          />
        </svg>
        <span className="tabular hidden font-medium text-text sm:inline">{quota ? quota.remaining : "–"}</span>
      </button>
      {open && quota && (
        <div role="status" className="absolute right-0 top-14 z-30 w-64 rounded-md border border-line bg-surface p-4 text-sm shadow-card">
          <p className="font-medium text-text">{label}</p>
          <p className="mt-1 text-muted">
            Refills at midnight, in <span className="tabular">{untilText(new Date(quota.resetsAt).getTime() - now)}</span>.
          </p>
        </div>
      )}
    </div>
  );
}
