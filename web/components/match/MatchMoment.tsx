"use client";

import dynamic from "next/dynamic";
import { useRouter } from "next/navigation";
import { animate, motion, useMotionValue, useReducedMotion } from "framer-motion";
import { useCallback, useEffect, useRef, useState } from "react";
import { Button } from "@/components/ui/Button";
import { canRun3D } from "@/lib/device";
import type { MatchMomentData } from "@/lib/match-moment";
import { spring } from "@/lib/motion";
import { useMood } from "@/lib/mood";

const MatchScene = dynamic(() => import("./MatchScene"), { ssr: false });
const IMPACT_MS = 900; // keep in sync with MatchScene.IMPACT
const SKIP_AFTER_MS = 1200;
const SOUND_KEY = "softlaunch.sound";

/** Soft two-note chime made with Web Audio (no file to download). */
function chime() {
  try {
    const Ctx = window.AudioContext ?? (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    const ctx = new Ctx();
    const now = ctx.currentTime;
    [659.25, 987.77].forEach((f, i) => {
      const o = ctx.createOscillator();
      const g = ctx.createGain();
      o.type = "sine";
      o.frequency.value = f;
      g.gain.setValueAtTime(0, now + i * 0.09);
      g.gain.linearRampToValueAtTime(0.07, now + i * 0.09 + 0.03);
      g.gain.exponentialRampToValueAtTime(0.0001, now + i * 0.09 + 1.4);
      o.connect(g).connect(ctx.destination);
      o.start(now + i * 0.09);
      o.stop(now + i * 0.09 + 1.5);
    });
    window.setTimeout(() => void ctx.close(), 2000);
  } catch {
    /* no audio: fine */
  }
}

export default function MatchMoment({ data, onClose }: { data: MatchMomentData; onClose: () => void }) {
  const router = useRouter();
  const reduce = useReducedMotion();
  const { t, palette } = useMood();
  const [cinematic] = useState(() => !reduce && canRun3D());
  const [started, setStarted] = useState(!cinematic);
  const [canSkip, setCanSkip] = useState(false);
  const [skipped, setSkipped] = useState(false);
  const [sound, setSound] = useState(() => {
    try { return localStorage.getItem(SOUND_KEY) === "on"; } catch { return false; } // muted by default
  });
  const soundRef = useRef(sound);
  useEffect(() => { soundRef.current = sound; }, [sound]);
  const skipRef = useRef<() => void>(() => {});
  const shakeX = useMotionValue(0);
  const shakeY = useMotionValue(0);
  const primary = useRef<HTMLButtonElement>(null);
  const dialog = useRef<HTMLDivElement>(null);

  const close = useCallback(() => onClose(), [onClose]);

  // impact effects, timed from when the scene really starts (after photos load)
  useEffect(() => {
    if (!started) return;
    const timers: number[] = [];
    if (cinematic) {
      timers.push(window.setTimeout(() => {
        // "haptic-style" micro-shake of the whole screen + a real tap on phones that support it
        void animate(shakeX, [0, -6, 5, -3, 2, 0], { duration: 0.35 });
        void animate(shakeY, [0, 3, -2, 1, 0], { duration: 0.35 });
        try { navigator.vibrate?.(25); } catch { /* not supported */ }
        if (soundRef.current) chime();
      }, IMPACT_MS));
    } else if (soundRef.current) {
      chime();
    }
    timers.push(window.setTimeout(() => setCanSkip(true), SKIP_AFTER_MS));
    return () => timers.forEach(window.clearTimeout);
  }, [started, cinematic, shakeX, shakeY]);

  // focus, Escape, keep Tab inside the dialog
  useEffect(() => {
    const prev = document.activeElement as HTMLElement | null;
    dialog.current?.focus();
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") { e.preventDefault(); close(); }
      if (e.key === "Tab" && dialog.current) {
        const f = dialog.current.querySelectorAll<HTMLElement>("button:not([disabled])");
        if (!f.length) return;
        const first = f[0], last = f[f.length - 1];
        if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
        else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
      }
    };
    window.addEventListener("keydown", onKey);
    const overflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = overflow;
      prev?.focus?.();
    };
  }, [close]);

  useEffect(() => {
    if (canSkip) primary.current?.focus();
  }, [canSkip]);

  const toggleSound = () => {
    const next = !sound;
    setSound(next);
    try { localStorage.setItem(SOUND_KEY, next ? "on" : "off"); } catch { /* ignore */ }
  };

  const textDelay = cinematic && !skipped ? 1.0 : 0.1;

  return (
    <motion.div
      ref={dialog}
      role="dialog"
      aria-modal="true"
      aria-labelledby="match-title"
      tabIndex={-1}
      className="fixed inset-0 z-50 flex flex-col bg-bg/95 outline-none backdrop-blur-md"
      style={{ x: shakeX, y: shakeY }}
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: reduce ? 0.2 : 0.3 }}
    >
      <div className="flex items-center justify-between px-4 py-4 sm:px-6">
        <button
          type="button"
          onClick={toggleSound}
          aria-pressed={sound}
          className="inline-flex min-h-12 items-center gap-2 rounded-md px-4 text-sm text-muted hover:text-text"
        >
          <svg aria-hidden width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round">
            <path d="M3 8v4h3l4 3V5L6 8H3z" fill="currentColor" stroke="none" />
            {sound ? <path d="M13 7.5a3.5 3.5 0 010 5M15 5.5a6.5 6.5 0 010 9" /> : <path d="M13.5 8l4 4M17.5 8l-4 4" />}
          </svg>
          {sound ? "Sound on" : "Sound off"}
        </button>
        <div className="flex items-center gap-2">
          {cinematic && canSkip && !skipped && (
            <button type="button" onClick={() => { skipRef.current(); setSkipped(true); }} className="min-h-12 rounded-md px-4 text-sm text-muted hover:text-text">
              Skip
            </button>
          )}
          <button type="button" onClick={close} aria-label="Close" className="grid h-12 w-12 place-items-center rounded-md text-muted hover:text-text">
            <svg aria-hidden width="20" height="20" viewBox="0 0 20 20"><path d="M5 5l10 10M15 5L5 15" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" /></svg>
          </button>
        </div>
      </div>

      <div className="relative flex-1">
        {cinematic ? (
          <div className="absolute inset-0" aria-hidden>
            <MatchScene mine={data.myPhoto} theirs={data.theirPhoto} palette={palette} skipRef={skipRef} onStart={() => setStarted(true)} />
          </div>
        ) : (
          // reduced motion / no WebGL: two photos, a simple fade
          <motion.div className="absolute inset-0 flex items-center justify-center gap-4" aria-hidden initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.25 }}>
            {[data.myPhoto, data.theirPhoto].map((src, i) => (
              <div key={i} className="aspect-[3/4] w-[34vw] max-w-[220px] overflow-hidden rounded-lg bg-surface-2 shadow-card">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                {src && <img src={src} alt="" className="h-full w-full object-cover" />}
              </div>
            ))}
          </motion.div>
        )}
      </div>

      <motion.div
        className="px-6 pb-10 text-center sm:pb-16"
        initial={reduce ? { opacity: 0 } : { opacity: 0, y: 16 }}
        animate={started ? { opacity: 1, y: 0 } : undefined}
        transition={reduce ? { duration: 0.2, delay: 0.1 } : { ...spring.soft, delay: textDelay }}
      >
        <h2 id="match-title" className="font-display text-3xl font-normal sm:text-4xl">
          You matched with <span className="text-grad-warm">{data.name}</span>
        </h2>
        <p className="mt-2 text-muted">{t("You both liked each other. No pressure, just say hi.", "The feeling\u2019s mutual. Go on, make the first move.")}</p>
        <div className="mt-8 flex flex-col items-center justify-center gap-4 sm:flex-row">
          <Button ref={primary} size="lg" onClick={() => { close(); router.push(`/matches?new=${data.matchId}`); }}>
            Send a message
          </Button>
          <Button variant="secondary" size="lg" onClick={close}>Keep swiping</Button>
        </div>
      </motion.div>
    </motion.div>
  );
}
