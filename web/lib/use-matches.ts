"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "./api";
import { profileOf } from "./me";
import type { AppNotification, MatchSummary, PublicProfile } from "./types";

export interface MatchItem extends MatchSummary {
  person: PublicProfile | null;
  unread: boolean; // an unread MATCH_CREATED or NEW_MESSAGE notification points at this match
  unreadNotificationIds: string[];
  hasMessages: boolean;
}

export function useMatches() {
  const [items, setItems] = useState<MatchItem[] | null>(null);
  const [error, setError] = useState(false);

  const load = useCallback(async () => {
    try {
      const [matches, notes] = await Promise.all([
        api<MatchSummary[]>("/api/matches"),
        api<AppNotification[]>("/api/notifications").catch(() => [] as AppNotification[]),
      ]);
      const people = await Promise.all(matches.map((m) => profileOf(m.otherUserId)));
      setItems(
        matches
          .map((m, i) => {
            const mine = notes.filter((n) => n.referenceId === m.matchId);
            const unreadIds = mine.filter((n) => !n.read).map((n) => n.id);
            return {
              ...m,
              person: people[i],
              unread: unreadIds.length > 0,
              unreadNotificationIds: unreadIds,
              hasMessages: mine.some((n) => n.type === "NEW_MESSAGE"),
            };
          })
          .filter((m) => m.person) // deleted accounts disappear
          .sort((a, b) => Date.parse(b.matchedAt) - Date.parse(a.matchedAt)),
      );
      setError(false);
    } catch {
      setError(true);
      setItems((i) => i ?? []);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- first load
    void load();
  }, [load]);

  const markRead = useCallback(async (m: MatchItem) => {
    setItems((list) => list?.map((x) => (x.matchId === m.matchId ? { ...x, unread: false, unreadNotificationIds: [] } : x)) ?? null);
    await Promise.all(m.unreadNotificationIds.map((id) => api(`/api/notifications/${id}/read`, { method: "PATCH" }).catch(() => {})));
  }, []);

  return { items, error, reload: load, markRead };
}
