"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { motion, useReducedMotion } from "framer-motion";
import { Suspense } from "react";
import { Avatar } from "@/components/matches/Avatar";
import { Button, ButtonLink } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Skeleton } from "@/components/ui/Skeleton";
import { reveal } from "@/lib/motion";
import { useMood } from "@/lib/mood";
import { ago } from "@/lib/time";
import { useMatches } from "@/lib/use-matches";

function MatchesInner() {
  const { items, error, reload, markRead } = useMatches();
  const { t } = useMood();
  const reduce = useReducedMotion();
  const highlight = useSearchParams().get("new");

  if (items === null) {
    return (
      <div aria-busy="true" aria-label="Loading matches">
        <Skeleton className="h-8 w-48" />
        <div className="mt-6 flex gap-4 overflow-hidden">{Array.from({ length: 5 }, (_, i) => <Skeleton key={i} className="h-20 w-20 shrink-0 rounded-full" />)}</div>
        <div className="mt-8 flex flex-col gap-2">{Array.from({ length: 4 }, (_, i) => <Skeleton key={i} className="h-20 w-full" />)}</div>
      </div>
    );
  }
  if (error && items.length === 0) {
    return <EmptyState kind="rings" title="We couldn't load your matches." body="Check your connection, then try again." action={<Button variant="secondary" onClick={() => void reload()}>Try again</Button>} />;
  }
  if (items.length === 0) {
    return (
      <EmptyState kind="rings" title={t("No matches yet.", "No sparks yet.")}
        body={t("When someone you like likes you back, they'll show up here.", "When the feeling's mutual, they'll be waiting here.")}
        action={<ButtonLink href="/swipe">Discover people</ButtonLink>} />
    );
  }

  const fresh = items.filter((m) => !m.hasMessages);
  const item = reduce ? reveal.itemReduced : reveal.item;

  return (
    <>
      <h1 className="font-display text-2xl font-normal">Matches</h1>

      {fresh.length > 0 && (
        <section aria-labelledby="new-matches" className="mt-6">
          <h2 id="new-matches" className="text-sm font-medium text-muted">New matches</h2>
          <motion.ul variants={reveal.container} initial="hidden" animate="show"
            className="-mx-4 mt-4 flex snap-x gap-4 overflow-x-auto px-4 pb-2 sm:-mx-6 sm:px-6 [scrollbar-width:none]">
            {fresh.map((m) => (
              <motion.li key={m.matchId} variants={item} className="snap-start">
                <Link href={`/matches/${m.matchId}`} onClick={() => void markRead(m)}
                  className="flex w-24 flex-col items-center gap-2 rounded-md py-1 text-center"
                  aria-label={`${m.person!.displayName}${m.unread ? ", new" : ""}`}>
                  <Avatar src={m.person!.photoUrls[0]} name={m.person!.displayName} unread={m.unread || m.matchId === highlight} />
                  <span className="w-full truncate text-sm">{m.person!.displayName}</span>
                </Link>
              </motion.li>
            ))}
          </motion.ul>
        </section>
      )}

      <section aria-labelledby="all-matches" className="mt-8">
        <h2 id="all-matches" className="text-sm font-medium text-muted">All matches</h2>
        <motion.ul variants={reveal.container} initial="hidden" animate="show" className="mt-4 flex flex-col divide-y divide-line rounded-lg border border-line bg-surface">
          {items.map((m) => (
            <motion.li key={m.matchId} variants={item}>
              <Link href={`/matches/${m.matchId}`} onClick={() => void markRead(m)}
                className="flex min-h-20 items-center gap-4 px-4 py-2 hover:bg-surface-2">
                <Avatar src={m.person!.photoUrls[0]} name={m.person!.displayName} size={48} />
                <span className="min-w-0 flex-1">
                  <span className={`block truncate ${m.unread ? "font-semibold text-text" : "text-text"}`}>{m.person!.displayName}</span>
                  <span className={`block truncate text-sm ${m.unread ? "text-text" : "text-muted"}`}>
                    {m.hasMessages ? (m.unread ? "New message" : "Chat") : t("Say hi before the moment passes", "Make the first move")}
                  </span>
                </span>
                <span className="flex flex-col items-end gap-1 text-xs text-muted">
                  {ago(m.matchedAt)}
                  {m.unread && <span className="h-2.5 w-2.5 rounded-full bg-accent" aria-label="Unread" />}
                </span>
              </Link>
            </motion.li>
          ))}
        </motion.ul>
      </section>
    </>
  );
}

export default function MatchesPage() {
  return (
    <Suspense>
      <MatchesInner />
    </Suspense>
  );
}
