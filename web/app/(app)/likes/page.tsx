"use client";

import { motion, useReducedMotion } from "framer-motion";
import { useCallback, useEffect, useState } from "react";
import { Button, ButtonLink } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Skeleton } from "@/components/ui/Skeleton";
import { api, ApiError } from "@/lib/api";
import { useEnergy } from "@/lib/energy";
import { useMatchMoment } from "@/lib/match-moment";
import { myProfile, profileOf } from "@/lib/me";
import { reveal } from "@/lib/motion";
import { useMood } from "@/lib/mood";
import type { Likes, PublicProfile, SwipeAction, SwipeResponse } from "@/lib/types";

interface Fan { userId: string; superLike: boolean; person: PublicProfile | null }

export default function LikesPage() {
  const reduce = useReducedMotion();
  const { t } = useMood();
  const energy = useEnergy();
  const { celebrate } = useMatchMoment();
  const [data, setData] = useState<{ count: number; plus: boolean; fans: Fan[] } | null>(null);
  const [status, setStatus] = useState("");

  const load = useCallback(async () => {
    try {
      const l = await api<Likes>("/api/matches/likes");
      const people = await Promise.all(l.likes.map((x) => profileOf(x.userId)));
      setData({ count: l.count, plus: l.plus, fans: l.likes.map((x, i) => ({ ...x, person: people[i] })).filter((f) => f.person) });
    } catch {
      setData({ count: 0, plus: false, fans: [] });
      setStatus("Couldn't load your likes.");
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- first load
    void load();
  }, [load]);

  async function answer(f: Fan, direction: SwipeAction) {
    setData((d) => d && { ...d, count: d.count - 1, fans: d.fans.filter((x) => x.userId !== f.userId) });
    energy.spend();
    try {
      const r = await api<SwipeResponse>("/api/matches/swipes", { method: "POST", body: { targetUserId: f.userId, direction } });
      if (r.matched && r.matchId) {
        const me = await myProfile();
        const real = await profileOf(f.userId, true); // matched: real photos now (veil lifts)
        celebrate({ matchId: r.matchId, name: f.person!.displayName, theirPhoto: real?.photoUrls[0] ?? null, myPhoto: me?.photoUrls[0] ?? null });
      }
    } catch (e) {
      energy.refund();
      setStatus((e as ApiError).isSwipeQuota ? "You're out of energy for today. Back at midnight." : "That didn't go through. Try again.");
      void load();
    }
  }

  const item = reduce ? reveal.itemReduced : reveal.item;

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="font-display text-2xl font-normal">{t("Liked you", "They noticed you")}</h1>
      <p role="status" aria-live="polite" className="mt-2 min-h-6 text-sm text-muted">{status}</p>

      {data === null ? (
        <div className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-3">{[0, 1, 2].map((i) => <Skeleton key={i} className="aspect-[3/4]" />)}</div>
      ) : data.count === 0 ? (
        <EmptyState kind="rings" title={t("No new likes yet.", "Nobody's made a move yet.")}
          body="When someone likes you, they'll appear here." action={<ButtonLink href="/swipe" variant="secondary">Keep discovering</ButtonLink>} />
      ) : !data.plus ? (
        <>
          <p className="text-lg">
            <span className="font-display text-2xl tabular">{data.count}</span> {data.count === 1 ? "person likes" : "people like"} you.
          </p>
          <ul className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-3" aria-hidden>
            {Array.from({ length: Math.min(6, data.count) }, (_, i) => (
              <li key={i} className="aspect-[3/4] overflow-hidden rounded-lg">
                <div className="h-full w-full bg-grad-warm opacity-40 blur-xl" style={{ filter: `hue-rotate(${i * 25}deg) blur(24px)` }} />
              </li>
            ))}
          </ul>
          <div className="mt-8 flex flex-col items-start gap-2">
            <ButtonLink href="/plus">See who with Plus</ButtonLink>
            <p className="text-sm text-muted">From ₹49 a week. Or keep swiping; they may show up in your deck.</p>
          </div>
        </>
      ) : (
        <motion.ul variants={reveal.container} initial="hidden" animate="show" className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-3">
          {data.fans.map((f) => (
            <motion.li key={f.userId} variants={item} className="relative aspect-[3/4] overflow-hidden rounded-lg bg-surface-2 shadow-card">
              {f.person!.photoUrls[0] && (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={f.person!.photoUrls[0]} alt="" className={`absolute inset-0 h-full w-full object-cover ${f.person!.veiled ? "scale-110 blur-xl" : ""}`} />
              )}
              <div className="absolute inset-0 bg-scrim" aria-hidden />
              {f.superLike && <span className="absolute left-2 top-2 rounded-full bg-super px-2 py-0.5 text-xs font-semibold text-[#06121f]">Super like</span>}
              <div className="absolute inset-x-0 bottom-0 p-4 text-white">
                <p className="font-display text-lg">{f.person!.displayName}{f.person!.age ? `, ${f.person!.age}` : ""}</p>
                <div className="mt-2 flex gap-2">
                  <Button variant="secondary" className="min-h-12 flex-1 px-2 text-sm" onClick={() => void answer(f, "PASS")} aria-label={`Pass on ${f.person!.displayName}`}>Pass</Button>
                  <Button variant="secondary" className="min-h-12 flex-1 px-2 text-sm text-like" onClick={() => void answer(f, "LIKE")} aria-label={`Like ${f.person!.displayName} back`}>Like</Button>
                </div>
              </div>
            </motion.li>
          ))}
        </motion.ul>
      )}
    </div>
  );
}
