"use client";

import { motion, useReducedMotion } from "framer-motion";

/** Round photo; unread = a slowly turning gradient ring with a soft glow. */
export function Avatar({ src, name, size = 72, unread = false }: { src?: string | null; name: string; size?: number; unread?: boolean }) {
  const reduce = useReducedMotion();
  return (
    <span className="relative inline-grid shrink-0 place-items-center" style={{ width: size + 8, height: size + 8 }}>
      {unread && (
        <motion.span
          aria-hidden
          className="absolute inset-0 rounded-full"
          style={{
            background: "conic-gradient(from 0deg, var(--grad-a), var(--grad-b), var(--grad-c), var(--grad-a))",
            boxShadow: "0 0 18px -2px color-mix(in srgb, var(--grad-b) 55%, transparent)",
          }}
          animate={reduce ? undefined : { rotate: 360 }}
          transition={{ duration: 6, ease: "linear", repeat: Infinity }}
        />
      )}
      <span className={`relative overflow-hidden rounded-full bg-surface-2 ${unread ? "ring-2 ring-bg" : "ring-1 ring-line"}`} style={{ width: size, height: size }}>
        {src ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={src} alt="" className="h-full w-full object-cover" />
        ) : (
          <span className="grid h-full w-full place-items-center font-display text-xl text-muted">{name.slice(0, 1)}</span>
        )}
      </span>
    </span>
  );
}
