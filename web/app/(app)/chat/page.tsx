"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useRef, useState } from "react";
import { Avatar } from "@/components/matches/Avatar";
import { Button } from "@/components/ui/Button";
import { Skeleton } from "@/components/ui/Skeleton";
import { api, ApiError } from "@/lib/api";
import { profileOf } from "@/lib/me";
import { useMood } from "@/lib/mood";
import { getSession } from "@/lib/session";
import { protectScreen } from "@/lib/native";
import type { MatchSummary, PublicProfile } from "@/lib/types";

interface Msg { id: string; senderId: string; text: string; sentAt: string; type: "TEXT" | "SYSTEM" | null }

/** Minimal web chat (REST, refreshed every 5s). Messages fade after 24 hours (chat-service). */
export default function ChatPage() {
  return (
    <Suspense fallback={<Skeleton className="h-12 w-40" />}>
      <Chat />
    </Suspense>
  );
}

/** /chat?m=<matchId>: a query parameter (not /matches/[id]) so the phone apps can ship as static files. */
function Chat() {
  const matchId = useSearchParams().get("m") ?? "";
  const { t } = useMood();
  const me = getSession()?.userId;
  const [person, setPerson] = useState<PublicProfile | null | undefined>(undefined);
  const [msgs, setMsgs] = useState<Msg[] | null>(null);
  const [text, setText] = useState("");
  const [gone, setGone] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const end = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let stop = false;
    api<MatchSummary[]>("/api/matches").then(async (list) => {
      const m = list.find((x) => x.matchId === matchId);
      if (!m) { if (!stop) setGone(true); return; }
      const p = await profileOf(m.otherUserId);
      if (!stop) setPerson(p);
    }).catch(() => !stop && setGone(true));
    const load = () => api<Msg[]>(`/api/chats/${matchId}/messages`).then((m) => !stop && setMsgs(m)).catch((e) => {
      if (e instanceof ApiError && (e.status === 403 || e.status === 404)) setGone(true);
    });
    void load();
    const id = window.setInterval(load, 5000);
    return () => { stop = true; window.clearInterval(id); };
  }, [matchId]);

  useEffect(() => { end.current?.scrollIntoView({ block: "end" }); }, [msgs?.length]);

  // phone apps: Android blocks screenshots / recordings of chats, iOS hides them from the app switcher
  useEffect(() => {
    void protectScreen(true);
    return () => void protectScreen(false);
  }, []);

  async function send(e: React.FormEvent) {
    e.preventDefault();
    const body = text.trim();
    if (!body) return;
    setText("");
    setError(null);
    const temp: Msg = { id: `tmp-${Date.now()}`, senderId: me ?? "", text: body, sentAt: new Date().toISOString(), type: "TEXT" };
    setMsgs((m) => [...(m ?? []), temp]);
    try {
      const saved = await api<Msg>(`/api/chats/${matchId}/messages`, { method: "POST", body: { text: body } });
      setMsgs((m) => m?.map((x) => (x.id === temp.id ? saved : x)) ?? null);
    } catch {
      setMsgs((m) => m?.filter((x) => x.id !== temp.id) ?? null);
      setText(body);
      setError("Message not sent. Try again.");
    }
  }

  if (gone) {
    return (
      <div className="py-12 text-center">
        <h1 className="font-display text-xl">This chat has ended.</h1>
        <p className="mt-2 text-muted">The match was removed.</p>
        <Link href="/matches" className="mt-6 inline-flex min-h-12 items-center text-accent underline underline-offset-4">Back to matches</Link>
      </div>
    );
  }

  return (
    <div className="mx-auto flex h-[calc(100dvh-10rem)] max-w-2xl flex-col">
      <header className="flex items-center gap-4 border-b border-line pb-4">
        <Link href="/matches" aria-label="Back to matches" className="grid h-12 w-12 place-items-center rounded-md text-muted hover:text-text">
          <svg aria-hidden width="20" height="20" viewBox="0 0 20 20"><path d="M12 4l-6 6 6 6" stroke="currentColor" strokeWidth="1.8" fill="none" strokeLinecap="round" strokeLinejoin="round" /></svg>
        </Link>
        {person === undefined ? <Skeleton className="h-12 w-40" /> : (
          <>
            <Avatar src={person?.photoUrls[0]} name={person?.displayName ?? "?"} size={44} />
            <h1 className="font-display text-xl">{person?.displayName}</h1>
          </>
        )}
      </header>

      <div className="flex-1 overflow-y-auto py-4" role="log" aria-live="polite" aria-label="Messages">
        {msgs === null ? (
          <div className="flex flex-col gap-2">{[60, 40, 70].map((w, i) => <Skeleton key={i} className={`h-10 ${i % 2 ? "ml-auto" : ""}`} />)}</div>
        ) : msgs.length === 0 ? (
          <p className="py-12 text-center text-muted">{t("Say hi. Messages disappear after 24 hours, so make a plan.", "Say something bold. Messages vanish after 24 hours.")}</p>
        ) : (
          <ul className="flex flex-col gap-2">
            {msgs.map((m) => m.type === "SYSTEM" ? (
              <li key={m.id} className="py-2 text-center text-xs text-muted">{m.text}</li>
            ) : (
              <li key={m.id} className={`max-w-[80%] rounded-lg px-4 py-2 ${m.senderId === me ? "ml-auto bg-accent text-on-accent" : "bg-surface-2"}`}>
                {m.text}
              </li>
            ))}
          </ul>
        )}
        <div ref={end} />
      </div>

      <form onSubmit={send} className="flex gap-2 border-t border-line pt-4">
        <label htmlFor="msg" className="sr-only">Message</label>
        <input id="msg" value={text} onChange={(e) => setText(e.target.value)} maxLength={1000} autoComplete="off"
          placeholder="Write a message" className="min-h-12 flex-1 rounded-md border border-line bg-surface-2 px-4 outline-none placeholder:text-muted focus-visible:border-focus" />
        <Button type="submit" disabled={!text.trim()}>Send</Button>
      </form>
      {error && <p role="alert" className="mt-2 text-sm text-danger">{error}</p>}
    </div>
  );
}
