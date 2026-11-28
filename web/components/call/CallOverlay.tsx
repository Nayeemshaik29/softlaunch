"use client";

import { useEffect, useRef, useState } from "react";
import { Note } from "@/components/ui/Doodles";
import { api } from "@/lib/api";
import { endedText, useCalls } from "@/lib/calls";

/* Icons (decorative; every button has an aria-label) */
const I = {
  phone: <svg aria-hidden width="24" height="24" viewBox="0 0 24 24"><path d="M6.6 10.8a15 15 0 006.6 6.6l2.2-2.2a1 1 0 011-.25 11.4 11.4 0 003.6.6 1 1 0 011 1V20a1 1 0 01-1 1A17 17 0 013 4a1 1 0 011-1h3.5a1 1 0 011 1c0 1.25.2 2.45.6 3.6a1 1 0 01-.25 1z" fill="currentColor" /></svg>,
  cam: <svg aria-hidden width="24" height="24" viewBox="0 0 24 24"><rect x="2" y="6" width="14" height="12" rx="2" fill="currentColor" /><path d="M16 10l6-3v10l-6-3z" fill="currentColor" /></svg>,
  camOff: <svg aria-hidden width="24" height="24" viewBox="0 0 24 24"><rect x="2" y="6" width="14" height="12" rx="2" fill="currentColor" opacity=".35" /><path d="M3 3l18 18" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" /></svg>,
  mic: <svg aria-hidden width="24" height="24" viewBox="0 0 24 24"><rect x="9" y="3" width="6" height="11" rx="3" fill="currentColor" /><path d="M5 11a7 7 0 0014 0M12 18v3" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" /></svg>,
  micOff: <svg aria-hidden width="24" height="24" viewBox="0 0 24 24"><rect x="9" y="3" width="6" height="11" rx="3" fill="currentColor" opacity=".35" /><path d="M3 3l18 18" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" /></svg>,
  flip: <svg aria-hidden width="24" height="24" viewBox="0 0 24 24"><path d="M4 9a8 8 0 0114-3l2-2v6h-6l2.5-2.5A5.5 5.5 0 006.5 9M20 15a8 8 0 01-14 3l-2 2v-6h6l-2.5 2.5A5.5 5.5 0 0017.5 15" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" /></svg>,
  end: <svg aria-hidden width="28" height="28" viewBox="0 0 24 24"><path d="M3 14.5c5-4.7 13-4.7 18 0l-2.3 2.3a1 1 0 01-1.3.1l-2.3-1.6a1 1 0 01-.4-.9l.2-2a10 10 0 00-5.8 0l.2 2a1 1 0 01-.4.9l-2.3 1.6a1 1 0 01-1.3-.1z" fill="currentColor" /></svg>,
  flag: <svg aria-hidden width="18" height="18" viewBox="0 0 18 18"><path d="M4 16V2M4 3h9l-2 3 2 3H4" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" /></svg>,
};

const round = "grid size-16 place-items-center rounded-full border-2 transition-colors disabled:opacity-40";

function Video({ stream, muted, mirror, className }: { stream: MediaStream | null; muted?: boolean; mirror?: boolean; className?: string }) {
  const v = useRef<HTMLVideoElement>(null);
  useEffect(() => {
    if (v.current && v.current.srcObject !== stream) v.current.srcObject = stream;
  }, [stream]);
  return <video ref={v} autoPlay playsInline muted={muted} className={`${className} ${mirror ? "-scale-x-100" : ""}`} />;
}

/** Plays the other person's audio in audio-only calls (video calls play it through the <video>). */
function RemoteAudio({ stream }: { stream: MediaStream | null }) {
  const a = useRef<HTMLAudioElement>(null);
  useEffect(() => {
    if (a.current && a.current.srcObject !== stream) a.current.srcObject = stream;
  }, [stream]);
  return <audio ref={a} autoPlay />;
}

function Timer({ since }: { since: number | null }) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const id = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(id);
  }, []);
  if (!since) return <span>Connecting…</span>;
  const s = Math.max(0, Math.floor((now - since) / 1000));
  return <span className="tabular">{String(Math.floor(s / 60)).padStart(2, "0")}:{String(s % 60).padStart(2, "0")}</span>;
}

const REASONS: [string, string][] = [
  ["INAPPROPRIATE_CONTENT", "Showed something inappropriate"],
  ["HARASSMENT", "Harassment or pressure"],
  ["SCAM", "Asked for money"],
  ["THREAT_OF_VIOLENCE", "Threats"],
  ["UNDERAGE", "Looks under 18"],
  ["OTHER", "Something else"],
];

function ReportSheet({ userId, matchId, onDone }: { userId: string; matchId: string; onDone: (msg: string | null) => void }) {
  const [busy, setBusy] = useState(false);
  const report = async (reason: string) => {
    setBusy(true);
    try {
      await api("/api/safety/reports", { method: "POST", body: { reportedUserId: userId, reason, matchId, details: "Reported during a call" } });
      onDone("Thanks. We've ended the call and our team will look at it.");
    } catch (e) {
      onDone((e as Error).message);
    }
  };
  return (
    <div role="dialog" aria-modal="true" aria-labelledby="report-title" className="absolute inset-x-0 bottom-0 z-20 rounded-t-lg border-t-2 border-text bg-bg p-6">
      <h2 id="report-title" className="font-display text-3xl">Report and end call</h2>
      <p className="mt-1 text-sm text-muted">Calls aren&rsquo;t recorded, so tell us what happened.</p>
      <div className="mt-4 grid gap-2">
        {REASONS.map(([k, label]) => (
          <button key={k} type="button" disabled={busy} onClick={() => void report(k)}
            className="min-h-12 rounded-md border-2 border-line px-4 text-left font-medium hover:border-text disabled:opacity-50">{label}</button>
        ))}
        <button type="button" onClick={() => onDone(null)} className="min-h-12 text-sm text-muted">Cancel</button>
      </div>
    </div>
  );
}

/** Incoming, ringing, in-call (video or audio) and "call ended" screens. Mounted once in the app layout. */
export function CallOverlay() {
  const c = useCalls();
  const s = c.state;
  const [reporting, setReporting] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const answerBtn = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (s.phase === "incoming") answerBtn.current?.focus();
  }, [s.phase]);

  // "Call ended" note disappears by itself
  useEffect(() => {
    if (s.phase !== "ended") return;
    const id = window.setTimeout(() => c.dismiss(), 3500);
    return () => window.clearTimeout(id);
  }, [s.phase, c]);

  if (s.phase === "idle") return null;

  if (s.phase === "ended") {
    return (
      <div role="status" className="fixed inset-x-4 bottom-24 z-[60] mx-auto max-w-sm rounded-md border-2 border-bg bg-accent-2 px-4 py-3 text-center font-semibold text-on-accent shadow-sticker-accent">
        {notice ?? endedText(s.endedReason)}
      </div>
    );
  }

  const name = s.peer?.name ?? "Your match";
  const photo = s.peer?.photo;

  if (s.phase === "incoming") {
    return (
      <div role="alertdialog" aria-modal="true" aria-labelledby="call-in" className="fixed inset-0 z-[60] flex flex-col items-center bg-ember px-5 pb-[max(1.5rem,env(safe-area-inset-bottom))] pt-[max(1.5rem,env(safe-area-inset-top))] text-center">
        <span className="inline-flex items-center gap-2 rounded-full bg-bg px-3 py-1.5 text-xs font-bold tracking-[0.08em] text-text uppercase">
          <span className="text-accent">{s.video ? I.cam : I.phone}</span>{s.video ? "Video call" : "Audio call"}
        </span>
        <div className="mt-10 w-48 -rotate-3 border-2 border-bg bg-accent-2 p-2.5 pb-9 shadow-[8px_8px_0_var(--bg)]">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          {photo ? <img src={photo} alt="" className="aspect-[4/5] w-full object-cover" /> : <div className="aspect-[4/5] w-full bg-surface-2" />}
          <Note className="mt-1.5 text-left !text-on-accent">{name}</Note>
        </div>
        <div className="mt-8 rounded-md bg-bg px-5 py-3">
          <h1 id="call-in" className="font-display text-5xl leading-[0.9]">{name} is<br /><span className="text-accent">calling</span></h1>
        </div>
        <div className="mt-auto flex w-full max-w-sm items-end justify-around">
          <Labeled label="Decline"><button type="button" aria-label="Decline" onClick={c.decline} className={`${round} size-[76px] border-bg bg-danger text-on-accent`}>{I.end}</button></Labeled>
          <Labeled label="Audio"><button type="button" aria-label="Answer with audio only" onClick={() => void c.accept(false)} className={`${round} border-bg bg-bg text-text`}>{I.phone}</button></Labeled>
          {s.video && (
            <Labeled label="Video"><button ref={answerBtn} type="button" aria-label="Answer with video" onClick={() => void c.accept(true)} className={`${round} size-[76px] border-bg bg-bg text-accent shadow-sticker`}>{I.cam}</button></Labeled>
          )}
        </div>
        <p className="mt-5 rounded-sm bg-accent-2 px-3 py-1.5 text-xs font-semibold text-on-accent">
          {s.video ? "Your camera stays off until you tap Video." : "Audio only. Your camera stays off."}
        </p>
      </div>
    );
  }

  const showVideo = s.video && (s.remote?.getVideoTracks().length ?? 0) > 0;
  const controls = (
    <div className="flex items-center justify-between gap-3">
      <button type="button" aria-label={s.muted ? "Unmute" : "Mute"} aria-pressed={s.muted} onClick={c.toggleMute}
        className={`${round} ${s.muted ? "border-accent bg-accent text-on-accent" : "border-text bg-surface text-text"}`}>{s.muted ? I.micOff : I.mic}</button>
      {s.video ? (
        <button type="button" aria-label={s.cameraOn ? "Turn camera off" : "Turn camera on"} aria-pressed={s.cameraOn} onClick={() => void c.toggleCamera()}
          disabled={s.phase !== "active"} className={`${round} border-text bg-surface text-text`}>{s.cameraOn ? I.cam : I.camOff}</button>
      ) : (
        <button type="button" aria-label={`Ask ${name} to turn on video`} onClick={c.askForVideo} disabled={s.phase !== "active" || s.videoAsk !== "none"}
          className={`${round} border-text bg-surface text-text`}>{I.cam}</button>
      )}
      {s.video && s.cameraOn && (
        <button type="button" aria-label="Switch camera" onClick={() => void c.flip()} className={`${round} border-text bg-surface text-text`}>{I.flip}</button>
      )}
      <button type="button" aria-label="End call" onClick={c.end} className={`${round} size-[72px] border-danger bg-danger text-on-accent`}>{I.end}</button>
    </div>
  );

  return (
    <div role="dialog" aria-modal="true" aria-label={`Call with ${name}`} className="fixed inset-0 z-[60] flex flex-col bg-bg">
      {!showVideo && <RemoteAudio stream={s.remote} />}
      {showVideo ? (
        <Video stream={s.remote} className="absolute inset-0 size-full object-cover" />
      ) : (
        <div className="flex flex-1 flex-col items-center px-5 pt-24 text-center">
          <div className="grid size-60 place-items-center rounded-full bg-[repeating-radial-gradient(circle,var(--accent)_0_6px,var(--bg)_6px_16px)]">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            {photo ? <img src={photo} alt="" className="size-[150px] rounded-full border-4 border-bg object-cover outline outline-[3px] outline-text" />
              : <span className="size-[150px] rounded-full border-4 border-bg bg-surface-2 outline outline-[3px] outline-text" />}
          </div>
          <h1 className="mt-7 font-display text-6xl">{name}</h1>
          <p className="mt-2 text-lg font-bold text-accent" aria-live="polite">{s.phase === "outgoing" ? "Calling…" : <Timer since={s.startedAt} />}</p>
        </div>
      )}

      {/* top bar */}
      <div className="relative z-10 flex items-start justify-between bg-gradient-to-b from-bg/80 to-transparent px-4 pb-8 pt-[max(1rem,env(safe-area-inset-top))]">
        <div>
          {showVideo && <p className="font-display text-3xl">{name}</p>}
          <p className="text-sm font-semibold">
            {showVideo && <><Timer since={s.startedAt} /> · </>}<span className="text-muted">{s.video ? "Video" : "Audio"} call · not recorded</span>
          </p>
        </div>
        {s.phase === "active" && (
          <button type="button" onClick={() => setReporting(true)} className="inline-flex min-h-11 items-center gap-1.5 rounded-md border-2 border-text bg-bg/60 px-3 text-xs font-bold tracking-[0.08em] uppercase">
            {I.flag} Report
          </button>
        )}
      </div>

      {/* my camera */}
      {s.cameraOn && s.local && (
        <Video stream={s.local} muted mirror className="absolute bottom-40 right-4 z-10 aspect-[3/4] w-28 rounded-lg border-2 border-text object-cover shadow-sticker-accent" />
      )}

      {/* "turn on video?" */}
      {s.videoAsk === "asked" && (
        <div role="alertdialog" aria-labelledby="vq" className="absolute inset-x-4 top-28 z-20 mx-auto max-w-sm rounded-lg border-2 border-text bg-bg p-5 shadow-sticker-accent">
          <p id="vq" className="font-display text-3xl">{name} wants to turn on video</p>
          <p className="mt-1 text-sm text-muted">Your camera stays off unless you turn it on.</p>
          <div className="mt-4 flex gap-2">
            <button type="button" onClick={() => c.answerVideo(true)} className="min-h-12 flex-1 rounded-md border-2 border-bg bg-accent font-bold text-on-accent uppercase shadow-sticker">Okay</button>
            <button type="button" onClick={() => c.answerVideo(false)} className="min-h-12 flex-1 rounded-md border-2 border-text font-bold uppercase">Not now</button>
          </div>
        </div>
      )}
      {s.videoAsk === "asking" && <p role="status" className="absolute inset-x-0 top-28 z-20 text-center text-sm font-semibold">Asking {name}…</p>}

      <div className="relative z-10 mt-auto border-t-2 border-surface-2 bg-bg/85 px-5 pb-[max(1.75rem,env(safe-area-inset-bottom))] pt-4">
        {s.phase === "outgoing" && <p className="mb-3 text-center text-sm text-muted">Ringing {name}…</p>}
        {controls}
      </div>

      {reporting && s.peer && s.matchId && (
        <ReportSheet userId={s.peer.userId} matchId={s.matchId} onDone={(msg) => {
          setReporting(false);
          if (msg) {
            setNotice(msg);
            c.end();
          }
        }} />
      )}
    </div>
  );
}

function Labeled({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-2">
      {children}
      <span aria-hidden className="rounded-sm bg-accent-2 px-2 py-0.5 text-xs font-bold text-on-accent">{label}</span>
    </div>
  );
}
