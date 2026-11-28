"use client";

import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useRef, useState } from "react";
import { DesireIcon } from "@/components/desires/DesireIcon";
import { Button } from "@/components/ui/Button";
import { Note } from "@/components/ui/Doodles";
import { Skeleton } from "@/components/ui/Skeleton";
import { api, ApiError } from "@/lib/api";
import { DESIRES, LIMIT, type DesireInfo, type Level } from "@/lib/desires";
import { spring } from "@/lib/motion";
import { haptic } from "@/lib/native";
import type { Desires } from "@/lib/types";

const LEVEL_TEXT: Record<Level, string> = { none: "Not set", into: "Into", openTo: "Open to", notInto: "Not into" };

/** Chip on each card: amber = into, peach = open to, struck through = not into. */
function LevelChip({ level }: { level: Level }) {
  if (level === "none") return null;
  const cls = level === "into" ? "bg-accent text-on-accent" : level === "openTo" ? "bg-accent-2 text-on-accent" : "border border-line text-muted line-through";
  return <span className={`rounded-full px-2.5 py-0.5 text-[11px] font-bold tracking-[0.06em] uppercase ${cls}`}>{LEVEL_TEXT[level]}</span>;
}

/** The sheet that opens when you tap a desire: big animated symbol, what it means, and the three choices. */
function DesireSheet({ d, level, counts, onPick, onClose }: {
  d: DesireInfo; level: Level; counts: { into: number; openTo: number };
  onPick: (l: Level) => void; onClose: () => void;
}) {
  const reduce = useReducedMotion();
  const first = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    first.current?.focus();
    const esc = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", esc);
    return () => window.removeEventListener("keydown", esc);
  }, [onClose]);

  const intoFull = counts.into >= LIMIT.into && level !== "into";
  const openFull = counts.openTo >= LIMIT.openTo && level !== "openTo";
  const choice = (l: Level, label: string, hint: string, disabled = false, ref?: React.Ref<HTMLButtonElement>) => (
    <button ref={ref} type="button" disabled={disabled} aria-pressed={level === l} onClick={() => onPick(level === l ? "none" : l)}
      className={`flex min-h-14 flex-col items-start justify-center rounded-md border-2 px-4 text-left transition-colors disabled:opacity-40 ${
        level === l
          ? l === "into" ? "border-bg bg-accent text-on-accent shadow-sticker" : l === "openTo" ? "border-bg bg-accent-2 text-on-accent shadow-sticker-accent" : "border-text bg-surface-2"
          : "border-line hover:border-text"}`}>
      <span className="text-sm font-bold tracking-[0.08em] uppercase">{level === l ? `✓ ${label}` : label}</span>
      <span className={`text-xs ${level === l && l !== "notInto" ? "" : "text-muted"}`}>{hint}</span>
    </button>
  );

  return (
    <motion.div className="fixed inset-0 z-50 flex items-end justify-center bg-bg/70 backdrop-blur-sm sm:items-center"
      initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}>
      <motion.div role="dialog" aria-modal="true" aria-labelledby="desire-name" onClick={(e) => e.stopPropagation()}
        initial={reduce ? { opacity: 0 } : { y: 60, opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={reduce ? { opacity: 0 } : { y: 60, opacity: 0 }}
        transition={spring.gentle}
        className="w-full max-w-md overflow-hidden rounded-t-lg border-2 border-text bg-bg pb-[max(1.25rem,env(safe-area-inset-bottom))] sm:rounded-lg">
        <div className="relative grid h-48 place-items-center bg-ember">
          <span className="grid size-32 place-items-center rounded-full border-[3px] border-bg bg-accent-2 text-on-accent shadow-[6px_6px_0_var(--bg)]">
            <DesireIcon id={d.id} size={72} className={`desire-${d.anim}`} />
          </span>
          <button type="button" onClick={onClose} aria-label="Close"
            className="absolute right-3 top-3 grid size-11 place-items-center rounded-full bg-bg text-text">
            <svg aria-hidden width="18" height="18" viewBox="0 0 20 20"><path d="M5 5l10 10M15 5L5 15" stroke="currentColor" strokeWidth="2" strokeLinecap="round" /></svg>
          </button>
        </div>
        <div className="px-5 pt-5">
          <h2 id="desire-name" className="font-display text-5xl leading-[0.9]">{d.label}</h2>
          <p className="mt-2 text-lg">{d.line}</p>
          <div className="mt-5 grid gap-2">
            {choice("into", "Into", intoFull ? `You've picked ${LIMIT.into} already` : "Shown when you both are", intoFull, first)}
            {choice("openTo", "Open to", openFull ? `You've picked ${LIMIT.openTo} already` : "Happy to, if they are", openFull)}
            {choice("notInto", "Not into", "Private. Nobody ever sees this")}
          </div>
        </div>
      </motion.div>
    </motion.div>
  );
}

export default function DesiresPage() {
  const router = useRouter();
  const reduce = useReducedMotion();
  const [levels, setLevels] = useState<Record<string, Level> | null>(null);
  const [open, setOpen] = useState<DesireInfo | null>(null);
  const [busy, setBusy] = useState(false);
  const [dirty, setDirty] = useState(false);
  const [status, setStatus] = useState("");

  useEffect(() => {
    void api<Desires>("/api/users/me/profile/desires").then((d) => {
      const l: Record<string, Level> = {};
      d.into.forEach((x) => (l[x] = "into"));
      d.openTo.forEach((x) => (l[x] = "openTo"));
      d.notInto.forEach((x) => (l[x] = "notInto"));
      setLevels(l);
    }).catch(() => setLevels({}));
  }, []);

  const counts = useMemo(() => {
    const v = Object.values(levels ?? {});
    return { into: v.filter((x) => x === "into").length, openTo: v.filter((x) => x === "openTo").length, notInto: v.filter((x) => x === "notInto").length };
  }, [levels]);

  const pick = (d: DesireInfo, l: Level) => {
    setLevels((x) => ({ ...(x ?? {}), [d.id]: l }));
    setDirty(true);
    setStatus(`${d.label}: ${LEVEL_TEXT[l]}`);
    if (l === "into" || l === "openTo") void haptic("tap");
    setOpen(null);
  };

  async function save() {
    if (!levels) return;
    setBusy(true);
    const of = (lv: Level) => Object.entries(levels).filter(([, v]) => v === lv).map(([k]) => k);
    try {
      await api("/api/users/me/profile/desires", { method: "PUT", body: { into: of("into"), openTo: of("openTo"), notInto: of("notInto") } });
      setStatus("Saved. When someone shares one of yours, it glows on both your cards.");
      setDirty(false);
      setTimeout(() => router.push("/swipe"), 900);
    } catch (e) {
      setStatus((e as ApiError).message);
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto max-w-3xl pb-24">
      <div className="flex items-end justify-between gap-3">
        <div>
          <p className="text-xs font-bold tracking-[0.14em] text-accent uppercase">Desires</p>
          <h1 className="mt-1 font-display text-[clamp(3rem,13vw,5rem)] leading-[0.88]">What you&rsquo;re<br />into</h1>
        </div>
        <Note className="mb-1 shrink-0 -rotate-6 text-right text-xl sm:text-2xl">only shared<br />ones ever show</Note>
      </div>
      <p className="mt-4 max-w-xl text-muted">
        Tap one to see what it means. Pick up to {LIMIT.into} you&rsquo;re <b className="text-accent">into</b> and {LIMIT.openTo} you&rsquo;re <b className="text-text">open to</b>.
        Your &ldquo;not into&rdquo; stays private.
      </p>
      <div className="mt-4 flex flex-wrap gap-2 text-xs font-bold tracking-[0.06em] uppercase" aria-live="polite">
        <span className="rounded-full bg-accent px-3 py-1 text-on-accent tabular">Into {counts.into}/{LIMIT.into}</span>
        <span className="rounded-full bg-accent-2 px-3 py-1 text-on-accent tabular">Open to {counts.openTo}/{LIMIT.openTo}</span>
        <span className="rounded-full border border-line px-3 py-1 text-muted tabular">Not into {counts.notInto}</span>
      </div>
      <p role="status" aria-live="polite" className="mt-2 min-h-6 text-sm text-muted">{status}</p>

      {levels === null ? (
        <div className="mt-2 grid grid-cols-2 gap-3 sm:grid-cols-3">{Array.from({ length: 6 }, (_, i) => <Skeleton key={i} className="h-40 rounded-lg" />)}</div>
      ) : (
        <ul className="mt-2 grid grid-cols-2 gap-3 sm:grid-cols-3">
          {DESIRES.map((d, i) => {
            const lv = levels[d.id] ?? "none";
            return (
              <motion.li key={d.id} initial={reduce ? false : { opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
                transition={{ ...spring.soft, delay: reduce ? 0 : Math.min(i * 0.025, 0.4) }}>
                <button type="button" onClick={() => setOpen(d)} aria-haspopup="dialog" aria-label={`${d.label}: ${LEVEL_TEXT[lv]}. ${d.line}`}
                  className={`group flex h-full min-h-40 w-full flex-col items-start gap-2 rounded-lg border-2 p-3.5 text-left transition-transform active:scale-[0.97] ${
                    lv === "into" ? "border-accent bg-surface shadow-sticker-accent"
                      : lv === "openTo" ? "border-accent-2 bg-surface shadow-sticker"
                        : lv === "notInto" ? "border-line bg-bg opacity-60" : "border-line bg-surface hover:border-text"}`}>
                  <span className={`grid size-12 place-items-center rounded-full ${lv === "into" ? "bg-accent text-on-accent" : lv === "openTo" ? "bg-accent-2 text-on-accent" : "bg-surface-2 text-accent"}`}>
                    <DesireIcon id={d.id} size={30} className="transition-transform group-hover:scale-110" />
                  </span>
                  <span className={`font-display text-2xl leading-[0.95] ${lv === "notInto" ? "line-through" : ""}`}>{d.label}</span>
                  <span className="line-clamp-2 text-xs leading-snug text-muted">{d.line}</span>
                  <span className="mt-auto"><LevelChip level={lv} /></span>
                </button>
              </motion.li>
            );
          })}
        </ul>
      )}

      {levels !== null && (
        <div className="fixed inset-x-0 bottom-16 z-30 border-t-2 border-surface-2 bg-bg/90 px-4 py-3 backdrop-blur md:bottom-0">
          <div className="mx-auto flex max-w-3xl items-center justify-end gap-3">
            <Button variant="ghost" onClick={() => router.back()}>Cancel</Button>
            <Button onClick={() => void save()} disabled={busy || !dirty}>{busy ? "Saving…" : "Save desires"}</Button>
          </div>
        </div>
      )}

      <AnimatePresence>
        {open && levels && (
          <DesireSheet key={open.id} d={open} level={levels[open.id] ?? "none"}
            counts={{ into: counts.into, openTo: counts.openTo }} onPick={(l) => pick(open, l)} onClose={() => setOpen(null)} />
        )}
      </AnimatePresence>
    </div>
  );
}
