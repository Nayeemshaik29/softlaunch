"use client";

import dynamic from "next/dynamic";
import { useRouter } from "next/navigation";
import { animate, motion, useMotionValue, useReducedMotion } from "framer-motion";
import { useCallback, useEffect, useRef, useState } from "react";
import { Button } from "@/components/ui/Button";
import { canRun3D } from "@/lib/device";
import type { MatchMomentData } from "@/lib/match-moment";
import { spring } from "@/lib/motion";
import { haptic, onBackButton, protectScreen } from "@/lib/native";
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
  const [wants3D] = useState(() => !reduce && canRun3D());
  // The 3D scene can fail or be very slow (weak phones, emulators): after 2.5s we fall back to the photo version,
  // so the match screen is never left black.
  const [fallback, setFallback] = useState(false);
  const cinematic = wants3D && !fallback;
  const [started, setStarted] = useState(!wants3D);
  useEffect(() => {
    if (!wants3D || started) return;
    const id = window.setTimeout(() => {
      setFallback(true);
      setStarted(true);
    }, 2500);
    return () => window.clearTimeout(id);
  }, [wants3D, started]);
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
        void haptic("match"); // phone apps: a real haptic tap
        if (soundRef.current) chime();
      }, IMPACT_MS));
    } else if (soundRef.current) {
      chime();
    }
    timers.push(window.setTimeout(() => setCanSkip(true), SKIP_AFTER_MS));
    return () => timers.forEach(window.clearTimeout);
  }, [started, cinematic, shakeX, shakeY]);

  // apps: Android back closes this; no screenshots of the two photos (Android blocks, iOS hides)
  useEffect(() => {
    const off = onBackButton(close);
    void protectScreen(true);
    return () => { off(); void protectScreen(false); };
  }, [close]);

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
      className="fixed inset-0 z-50 flex flex-col bg-bg outline-none"
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
          <div
            className="absolute inset-0"
            aria-hidden
            // soft edges: the 3D frame melts into the page instead of showing a box
            style={{
              WebkitMaskImage: "radial-gradient(ellipse 70% 70% at 50% 50%, #000 55%, transparent 100%)",
              maskImage: "radial-gradient(ellipse 70% 70% at 50% 50%, #000 55%, transparent 100%)",
            }}
          >
            <MatchScene mine={data.myPhoto} theirs={data.theirPhoto} palette={palette} skipRef={skipRef} onStart={() => setStarted(true)} />
          </div>
        ) : (
          // reduced motion / no WebGL: two photos, a simple fade
          <motion.div className="absolute inset-0 flex items-center justify-center gap-4" aria-hidden initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.25 }}>
            {[data.myPhoto, data.theirPhoto].map((src, i) => (
              <div key={i} className={`aspect-[3/4] w-[34vw] max-w-[220px] overflow-hidden border-2 border-bg bg-accent-2 p-2 pb-8 shadow-[6px_6px_0_var(--accent)] ${i === 0 ? "-rotate-6" : "rotate-6"}`}>
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
        <p className="text-xs font-bold tracking-[0.14em] text-accent uppercase">New match</p>
        <h2 id="match-title" className="mt-2 font-display text-[clamp(4rem,18vw,7.5rem)] leading-[0.86]">
          It&rsquo;s mutual<span className="text-accent">.</span>
          <span className="sr-only"> You matched with {data.name}.</span>
        </h2>
        <p className="mt-3 text-lg" aria-hidden>You + <span className="font-bold text-accent">{data.name}</span></p>
        <p className="mt-2 text-muted">{t("You both liked each other. No pressure, just say hi.", "The feeling\u2019s mutual. Go on, make the first move.")}</p>
        <p className="mx-auto mt-4 inline-block rounded-md bg-accent-2 px-3 py-2 text-sm font-semibold text-on-accent">This chat fades in 24 hours. Make it count.</p>
        <div className="mt-8 flex flex-col items-center justify-center gap-4 sm:flex-row">
          <Button ref={primary} size="lg" onClick={() => { close(); router.push(`/chat?m=${data.matchId}`); }}>
            Say something
          </Button>
          <Button variant="secondary" size="lg" onClick={close}>Keep swiping</Button>
        </div>
      </motion.div>
    </motion.div>
  );
}
