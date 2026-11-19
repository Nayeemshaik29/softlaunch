"use client";

import { motion, useMotionValue, useReducedMotion } from "framer-motion";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { api, ApiError } from "@/lib/api";
import { useEnergy } from "@/lib/energy";
import { useMatchMoment } from "@/lib/match-moment";
import { myProfile, profileOf } from "@/lib/me";
import { spring } from "@/lib/motion";
import type { FeedCard, SwipeAction, SwipeResponse } from "@/lib/types";
import { Skeleton } from "@/components/ui/Skeleton";
import { BackCard, TopCard, type TopCardHandle } from "./SwipeCard";
import { DeckEmpty } from "./DeckEmpty";

/** Warm the browser cache so the next cards' photos show instantly. */
function preload(cards: FeedCard[]) {
  for (const c of cards) {
    for (const url of c.photoUrls.slice(0, 2)) {
      const img = new Image();
      img.decoding = "async";
      img.src = url;
    }
  }
}

export function SwipeDeck() {
  const [cards, setCards] = useState<FeedCard[] | null>(null);
  const [restoredId, setRestoredId] = useState<string | null>(null);
  const [status, setStatus] = useState("");
  const [note, setNote] = useState("");
  const [loadError, setLoadError] = useState(false);
  const [needsLocation, setNeedsLocation] = useState(false);
  const progress = useMotionValue(0);
  const top = useRef<TopCardHandle>(null);
  const seen = useRef(new Set<string>());
  const energy = useEnergy();
  const { celebrate } = useMatchMoment();
  const reduce = useReducedMotion();
  const router = useRouter();
  const lastPassed = useRef<FeedCard | null>(null);

  const load = useCallback(async () => {
    try {
      const me = await myProfile();
      const radius = Math.min(100, Math.max(1, me?.maxDistanceKm ?? 25));
      const feed = await api<FeedCard[]>(`/api/discovery/feed?radiusKm=${radius}`);
      setLoadError(false);
      setNeedsLocation(false);
      setCards((prev) => {
        const have = new Set((prev ?? []).map((c) => c.userId));
        return [...(prev ?? []), ...feed.filter((c) => !have.has(c.userId) && !seen.current.has(c.userId))];
      });
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) setNeedsLocation(true); // "Set your location first"
      else setLoadError(true);
      setCards((prev) => prev ?? []);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- first load of the deck
    void load();
  }, [load]);

  // preload the next 3 cards' photos; fetch more when running low
  useEffect(() => {
    if (!cards) return;
    preload(cards.slice(1, 4));
    // eslint-disable-next-line react-hooks/set-state-in-effect -- fetching more cards when the stack runs low
    if (cards.length === 2) void load();
  }, [cards, load]);

  const onCommit = useCallback(
    (action: SwipeAction, flown: Promise<void>) => {
      const card = cards?.[0];
      if (!card) return;
      seen.current.add(card.userId);
      energy.spend();
      lastPassed.current = action === "PASS" ? card : null;
      setStatus(action === "LIKE" ? `Liked ${card.displayName}` : action === "PASS" ? `Passed on ${card.displayName}` : `Super liked ${card.displayName}`);

      // the card leaves the stack as soon as its throw finishes; the cards behind continue smoothly
      const removed = flown.then(() => {
        setCards((c) => (c && c[0]?.userId === card.userId ? c.slice(1) : c));
        progress.set(0);
        setRestoredId(null);
      });

      // optimistic: the request runs while the card flies
      api<SwipeResponse>("/api/matches/swipes", { method: "POST", body: { targetUserId: card.userId, direction: action } })
        .then(async (res) => {
          if (res.matched && res.matchId) {
            // matched now: a veiled profile's REAL photo is allowed, ask for it (the veil lifts in the match moment)
            const [me, them] = await Promise.all([myProfile(), card.veiled ? profileOf(card.userId, true) : null]);
            const theirPhoto = (card.veiled ? them?.photoUrls[0] : card.photoUrls[0]) ?? null;
            celebrate({ matchId: res.matchId, name: card.displayName, theirPhoto, myPhoto: me?.photoUrls[0] ?? null });
          }
        })
        .catch(async (err: unknown) => {
          if (err instanceof ApiError && err.status === 409) return; // already swiped (other tab): fine
          await removed;
          seen.current.delete(card.userId);
          setCards((c) => [card, ...(c ?? [])]);
          if (err instanceof ApiError && err.isSwipeQuota) {
            energy.exhausted(err.problem.resetsAt); // calm rest screen, no error toast
            return;
          }
          if (err instanceof ApiError && err.problem.code === "SUPER_LIKE_QUOTA") {
            energy.refund();
            setNote(err.problem.plus ? "That's your 5 super likes for today. Back at midnight." : "That's your super like for today. Plus gives you 5 a day.");
            void energy.refresh();
            return;
          }
          energy.refund();
          setRestoredId(card.userId);
          setStatus(`Couldn't save that swipe. ${card.displayName} is back on top. Try again.`);
        });
    },
    [cards, energy, celebrate, progress],
  );

  // keyboard: ← pass, → like, ↑ super like
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const t = e.target as HTMLElement;
      if (t.closest("input, textarea, [contenteditable], [role=dialog]") || e.metaKey || e.ctrlKey || e.altKey) return;
      const action = e.key === "ArrowRight" ? "LIKE" : e.key === "ArrowLeft" ? "PASS" : e.key === "ArrowUp" ? "SUPER_LIKE" : null;
      if (!action) return;
      e.preventDefault();
      top.current?.fling(action);
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  const outOfEnergy = energy.quota?.remaining === 0;
  const isPlus = energy.quota?.plus ?? false;

  async function rewind() {
    if (!isPlus) return router.push("/plus");
    const card = lastPassed.current;
    try {
      await api<{ targetUserId: string }>("/api/matches/swipes/rewind", { method: "POST" });
      if (card) {
        seen.current.delete(card.userId);
        setCards((c) => [card, ...(c ?? []).filter((x) => x.userId !== card.userId)]);
        setNote(`${card.displayName} is back.`);
      }
      lastPassed.current = null;
    } catch (e) {
      setNote((e as ApiError).status === 409 ? "Nothing to undo." : "Couldn't rewind. Try again.");
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-[420px] flex-col items-center">
      <p role="status" aria-live="polite" className="sr-only">{status}</p>

      <div className="relative aspect-[3/4] w-full [perspective:1200px]">
        {cards === null ? (
          <Skeleton className="absolute inset-0 rounded-lg" />
        ) : cards.length === 0 ? (
          <DeckEmpty error={loadError} needsLocation={needsLocation} onRetry={() => void load()} />
        ) : (
          <>
            {cards.slice(1, 3).map((c, i) => (
              <BackCard key={c.userId} card={c} depth={i + 1} progress={progress} />
            ))}
            <TopCard
              key={cards[0].userId + (restoredId === cards[0].userId ? "-back" : "")}
              ref={top}
              card={cards[0]}
              progress={progress}
              restored={restoredId === cards[0].userId}
              onCommit={onCommit}
            />
          </>
        )}
      </div>

      {cards && cards.length > 0 && !outOfEnergy && (
        <motion.div
          className="mt-8 flex items-center gap-6"
          initial={reduce ? { opacity: 0 } : { opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={spring.gentle}
        >
          <RoundButton label={isPlus ? "Rewind" : "Rewind (Plus)"} hint="undo your last pass" onClick={() => void rewind()} tone="super" size={48}>
            <path d="M9 7H5V3M5.5 7A8 8 0 1112 20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
          </RoundButton>
          <RoundButton label="Pass" hint="Left arrow" onClick={() => top.current?.fling("PASS")} tone="pass" size={64}>
            <path d="M7 7l10 10M17 7L7 17" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" />
          </RoundButton>
          <RoundButton label="Super like" hint="Up arrow" onClick={() => top.current?.fling("SUPER_LIKE")} tone="super" size={52}>
            <path d="M12 3.5l2.5 5.4 5.9.6-4.4 4 1.3 5.8L12 16.4l-5.3 2.9L8 13.5l-4.4-4 5.9-.6z" fill="currentColor" />
          </RoundButton>
          <RoundButton label="Like" hint="Right arrow" onClick={() => top.current?.fling("LIKE")} tone="like" size={64}>
            <path d="M12 19.5s-7-4.3-7-9.6A3.9 3.9 0 0112 7.6a3.9 3.9 0 017 2.3c0 5.3-7 9.6-7 9.6z" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinejoin="round" />
          </RoundButton>
        </motion.div>
      )}
      {note && <p role="status" className="mt-4 text-center text-sm text-text">{note}</p>}
      {energy.quota && (
        <p className="mt-4 text-xs text-muted tabular" aria-label={`${energy.quota.superLikesRemaining} super likes left today`}>
          ★ {energy.quota.superLikesRemaining}/{energy.quota.superLikeLimit} super likes today
        </p>
      )}
      <p className="mt-2 hidden text-xs text-muted sm:block">Tip: use ← → and ↑ on your keyboard.</p>
    </div>
  );
}

function RoundButton({
  label, hint, onClick, tone, size, children,
}: { label: string; hint: string; onClick: () => void; tone: "like" | "pass" | "super"; size: number; children: React.ReactNode }) {
  const reduce = useReducedMotion();
  const color = tone === "like" ? "text-like" : tone === "pass" ? "text-pass" : "text-super";
  return (
    <motion.button
      type="button"
      aria-label={label}
      title={`${label} (${hint})`}
      onClick={onClick}
      whileTap={reduce ? undefined : { scale: 0.9 }}
      whileHover={reduce ? undefined : { y: -2 }}
      transition={spring.snappy}
      className={`grid place-items-center rounded-full border border-line bg-surface shadow-card ${color}`}
      style={{ width: size, height: size }}
    >
      <svg aria-hidden width="24" height="24" viewBox="0 0 24 24">{children}</svg>
    </motion.button>
  );
}
