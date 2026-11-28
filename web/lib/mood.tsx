"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { myProfile } from "./me";

export type Mood = "calm" | "after-dark";
const KEY = "softlaunch.mood";
const CASUAL = new Set(["HOOKUP", "FRIENDS_WITH_BENEFITS", "ONE_NIGHT_STAND", "CASUAL_DATING"]);

/** 3D scenes read their colors from here (WebGL can't use CSS variables). */
export const palettes = {
  calm: { a: "#f28c28", b: "#f6b26b", c: "#f3d9c6", glow: "#ffd2c2", core: "#f3d9c6", bg: "#0b0b0c" },
  "after-dark": { a: "#f6b26b", b: "#f28c28", c: "#f3d9c6", glow: "#ffc2cf", core: "#f28c28", bg: "#0b0608" },
} as const;

interface Ctx {
  mood: Mood;
  setMood: (m: Mood) => void;
  /** pick the copy for the current mood */
  t: (calm: string, afterDark: string) => string;
  palette: (typeof palettes)[Mood];
}
const C = createContext<Ctx | null>(null);

export function MoodProvider({ children }: { children: ReactNode }) {
  const [mood, setMoodState] = useState<Mood>("calm");

  useEffect(() => {
    let saved: string | null = null;
    try { saved = localStorage.getItem(KEY); } catch { /* ignore */ }
    if (saved === "calm" || saved === "after-dark") {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- restoring the saved choice
      setMoodState(saved);
      return;
    }
    // no choice yet: people looking for something casual start "after dark"
    void myProfile().then((p) => {
      if (p?.lookingFor?.some((i) => CASUAL.has(i))) setMoodState("after-dark");
    });
  }, []);

  useEffect(() => {
    const root = document.documentElement;
    if (mood === "after-dark") root.dataset.mood = "after-dark";
    else delete root.dataset.mood;
    return () => { delete root.dataset.mood; }; // leaving the app (e.g. landing page): back to calm
  }, [mood]);

  const setMood = useCallback((m: Mood) => {
    setMoodState(m);
    try { localStorage.setItem(KEY, m); } catch { /* ignore */ }
  }, []);

  const value = useMemo<Ctx>(
    () => ({ mood, setMood, t: (calm, dark) => (mood === "after-dark" ? dark : calm), palette: palettes[mood] }),
    [mood, setMood],
  );
  return <C.Provider value={value}>{children}</C.Provider>;
}

export function useMood() {
  const v = useContext(C);
  if (!v) throw new Error("useMood outside MoodProvider");
  return v;
}

export function MoodToggle() {
  const { mood, setMood } = useMood();
  const on = mood === "after-dark";
  return (
    <button
      type="button"
      aria-pressed={on}
      onClick={() => setMood(on ? "calm" : "after-dark")}
      title={on ? "Switch to calm" : "Switch to after dark"}
      className="inline-flex min-h-12 items-center gap-2 whitespace-nowrap rounded-md px-2 text-sm text-muted hover:text-text sm:px-4"
    >
      <svg aria-hidden width="20" height="20" viewBox="0 0 20 20">
        {on ? (
          <path d="M14.5 12.8A6 6 0 017.2 5.5a6 6 0 107.3 7.3z" fill="currentColor" />
        ) : (
          <path d="M14.5 12.8A6 6 0 017.2 5.5a6 6 0 107.3 7.3z" fill="none" stroke="currentColor" strokeWidth="1.6" />
        )}
      </svg>
      <span className="hidden md:inline">After dark</span>
      <span className="sr-only md:hidden">After dark mode</span>
    </button>
  );
}
