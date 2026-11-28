"use client";

import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { Heart } from "@/components/hearts/Heart";
import { api, ApiError } from "@/lib/api";
import { useCalls } from "@/lib/calls";
import { needsHearts, spendHearts } from "@/lib/hearts";
import { spring } from "@/lib/motion";
import type { CallStatus } from "@/lib/types";

/** The call money rules for one chat, from chat-service (refreshes every 30 s and after each call). */
export function useCallStatus(matchId: string) {
  const { state } = useCalls();
  const [status, setStatus] = useState<CallStatus | null>(null);
  const refresh = useCallback(() => api<CallStatus>(`/api/chats/${matchId}/calls/status`).then(setStatus).catch(() => {}), [matchId]);
  useEffect(() => {
    void refresh();
    const id = window.setInterval(() => void refresh(), 30_000);
    return () => window.clearInterval(id);
  }, [refresh]);
  useEffect(() => { if (state.phase === "ended" || state.phase === "active") void refresh(); }, [state.phase, refresh]);
  return { status, refresh };
}

function useNow(ms = 1000) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => { const id = window.setInterval(() => setNow(Date.now()), ms); return () => window.clearInterval(id); }, [ms]);
  return now;
}

/** Day 1 · 2 · 3 · 🎥 Day 4 */
function DayTracker({ s }: { s: CallStatus }) {
  return (
    <div className="flex items-center justify-center gap-2" aria-label={`Called on ${s.callDays} of ${s.daysForVideo} days needed for video`}>
      {Array.from({ length: s.daysForVideo + 1 }, (_, i) => {
        const done = i < s.callDays;
        const video = i === s.daysForVideo;
        const lit = video ? s.videoUnlocked : done;
        return (
          <span key={i} className="flex flex-col items-center gap-1 text-[11px] font-bold">
            <span className={`grid size-10 place-items-center rounded-full border-2 ${lit ? "border-accent bg-accent text-on-accent" : video ? "border-text" : "border-line text-muted"}`}>
              {video ? "🎥" : done ? "✓" : i + 1}
            </span>
            Day {i + 1}
          </span>
        );
      })}
    </div>
  );
}

/**
 * Under the messages: the free calling day countdown, the day tracker (3 call days unlock video), and the
 * pass button once the free day is over. Only shown when both of you allowed calls.
 */
export function CallPlanBar({ matchId, name, s, onChange }: { matchId: string; name: string; s: CallStatus; onChange: () => void }) {
  const now = useNow();
  const [paywall, setPaywall] = useState(false);
  if (s.consent.allowed === "OFF") return null;
  const left = s.freeUntil ? Date.parse(s.freeUntil) - now : null;
  const inFree = s.me === "FREE_WINDOW" && left !== null && left > 0;
  const hms = (ms: number) => { const t = Math.floor(ms / 1000); return [Math.floor(t / 3600), Math.floor((t % 3600) / 60), t % 60].map((n) => String(n).padStart(2, "0")).join(":"); };

  return (
    <div className="flex flex-col gap-3 border-t border-line py-3">
      {inFree && (
        <div className="rounded-lg border-2 border-bg bg-accent px-4 py-3 text-on-accent shadow-sticker">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold tracking-[0.12em] uppercase">Free calling day</span>
            <span className="font-display text-3xl tabular">{hms(left!)}</span>
          </div>
          <div className="mt-2 h-2 overflow-hidden rounded-full bg-bg/20"><div className="h-full rounded-full bg-bg" style={{ width: `${Math.max(2, (left! / 86_400_000) * 100)}%` }} /></div>
          <p className="mt-2 text-xs font-semibold">Started with your first call. After that, a ₹50 / 10 ❤ day pass each to keep calling.</p>
        </div>
      )}
      {!s.firstCallDone && <p className="text-center text-xs text-muted">Your first 24 hours of calls with {name} are free. The clock starts at your first call.</p>}
      {s.me === "FREE_TIER" && <p className="text-center text-xs text-muted">Calls are free for you with verified Plus.</p>}
      {s.me === "NONE" && (
        <button type="button" onClick={() => setPaywall(true)} className="flex min-h-12 items-center justify-between rounded-md border-2 border-accent bg-surface px-4 text-left">
          <span><b className="block">Your free calling day is over</b><span className="text-xs text-muted">Get today&rsquo;s pass to keep calling {name}</span></span>
          <span className="font-bold text-accent">₹50 ›</span>
        </button>
      )}
      <DayTracker s={s} />
      {!s.videoUnlocked && <p className="text-center font-[family-name:var(--font-hand)] text-lg text-accent">call on {s.daysForVideo} different days → day {s.daysForVideo + 1} video is on us</p>}
      <AnimatePresence>{paywall && <PassSheet matchId={matchId} name={name} s={s} onClose={() => setPaywall(false)} onPaid={() => { setPaywall(false); onChange(); }} />}</AnimatePresence>
    </div>
  );
}

/** ₹50 per day, each: 10 ❤ from the wallet (top up on the Hearts page). Both people need access to call. */
export function PassSheet({ matchId, name, s, onClose, onPaid }: { matchId: string; name: string; s: CallStatus; onClose: () => void; onPaid: () => void }) {
  const reduce = useReducedMotion();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<"hearts" | string | null>(null);
  const buy = async () => {
    setBusy(true);
    setError(null);
    try {
      await spendHearts("CALL_PASS", matchId);
      onPaid();
    } catch (e) {
      setError(needsHearts(e) ? "hearts" : (e as ApiError).message);
    } finally {
      setBusy(false);
    }
  };
  const chip = (ok: boolean, yes: string, no: string) => (
    <span className={`rounded-full border-[1.5px] px-3 py-0.5 text-xs font-bold ${ok ? "border-accent bg-accent text-on-accent" : "border-dashed border-text"}`}>{ok ? `✓ ${yes}` : no}</span>
  );
  const themOk = s.them !== "NONE";
  return (
    <motion.div className="fixed inset-0 z-50 flex items-end justify-center bg-bg/75 backdrop-blur-sm sm:items-center" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}>
      <motion.div role="dialog" aria-modal="true" aria-labelledby="pass-h" onClick={(e) => e.stopPropagation()}
        initial={reduce ? { opacity: 0 } : { y: 80 }} animate={{ y: 0, opacity: 1 }} exit={reduce ? { opacity: 0 } : { y: 80 }} transition={spring.gentle}
        className="w-full max-w-md overflow-hidden rounded-t-lg border-2 border-text bg-bg pb-[max(1.25rem,env(safe-area-inset-bottom))] sm:rounded-lg">
        <div className="relative grid h-28 place-items-center bg-burst">
          <span className="font-display text-6xl text-on-accent">₹50</span>
          <span className="absolute bottom-2 text-xs font-extrabold tracking-[0.1em] text-on-accent">PER DAY · EACH · OR 10 ❤</span>
        </div>
        <div className="px-5 pt-4">
          <h2 id="pass-h" className="font-display text-4xl">Keep the calls going</h2>
          <p className="mt-1 text-muted">Calls with {name} continue when you <b className="text-text">both</b> have today&rsquo;s pass.</p>
          <div className="mt-3 grid gap-2 text-sm">
            <div className="flex items-center justify-between"><span>You</span>{chip(s.me !== "NONE", s.me === "FREE_TIER" ? "Free" : "Has today's pass", "No pass yet")}</div>
            <div className="flex items-center justify-between"><span>{name}</span>{chip(themOk, s.them === "FREE_TIER" ? "Free" : "Has today's pass", "No pass yet")}</div>
          </div>
          <ul className="mt-3 list-disc pl-5 text-sm text-muted"><li>Unlimited calls with {name} until midnight</li><li>Video too, once it&rsquo;s unlocked</li><li>Chatting is always free</li></ul>
          <button type="button" disabled={busy || s.me !== "NONE"} onClick={() => void buy()}
            className="mt-4 flex min-h-14 w-full items-center justify-center gap-2 rounded-md border-2 border-bg bg-accent font-bold tracking-[0.08em] text-on-accent uppercase shadow-sticker disabled:opacity-50">
            Get today&rsquo;s pass · 10 <Heart size={16} />
          </button>
          {error === "hearts" ? (
            <p role="alert" className="mt-2 text-sm">Not enough Hearts. <Link href="/hearts" className="font-bold text-accent underline">Get 10 for ₹49</Link></p>
          ) : error && <p role="alert" className="mt-2 text-sm text-danger">{error}</p>}
        </div>
      </motion.div>
    </motion.div>
  );
}

/** "Day 4: video's on us": once per day, when video just unlocked and today's video is free. */
export function FreeVideoMoment({ matchId, name, s, onVideo }: { matchId: string; name: string; s: CallStatus; onVideo: () => void }) {
  const reduce = useReducedMotion();
  const key = `sl.freevideo.${matchId}.${new Date().toISOString().slice(0, 10)}`;
  const [show, setShow] = useState(false);
  useEffect(() => {
    if (!s.videoUnlocked || !s.freeVideoToday || s.consent.allowed !== "VIDEO") return;
    try {
      if (localStorage.getItem(key)) return;
    } catch { /* private mode: show anyway */ }
    // remember "seen today" only once it's really on screen (a status refresh may cancel this timer)
    const id = window.setTimeout(() => {
      setShow(true);
      try { localStorage.setItem(key, "1"); } catch { /* private mode */ }
    }, 400);
    return () => window.clearTimeout(id);
  }, [s.videoUnlocked, s.freeVideoToday, s.consent.allowed, key]);
  return (
    <AnimatePresence>
      {show && (
        <motion.div role="dialog" aria-modal="true" aria-labelledby="fv-h" className="fixed inset-0 z-50 flex flex-col items-center bg-ember px-5 pb-[max(1.5rem,env(safe-area-inset-bottom))] pt-[max(1.75rem,env(safe-area-inset-top))] text-center"
          initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
          <span className="rounded-full bg-bg px-3 py-1 text-xs font-bold tracking-[0.08em] text-accent uppercase">🎥 Unlocked</span>
          <div className="mt-6 flex gap-2">
            {["✓", "✓", "✓", "🎥"].map((t, i) => (
              <motion.span key={i} initial={reduce ? false : { scale: 0 }} animate={{ scale: 1 }} transition={{ ...spring.snappy, delay: 0.15 * i }}
                className={`grid size-14 place-items-center rounded-full border-[3px] border-bg font-extrabold text-on-accent shadow-[4px_4px_0_var(--bg)] ${i === 3 ? "bg-accent" : "bg-accent-2"}`}>{t}</motion.span>
            ))}
          </div>
          <div className="mt-6 rounded-lg bg-bg px-5 py-4">
            <h2 id="fv-h" className="font-display text-6xl leading-[0.9]">Day 4:<br /><span className="text-accent">video&rsquo;s on us</span></h2>
            <p className="mt-2">You and {name} called on 3 different days. Today, video calls are free for both of you.</p>
          </div>
          <button type="button" onClick={() => { setShow(false); onVideo(); }} className="mt-auto min-h-14 w-full max-w-md rounded-md border-2 border-bg bg-bg font-bold tracking-[0.08em] uppercase shadow-sticker">🎥 Video call {name}</button>
          <button type="button" onClick={() => setShow(false)} className="mt-3 rounded-full bg-accent-2 px-4 py-2 font-bold text-on-accent">Maybe later</button>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
