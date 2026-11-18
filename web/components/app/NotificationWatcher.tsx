"use client";

import { useEffect } from "react";
import { api } from "@/lib/api";
import { useMatchMoment } from "@/lib/match-moment";
import { myProfile, profileOf } from "@/lib/me";
import type { AppNotification, MatchSummary } from "@/lib/types";

/**
 * Polls notifications every 20s (and when the tab comes back). A new MATCH_CREATED that we haven't celebrated
 * yet (the OTHER person swiped second) opens the match moment. Reports the unread count to the header.
 */
export function NotificationWatcher({ onUnread }: { onUnread: (n: number) => void }) {
  const { celebrate, alreadyCelebrated } = useMatchMoment();

  useEffect(() => {
    let stop = false;
    const startedAt = Date.now();

    async function check() {
      if (document.hidden) return;
      let list: AppNotification[];
      try {
        list = await api<AppNotification[]>("/api/notifications");
      } catch {
        return;
      }
      if (stop) return;
      onUnread(list.filter((n) => !n.read).length);
      // only fresh ones (last 10 minutes): old unread matches live on the Matches screen
      const fresh = list.find(
        (n) => n.type === "MATCH_CREATED" && !n.read && !alreadyCelebrated(n.referenceId) &&
          Date.parse(n.createdAt) > Math.min(startedAt, Date.now()) - 10 * 60_000,
      );
      if (!fresh) return;
      const matches = await api<MatchSummary[]>("/api/matches").catch(() => []);
      const match = matches.find((m) => m.matchId === fresh.referenceId);
      if (!match || stop) return;
      const [other, me] = await Promise.all([profileOf(match.otherUserId), myProfile()]);
      celebrate({
        matchId: match.matchId,
        name: other?.displayName ?? "someone new",
        theirPhoto: other?.photoUrls[0] ?? null,
        myPhoto: me?.photoUrls[0] ?? null,
      });
    }

    void check();
    const id = window.setInterval(check, 20_000);
    const onVisible = () => !document.hidden && void check();
    document.addEventListener("visibilitychange", onVisible);
    return () => {
      stop = true;
      window.clearInterval(id);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [celebrate, alreadyCelebrated, onUnread]);

  return null;
}
