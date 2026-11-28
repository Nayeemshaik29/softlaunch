"use client";

import { useEffect, useState } from "react";
import { getConsent, setConsent, useCalls, type CallMode, type Consent } from "@/lib/calls";

/** Consent for one match, kept live: when the other person changes theirs, the server pushes it. */
export function useConsent(matchId: string) {
  const { onConsent } = useCalls();
  const [consent, set] = useState<Consent | null>(null);
  useEffect(() => {
    let stop = false;
    getConsent(matchId).then((c) => !stop && set(c)).catch(() => {});
    const off = onConsent(matchId, set);
    return () => {
      stop = true;
      off();
    };
  }, [matchId, onConsent]);
  return [consent, set] as const;
}

const lock = <svg aria-hidden width="12" height="12" viewBox="0 0 12 12" className="absolute -bottom-0.5 -right-0.5"><rect x="2" y="5" width="8" height="6" rx="1" fill="currentColor" /><path d="M4 5V3.5a2 2 0 014 0V5" fill="none" stroke="currentColor" strokeWidth="1.5" /></svg>;

/** Phone + camera buttons in the chat header. Locked (with the reason) until both people said yes. */
export function CallButtons({ matchId, consent, peer, videoUnlocked = true }: {
  matchId: string; consent: Consent | null; peer: { userId: string; name: string; photo: string | null } | null; videoUnlocked?: boolean;
}) {
  const calls = useCalls();
  const audioOk = !!consent && consent.allowed !== "OFF";
  const videoOk = consent?.allowed === "VIDEO" && videoUnlocked;
  const busy = calls.state.phase !== "idle" && calls.state.phase !== "ended";
  const why = !consent || consent.allowed === "OFF" ? "Calls unlock when you both say yes"
    : consent.allowed === "VIDEO" && !videoUnlocked ? "Video unlocks after calls on 3 different days" : "Video needs both of you to allow it";
  const cls = (ok: boolean) =>
    `relative grid size-11 place-items-center rounded-full border-2 ${ok ? "border-text text-text hover:bg-surface-2" : "border-line text-muted/60"}`;
  return (
    <div className="ml-auto flex gap-2">
      <button type="button" aria-label={audioOk ? `Audio call ${peer?.name ?? ""}` : `Audio call (${why})`} title={audioOk ? "Audio call" : why}
        aria-disabled={!audioOk || busy} onClick={() => audioOk && !busy && peer && void calls.start(matchId, peer, false)} className={cls(audioOk)}>
        <svg aria-hidden width="20" height="20" viewBox="0 0 24 24"><path d="M6.6 10.8a15 15 0 006.6 6.6l2.2-2.2a1 1 0 011-.25 11.4 11.4 0 003.6.6 1 1 0 011 1V20a1 1 0 01-1 1A17 17 0 013 4a1 1 0 011-1h3.5a1 1 0 011 1c0 1.25.2 2.45.6 3.6a1 1 0 01-.25 1z" fill="currentColor" /></svg>
        {!audioOk && lock}
      </button>
      <button type="button" aria-label={videoOk ? `Video call ${peer?.name ?? ""}` : `Video call (${why})`} title={videoOk ? "Video call" : why}
        aria-disabled={!videoOk || busy} onClick={() => videoOk && !busy && peer && void calls.start(matchId, peer, true)} className={cls(videoOk)}>
        <svg aria-hidden width="20" height="20" viewBox="0 0 24 24"><rect x="2" y="6" width="14" height="12" rx="2" fill="currentColor" /><path d="M16 10l6-3v10l-6-3z" fill="currentColor" /></svg>
        {!videoOk && lock}
      </button>
    </div>
  );
}

const label: Record<CallMode, string> = { OFF: "Not yet", AUDIO: "Audio only", VIDEO: "Audio + video" };

/** "Open to a call?": each person decides for themselves; calls use only what BOTH chose. */
export function ConsentCard({ matchId, name, consent, onChange }: { matchId: string; name: string; consent: Consent | null; onChange: (c: Consent) => void }) {
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  if (!consent) return null;

  const choose = async (mode: CallMode) => {
    setBusy(true);
    setError(null);
    try {
      onChange(await setConsent(matchId, mode));
      setOpen(false);
    } catch {
      setError("That didn't save. Try again.");
    } finally {
      setBusy(false);
    }
  };

  // both said yes: a quiet line instead of the card
  if (consent.allowed !== "OFF" && !open) {
    return (
      <p className="flex items-center justify-between gap-2 border-t border-line py-2 text-xs text-muted">
        <span>Calls on: <b className="text-text">{label[consent.allowed]}</b> · never recorded</span>
        <button type="button" onClick={() => setOpen(true)} className="min-h-11 px-2 font-bold text-accent">Change</button>
      </p>
    );
  }

  const chip = (m: CallMode, dashed = false) => (
    <span className={`inline-flex min-h-7 items-center rounded-full border-[1.5px] px-3 text-xs font-semibold ${m !== "OFF" ? "border-accent bg-accent text-on-accent" : `border-text ${dashed ? "border-dashed" : ""}`}`}>
      {m !== "OFF" ? "✓ " : ""}{label[m]}
    </span>
  );

  return (
    <section aria-labelledby="calls-h" className="my-3 rounded-lg border-2 border-accent bg-surface p-4 shadow-sticker-accent">
      <p className="text-xs font-bold tracking-[0.14em] text-accent uppercase">Calls · off until you both say yes</p>
      <h2 id="calls-h" className="mt-2 font-display text-3xl">Open to a call?</h2>
      <dl className="mt-3 grid gap-2 text-sm">
        <div className="flex items-center justify-between"><dt>You</dt><dd>{chip(consent.mine)}</dd></div>
        <div className="flex items-center justify-between"><dt>{name}</dt><dd>{chip(consent.theirs, true)}</dd></div>
      </dl>
      <div className="mt-4 grid grid-cols-2 gap-2">
        <button type="button" disabled={busy} aria-pressed={consent.mine === "VIDEO"} onClick={() => void choose("VIDEO")}
          className="min-h-12 rounded-md border-2 border-bg bg-accent text-xs font-bold tracking-[0.08em] text-on-accent uppercase shadow-sticker disabled:opacity-50">Audio + video</button>
        <button type="button" disabled={busy} aria-pressed={consent.mine === "AUDIO"} onClick={() => void choose("AUDIO")}
          className="min-h-12 rounded-md border-2 border-text text-xs font-bold tracking-[0.08em] uppercase disabled:opacity-50">Audio only</button>
      </div>
      {consent.mine !== "OFF" && (
        <button type="button" disabled={busy} onClick={() => void choose("OFF")} className="mt-2 min-h-11 w-full text-sm text-muted underline underline-offset-4">Turn calls off</button>
      )}
      {error && <p role="alert" className="mt-2 text-sm text-danger">{error}</p>}
      <p className="mt-3 text-xs leading-relaxed text-muted">
        {name} only sees that you&rsquo;re open to calls, with no pressure. Change your mind anytime; calls are never recorded.
      </p>
    </section>
  );
}
