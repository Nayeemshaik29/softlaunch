"use client";

import { motion, useReducedMotion } from "framer-motion";
import type { ReactNode } from "react";
import { spring } from "@/lib/motion";

/** Progress arc at the top of onboarding: one segment per step, shared across steps (layoutId). */
export function ProgressArc({ step, total }: { step: number; total: number }) {
  const reduce = useReducedMotion();
  const r = 26;
  const c = 2 * Math.PI * r;
  const frac = step / total;
  return (
    <motion.div layoutId="progress-arc" className="relative grid h-16 w-16 place-items-center" role="progressbar" aria-valuemin={1} aria-valuemax={total} aria-valuenow={step} aria-label={`Step ${step} of ${total}`}>
      <svg width="64" height="64" viewBox="0 0 64 64" className="-rotate-90" aria-hidden>
        <defs>
          <linearGradient id="arc-grad" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stopColor="var(--grad-a)" />
            <stop offset="1" stopColor="var(--grad-c)" />
          </linearGradient>
        </defs>
        <circle cx="32" cy="32" r={r} fill="none" stroke="var(--surface-2)" strokeWidth="5" />
        <motion.circle cx="32" cy="32" r={r} fill="none" stroke="url(#arc-grad)" strokeWidth="5" strokeLinecap="round" strokeDasharray={c}
          initial={false} animate={{ strokeDashoffset: c * (1 - frac) }} transition={reduce ? { duration: 0 } : spring.gentle} />
      </svg>
      <span className="absolute text-sm font-medium tabular" aria-hidden>{step}/{total}</span>
    </motion.div>
  );
}

/** Profile-completion ring: fills live as photos, bio and interests are added. */
export function CompletionRing({ percent }: { percent: number }) {
  const reduce = useReducedMotion();
  const r = 20;
  const c = 2 * Math.PI * r;
  return (
    <div className="flex items-center gap-4" role="status" aria-live="polite">
      <div className="relative grid h-14 w-14 place-items-center">
        <svg width="56" height="56" viewBox="0 0 56 56" className="-rotate-90" aria-hidden>
          <circle cx="28" cy="28" r={r} fill="none" stroke="var(--surface-2)" strokeWidth="5" />
          <motion.circle cx="28" cy="28" r={r} fill="none" stroke="var(--accent)" strokeWidth="5" strokeLinecap="round" strokeDasharray={c}
            initial={false} animate={{ strokeDashoffset: c * (1 - percent / 100) }} transition={reduce ? { duration: 0 } : spring.soft} />
        </svg>
        <span className="absolute text-xs font-semibold tabular">{percent}%</span>
      </div>
      <p className="text-sm text-muted">
        Profile {percent}% complete
        <span className="block text-xs">More details = better matches. You can add the rest later.</span>
      </p>
    </div>
  );
}

/** Big, thumb-friendly choice tile (single or multi select). */
export function Choice({ selected, onClick, children, role = "radio" }: { selected: boolean; onClick: () => void; children: ReactNode; role?: "radio" | "checkbox" }) {
  const reduce = useReducedMotion();
  return (
    <motion.button
      type="button"
      role={role}
      aria-checked={selected}
      onClick={onClick}
      whileTap={reduce ? undefined : { scale: 0.97 }}
      transition={spring.snappy}
      className={`min-h-14 rounded-md border px-4 text-left text-base font-medium transition-colors ${selected ? "border-accent bg-surface-2 text-text" : "border-line bg-surface text-muted hover:text-text"}`}
    >
      <span className="flex items-center justify-between gap-2">
        {children}
        <span aria-hidden className={`grid h-6 w-6 place-items-center rounded-full border ${selected ? "border-accent bg-accent text-on-accent" : "border-line"}`}>
          {selected && <svg width="12" height="12" viewBox="0 0 12 12"><path d="M2.5 6.2l2.2 2.2 4.8-4.8" stroke="currentColor" strokeWidth="1.8" fill="none" strokeLinecap="round" strokeLinejoin="round" /></svg>}
        </span>
      </span>
    </motion.button>
  );
}

export function Chip({ selected, onClick, children, disabled }: { selected: boolean; onClick: () => void; children: ReactNode; disabled?: boolean }) {
  return (
    <button
      type="button"
      aria-pressed={selected}
      disabled={disabled && !selected}
      onClick={onClick}
      className={`min-h-12 rounded-full border px-4 text-sm font-medium transition-colors disabled:opacity-45 ${selected ? "border-accent bg-surface-2 text-text" : "border-line text-muted hover:text-text"}`}
    >
      {children}
    </button>
  );
}
