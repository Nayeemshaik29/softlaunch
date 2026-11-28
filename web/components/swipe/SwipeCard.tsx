"use client";

import {
  animate,
  motion,
  useMotionTemplate,
  useMotionValue,
  useMotionValueEvent,
  useReducedMotion,
  useSpring,
  useTransform,
  type MotionValue,
  type PanInfo,
} from "framer-motion";
import { forwardRef, useEffect, useImperativeHandle, useRef, useState } from "react";
import type { FeedCard, SwipeAction } from "@/lib/types";
import { spring } from "@/lib/motion";
import { useMood } from "@/lib/mood";
import { desireLabel } from "@/lib/desires";

export interface TopCardHandle {
  /** Buttons / keyboard: throw the card as if it was swiped. */
  fling: (action: SwipeAction) => void;
}

const THRESHOLD = 110; // px of drag that counts as a decision
const FLICK = 600; // px/s release speed that counts as a decision, even for a short drag

/* ----------------------------------------------------------- photo carousel */

function PhotoCarousel({ card, draggedRef }: { card: FeedCard; draggedRef: React.MutableRefObject<boolean> }) {
  const photos = card.photoUrls.length ? card.photoUrls : [null];
  const [i, setI] = useState(0);
  const go = (d: number) => {
    if (draggedRef.current) return; // the pointer moved: that was a swipe, not a tap
    setI((v) => Math.min(photos.length - 1, Math.max(0, v + d)));
  };
  return (
    <>
      {photos[i] ? (
        // eslint-disable-next-line @next/next/no-img-element -- signed, short-lived links; no optimizer needed
        <img
          src={photos[i]!}
          alt={`${card.displayName}, photo ${i + 1} of ${photos.length}`}
          className={`absolute inset-0 h-full w-full select-none object-cover ${card.veiled ? "scale-110 blur-xl" : ""}`}
          draggable={false}
        />
      ) : (
        card.veiled ? (
          // veiled, links not ready yet: a soft wash of the brand colours
          <div className="absolute inset-0 bg-grad-warm opacity-60 blur-2xl" aria-hidden />
        ) : (
          <div className="absolute inset-0 grid place-items-center bg-surface-2 font-display text-4xl text-muted" aria-hidden>
            {card.displayName.slice(0, 1)}
          </div>
        )
      )}
      {photos.length > 1 && (
        <>
          {/* segmented progress bar */}
          <div className="absolute inset-x-3 top-3 z-10 flex gap-1" aria-hidden>
            {photos.map((_, k) => (
              <span key={k} className={`h-1 flex-1 rounded-full ${k === i ? "bg-white" : "bg-white/35"}`} />
            ))}
          </div>
          {/* tap zones: left third = previous, right third = next */}
          <button
            type="button"
            aria-label="Previous photo"
            disabled={i === 0}
            onClick={() => go(-1)}
            className="absolute left-0 top-0 z-10 h-[62%] w-1/3 cursor-w-resize disabled:cursor-default"
          />
          <button
            type="button"
            aria-label="Next photo"
            disabled={i === photos.length - 1}
            onClick={() => go(1)}
            className="absolute right-0 top-0 z-10 h-[62%] w-1/3 cursor-e-resize disabled:cursor-default"
          />
        </>
      )}
    </>
  );
}

/* ----------------------------------------------------------- stamps */

function Stamp({ label, color, opacity, className }: { label: string; color: string; opacity: MotionValue<number>; className: string }) {
  return (
    <motion.span
      aria-hidden
      style={{ opacity, color, borderColor: color }}
      className={`pointer-events-none absolute z-20 rounded-md border-4 bg-black/25 px-4 py-1 font-sans text-2xl font-semibold tracking-widest backdrop-blur-sm ${className}`}
    >
      {label}
    </motion.span>
  );
}

function StarBurst({ strength }: { strength: MotionValue<number> }) {
  const scale = useTransform(strength, [0.55, 1], [0, 1]);
  const opacity = useTransform(strength, [0.55, 0.8, 1], [0, 1, 1]);
  return (
    <motion.div aria-hidden style={{ scale, opacity }} className="pointer-events-none absolute bottom-[30%] left-1/2 z-20 h-0 w-0">
      {Array.from({ length: 10 }, (_, k) => {
        const a = (k / 10) * Math.PI * 2;
        const r = k % 2 ? 70 : 96;
        return (
          <svg key={k} width="16" height="16" viewBox="0 0 24 24" className="absolute" style={{ left: Math.cos(a) * r - 8, top: Math.sin(a) * r - 8 }}>
            <path d="M12 2l2.9 6.6 7.1.6-5.4 4.7 1.6 7L12 17.3 5.8 20.9l1.6-7L2 9.2l7.1-.6z" fill="var(--super)" />
          </svg>
        );
      })}
    </motion.div>
  );
}

/* ----------------------------------------------------------- the card */

interface Props {
  card: FeedCard;
  progress: MotionValue<number>; // 0..1 how far the top card is dragged (drives the cards behind it)
  restored: boolean; // came back after a failed request: shake gently
  onCommit: (action: SwipeAction, flown: Promise<void>) => void;
}

export const TopCard = forwardRef<TopCardHandle, Props>(function TopCard({ card, progress, restored, onCommit }, ref) {
  const reduce = useReducedMotion();
  const x = useMotionValue(0);
  const y = useMotionValue(0);
  const opacity = useMotionValue(1);
  const committed = useRef(false);
  const dragged = useRef(false);
  const [dragging, setDragging] = useState(false);

  // drag → rotation, stamps, and the cards behind
  const rotate = useTransform(x, [-320, 320], [-16, 16]);
  const likeO = useTransform(x, [30, THRESHOLD], [0, 1]);
  const passO = useTransform(x, [-THRESHOLD, -30], [1, 0]);
  const upness = useTransform<number, number>([x, y], ([a, b]) => (b < 0 && -b > Math.abs(a) * 0.8 ? Math.min(1, -b / (THRESHOLD + 30)) : 0));
  const superO = useTransform(upness, [0.2, 0.75], [0, 1]);
  const syncProgress = () => progress.set(Math.min(1, Math.hypot(x.get(), y.get()) / 160));
  useMotionValueEvent(x, "change", syncProgress);
  useMotionValueEvent(y, "change", syncProgress);

  // 3D tilt following the pointer (mouse/pen only), with a specular glare
  const tiltX = useSpring(0, { stiffness: 300, damping: 30 });
  const tiltY = useSpring(0, { stiffness: 300, damping: 30 });
  const gx = useMotionValue(50);
  const gy = useMotionValue(30);
  const glareO = useSpring(0, { stiffness: 300, damping: 30 });
  const glare = useMotionTemplate`radial-gradient(circle at ${gx}% ${gy}%, rgb(255 255 255 / 0.32), rgb(255 255 255 / 0) 45%)`;

  const onPointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (reduce || dragging || e.pointerType === "touch") return;
    const r = e.currentTarget.getBoundingClientRect();
    const px = (e.clientX - r.left) / r.width;
    const py = (e.clientY - r.top) / r.height;
    tiltY.set((px - 0.5) * 12);
    tiltX.set((0.5 - py) * 10);
    gx.set(px * 100);
    gy.set(py * 100);
    glareO.set(1);
  };
  const resetTilt = () => {
    tiltX.set(0);
    tiltY.set(0);
    glareO.set(0);
  };

  // came back after a failed swipe: a small "nope" shake
  useEffect(() => {
    if (restored && !reduce) void animate(x, [0, -14, 12, -8, 6, -3, 0], { duration: 0.45, ease: "easeOut" });
  }, [restored, reduce, x]);

  const fly = (action: SwipeAction, vx = 0, vy = 0): Promise<void> => {
    if (reduce) {
      return animate(opacity, 0, { duration: 0.18 }).then(() => undefined);
    }
    const w = window.innerWidth;
    const h = window.innerHeight;
    const tx = action === "LIKE" ? w * 1.1 : action === "PASS" ? -w * 1.1 : x.get() + vx * 0.15;
    const ty = action === "SUPER_LIKE" ? -h * 1.1 : y.get() + vy * 0.15;
    // physics from the release velocity: a fast flick leaves faster
    const opts = { type: "spring" as const, stiffness: 170, damping: 24, restDelta: 4 };
    return Promise.all([animate(x, tx, { ...opts, velocity: vx }), animate(y, ty, { ...opts, velocity: vy })]).then(() => undefined);
  };

  const commit = (action: SwipeAction, vx = 0, vy = 0) => {
    if (committed.current) return;
    committed.current = true;
    resetTilt();
    onCommit(action, fly(action, vx, vy));
  };

  useImperativeHandle(ref, () => ({
    fling: (action) => {
      // show the stamp briefly so button/keyboard swipes read the same as drags
      if (!reduce) {
        if (action === "LIKE") x.set(40);
        if (action === "PASS") x.set(-40);
        if (action === "SUPER_LIKE") y.set(-60);
      }
      commit(action, action === "LIKE" ? 900 : action === "PASS" ? -900 : 0, action === "SUPER_LIKE" ? -1100 : 0);
    },
  }));

  const onDragEnd = (_: unknown, info: PanInfo) => {
    setDragging(false);
    const { x: ox, y: oy } = info.offset;
    const { x: vx, y: vy } = info.velocity;
    const up = -oy > Math.abs(ox) * 0.8;
    if (up && (oy < -(THRESHOLD + 30) || vy < -FLICK)) return commit("SUPER_LIKE", vx, vy);
    if (ox > THRESHOLD || vx > FLICK) return commit("LIKE", vx, vy);
    if (ox < -THRESHOLD || vx < -FLICK) return commit("PASS", vx, vy);
    // not far or fast enough: spring back home, carrying the release velocity
    const back = reduce ? { duration: 0.15 } : { ...spring.gentle, velocity: vx };
    void animate(x, 0, back);
    void animate(y, 0, reduce ? { duration: 0.15 } : { ...spring.gentle, velocity: vy });
  };

  return (
    <motion.article
      aria-label={`${card.displayName}${card.age ? `, ${card.age}` : ""}`}
      className="absolute inset-0 touch-none cursor-grab overflow-hidden rounded-lg bg-surface shadow-card active:cursor-grabbing"
      style={{ x, y, rotate, opacity, rotateX: tiltX, rotateY: tiltY, transformPerspective: 900, zIndex: 3 }}
      drag
      dragMomentum={false}
      dragElastic={1}
      onDragStart={() => {
        dragged.current = true;
        setDragging(true);
        resetTilt();
      }}
      onDragEnd={onDragEnd}
      onPointerDown={() => (dragged.current = false)}
      onPointerMove={onPointerMove}
      onPointerLeave={resetTilt}
    >
      <PhotoCarousel card={card} draggedRef={dragged} />
      <motion.div aria-hidden className="pointer-events-none absolute inset-0 z-10 mix-blend-soft-light" style={{ background: glare, opacity: glareO }} />
      <div aria-hidden className="pointer-events-none absolute inset-0 bg-scrim" />

      <Stamp label="LIKE" color="var(--like)" opacity={likeO} className="left-6 top-12 -rotate-12" />
      <Stamp label="PASS" color="var(--pass)" opacity={passO} className="right-6 top-12 rotate-12" />
      <Stamp label="SUPER" color="var(--super)" opacity={superO} className="bottom-[34%] left-1/2 -translate-x-1/2" />
      {!reduce && <StarBurst strength={superO} />}

      {card.veiled && <VeilBadge />}
      <CardInfo card={card} />
    </motion.article>
  );
});

/** Back of the stack: grows into place as the top card is dragged away. */
export function BackCard({ card, depth, progress }: { card: FeedCard; depth: number; progress: MotionValue<number> }) {
  const scale = useTransform(progress, (p) => 1 - 0.05 * (depth - p));
  const y = useTransform(progress, (p) => 14 * (depth - p));
  return (
    <motion.div
      aria-hidden
      className="absolute inset-0 overflow-hidden rounded-lg bg-surface shadow-card"
      style={{ scale, y, zIndex: 3 - depth }}
      initial={{ opacity: 0 }}
      animate={{ opacity: depth > 1 ? 0.6 : 1 }}
      transition={{ duration: 0.25 }}
    >
      {card.photoUrls[0] && (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={card.photoUrls[0]} alt="" className={`absolute inset-0 h-full w-full object-cover ${card.veiled ? "scale-110 blur-xl" : ""}`} draggable={false} />
      )}
      <div className="absolute inset-0 bg-scrim" />
      <CardInfo card={card} />
    </motion.div>
  );
}

/** Veiled profile: the photos are blurred copies; the real ones appear only after a match. */
function VeilBadge() {
  return (
    <div className="pointer-events-none absolute inset-x-0 top-[34%] z-10 flex flex-col items-center gap-2 text-white">
      <svg aria-hidden width="40" height="24" viewBox="0 0 40 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round">
        <path d="M2 6c6-4 12-4 18 0 6-4 12-4 18 0-1 8-5 14-11 14-3 0-5-2-7-5-2 3-4 5-7 5C7 20 3 14 2 6z" />
        <ellipse cx="11" cy="10" rx="3.2" ry="2" /><ellipse cx="29" cy="10" rx="3.2" ry="2" />
      </svg>
      <span className="rounded-full bg-black/35 px-4 py-1 text-sm font-medium backdrop-blur-sm">Photos unveil when you match</span>
    </div>
  );
}

const stripEmoji = (s: string) => s.replace(/[\p{Extended_Pictographic}️]/gu, "").trim();

function CardInfo({ card }: { card: FeedCard }) {
  const { mood } = useMood();
  const desires = mood === "after-dark" ? card.mutualDesires ?? [] : []; // desires stay in After dark
  return (
    <div className="pointer-events-none absolute inset-x-0 bottom-0 z-10 p-6 text-white">
      <div className="flex items-baseline gap-2">
        <h2 className="font-display text-2xl">{card.displayName}</h2>
        {card.age && <span className="text-xl text-white/90">{card.age}</span>}
        {card.verified && (
          <span className="ml-1 inline-flex items-center gap-1 rounded-full bg-white/20 px-2 py-0.5 text-xs font-medium backdrop-blur-sm" title="ID verified">
            <svg aria-hidden width="12" height="12" viewBox="0 0 12 12"><path d="M2.5 6.2l2.2 2.2 4.8-4.8" stroke="currentColor" strokeWidth="1.8" fill="none" strokeLinecap="round" strokeLinejoin="round" /></svg>
            Verified
          </span>
        )}
      </div>
      <p className="mt-1 text-sm text-white/85">
        {card.distanceKm <= 1 ? "Less than 1 km away" : `${card.distanceKm} km away`}
        {card.city ? ` · ${card.city}` : ""}
      </p>
      {card.bio && <p className="mt-2 line-clamp-2 text-base text-white/90">{card.bio}</p>}
      {desires.length > 0 && (
        <div className="mt-4">
          <p className="text-xs font-medium tracking-wide text-white/80 uppercase">You&rsquo;re both into</p>
          <ul className="mt-2 flex flex-wrap gap-2" aria-label="Desires you share">
            {desires.slice(0, 3).map((d) => (
              <li key={d} className="rounded-full px-3 py-1 text-xs font-semibold text-[var(--on-accent)]"
                style={{ background: "var(--grad-warm)", boxShadow: "0 0 18px -4px color-mix(in srgb, var(--grad-b) 70%, transparent)" }}>
                {desireLabel(d)}
              </li>
            ))}
          </ul>
        </div>
      )}
      {(card.sharedInterests.length > 0 || card.reasons.length > 0) && (
        <ul className="mt-4 flex flex-wrap gap-2" aria-label="Why you might click">
          {card.reasons.slice(0, 2).map((r) => (
            <li key={r} className="rounded-full bg-white/20 px-2 py-1 text-xs font-medium backdrop-blur-sm">{stripEmoji(r)}</li>
          ))}
          {card.sharedInterests.slice(0, 3).map((i) => (
            <li key={i} className="rounded-full border border-white/40 px-2 py-1 text-xs">{i.charAt(0) + i.slice(1).toLowerCase().replace(/_/g, " ")}</li>
          ))}
        </ul>
      )}
    </div>
  );
}
