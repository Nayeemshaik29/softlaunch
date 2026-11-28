"use client";

import Link from "next/link";
import { AnimatePresence, animate, motion, useMotionValue, useReducedMotion, useTransform, type PanInfo } from "framer-motion";
import { useCallback, useEffect, useRef, useState } from "react";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Skeleton } from "@/components/ui/Skeleton";
import { api } from "@/lib/api";
import { spring } from "@/lib/motion";
import { useMood } from "@/lib/mood";
import { ago, stripEmoji } from "@/lib/time";
import type { AppNotification } from "@/lib/types";

/** One row: drag sideways past 35% (or flick) to dismiss; keyboard users get a Dismiss button. */
function Row({ n, onOpen, onDismiss }: { n: AppNotification; onOpen: () => void; onDismiss: () => void }) {
  const reduce = useReducedMotion();
  const x = useMotionValue(0);
  const dragged = useRef(false); // a drag must never also count as a click on the link
  const hint = useTransform(x, [-140, -40, 0, 40, 140], [1, 0.3, 0, 0.3, 1]);

  const end = (e: PointerEvent | MouseEvent | TouchEvent, info: PanInfo) => {
    const w = (e.target as HTMLElement).closest("li")?.getBoundingClientRect().width ?? 400;
    if (Math.abs(info.offset.x) > w * 0.35 || Math.abs(info.velocity.x) > 700) {
      void animate(x, Math.sign(info.offset.x || info.velocity.x) * w, { ...spring.gentle, velocity: info.velocity.x }).then(onDismiss);
    } else {
      void animate(x, 0, reduce ? { duration: 0.15 } : spring.gentle);
    }
  };

  const title = n.type === "MATCH_CREATED" ? "New match" : "New message";
  return (
    <motion.li
      layout={!reduce}
      exit={reduce ? { opacity: 0 } : { opacity: 0, height: 0, marginTop: 0 }}
      transition={spring.gentle}
      className="relative overflow-hidden rounded-md"
    >
      <motion.span aria-hidden style={{ opacity: hint }} className="absolute inset-0 flex items-center justify-between bg-surface-2 px-6 text-sm text-muted">
        <span>Dismiss</span><span>Dismiss</span>
      </motion.span>
      <motion.div
        drag="x"
        dragElastic={0.6}
        dragConstraints={{ left: 0, right: 0 }}
        dragMomentum={false}
        onDragStart={() => (dragged.current = true)}
        onPointerDown={() => (dragged.current = false)}
        onDragEnd={end}
        style={{ x }}
        className={`relative flex touch-pan-y items-center gap-4 border px-4 py-2 ${n.read ? "border-line bg-bg" : "border-transparent bg-surface"} rounded-md`}
      >
        <span aria-hidden className={`h-2.5 w-2.5 shrink-0 rounded-full ${n.read ? "bg-transparent" : "bg-accent"}`} />
        <Link href={`/chat?m=${n.referenceId}`} onClick={(e) => (dragged.current ? e.preventDefault() : onOpen())} className="min-h-14 min-w-0 flex-1 py-2" draggable={false}>
          <span className={`block ${n.read ? "text-muted" : "font-semibold text-text"}`}>{title}</span>
          <span className="block truncate text-sm text-muted">{stripEmoji(n.message)}</span>
        </Link>
        <span className="text-xs text-muted">{ago(n.createdAt)}</span>
        <button type="button" onClick={onDismiss} aria-label={`Dismiss: ${title}`}
          className="grid h-12 w-12 shrink-0 place-items-center rounded-md text-muted hover:bg-surface-2 hover:text-text">
          <svg aria-hidden width="16" height="16" viewBox="0 0 16 16"><path d="M4 4l8 8M12 4l-8 8" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" /></svg>
        </button>
      </motion.div>
    </motion.li>
  );
}

export default function NotificationsPage() {
  const { t } = useMood();
  const [list, setList] = useState<AppNotification[] | null>(null);
  const [error, setError] = useState(false);
  const [status, setStatus] = useState("");

  const load = useCallback(async () => {
    try {
      setList(await api<AppNotification[]>("/api/notifications"));
      setError(false);
    } catch {
      setError(true);
      setList((l) => l ?? []);
    }
  }, []);
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- first load
    void load();
  }, [load]);

  const open = (n: AppNotification) => {
    if (!n.read) void api(`/api/notifications/${n.id}/read`, { method: "PATCH" }).catch(() => {});
  };

  const dismiss = async (n: AppNotification) => {
    const before = list;
    setList((l) => l?.filter((x) => x.id !== n.id) ?? null);
    setStatus("Notification dismissed");
    try {
      await api(`/api/notifications/${n.id}`, { method: "DELETE" });
    } catch {
      setList(before); // put it back
      setStatus("Couldn't dismiss that. Try again.");
    }
  };

  const markAll = async () => {
    const unread = list?.filter((n) => !n.read) ?? [];
    setList((l) => l?.map((n) => ({ ...n, read: true })) ?? null);
    await Promise.all(unread.map((n) => api(`/api/notifications/${n.id}/read`, { method: "PATCH" }).catch(() => {})));
  };

  return (
    <>
      <div className="flex items-center justify-between gap-4">
        <h1 className="font-display text-2xl">Activity</h1>
        {list?.some((n) => !n.read) && <Button variant="ghost" onClick={() => void markAll()}>Mark all read</Button>}
      </div>
      <p role="status" aria-live="polite" className="sr-only">{status}</p>

      {list === null ? (
        <div className="mt-6 flex flex-col gap-2" aria-busy="true" aria-label="Loading activity">
          {Array.from({ length: 5 }, (_, i) => <Skeleton key={i} className="h-16 w-full" />)}
        </div>
      ) : error && list.length === 0 ? (
        <EmptyState kind="moon" title="We couldn't load your activity." action={<Button variant="secondary" onClick={() => void load()}>Try again</Button>} />
      ) : list.length === 0 ? (
        <EmptyState kind="moon" title={t("All quiet.", "Quiet night so far.")} body={t("New matches and messages will show up here.", "Matches and messages will light up here.")} />
      ) : (
        <>
          <p className="mt-2 text-sm text-muted">Swipe a row sideways to dismiss it.</p>
          <ul className="mt-6 flex flex-col gap-2">
            <AnimatePresence initial={false}>
              {list.map((n) => <Row key={n.id} n={n} onOpen={() => open(n)} onDismiss={() => void dismiss(n)} />)}
            </AnimatePresence>
          </ul>
        </>
      )}
    </>
  );
}
