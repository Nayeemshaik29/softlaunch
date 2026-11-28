"use client";

import { Peach } from "@/components/ui/Peach";

/**
 * Flirty stickers (free) and gifts (3 ❤). Ids must match chat-service sticker/Stickers.java.
 * Suggestive, never explicit (App Store 1.1.4 / Play). All drawn here: no outside service, no tracking.
 */

const svg = (inner: React.ReactNode, size: number) => (
  <svg aria-hidden width={size} height={size} viewBox="0 0 48 48" fill="none" strokeLinecap="round" strokeLinejoin="round">{inner}</svg>
);

function Txt({ t, bg, fg, rot, size }: { t: string; bg: string; fg: string; rot: number; size: number }) {
  return (
    <span aria-hidden className="inline-block rounded-md border-2 border-bg px-2 py-1 text-center font-display leading-[0.9] shadow-[3px_3px_0_var(--bg)]"
      style={{ background: bg, color: fg, transform: `rotate(${rot}deg)`, fontSize: size * 0.34 }}>
      {t.split("\n").map((l) => <span key={l} className="block">{l}</span>)}
    </span>
  );
}

const A = "var(--accent)", P = "var(--accent-2)", K = "var(--bg)";

export const STICKERS: Record<string, { label: string; draw: (s: number) => React.ReactNode }> = {
  lips: { label: "Kiss", draw: (s) => svg(<><path d="M6 24c5-6 9-9 13-9 2 0 4 2 5 3 1-1 3-3 5-3 4 0 8 3 13 9-5 6-11 10-18 10S11 30 6 24z" fill={A} stroke={K} strokeWidth="2.5" /><path d="M6 24c6 2 12 3 18 3s12-1 18-3" stroke={K} strokeWidth="2.5" /><path d="M38 8c1-3 6-3 6 1 0 3-6 6-6 6s-6-3-6-6c0-4 5-4 6-1z" fill={P} stroke={K} strokeWidth="1.8" /></>, s) },
  uup: { label: "U up?", draw: (s) => <Txt t="U UP?" bg={A} fg="#0b0b0c" rot={-4} size={s} /> },
  fire: { label: "Hot", draw: (s) => svg(<><path d="M24 44c-8 0-14-6-14-13 0-8 7-11 8-19 5 4 7 8 7 12 2-2 3-5 3-8 6 5 10 9 10 15 0 7-6 13-14 13z" fill={A} stroke={K} strokeWidth="2.5" /><path d="M24 44c-4 0-6-3-6-6 0-4 4-6 5-9 4 2 7 6 7 9 0 4-2 6-6 6z" fill={P} stroke={K} strokeWidth="2" /></>, s) },
  comehere: { label: "Come here", draw: (s) => <Txt t={"COME\nHERE"} bg="#f3d9c6" fg="#0b0b0c" rot={3} size={s} /> },
  wink: { label: "Wink", draw: (s) => svg(<><circle cx="24" cy="24" r="19" fill={P} stroke={K} strokeWidth="2.5" /><path d="M14 20c2-2 5-2 7 0M28 21h7" stroke={K} strokeWidth="2.5" /><path d="M16 29c4 5 12 5 16 0" stroke={K} strokeWidth="2.5" /><circle cx="13" cy="27" r="3" fill={A} /><circle cx="35" cy="27" r="3" fill={A} /></>, s) },
  tonight: { label: "Tonight?", draw: (s) => <Txt t="TONIGHT?" bg="#0b0b0c" fg="#f28c28" rot={-3} size={s * 0.85} /> },
  peach: { label: "Peach", draw: (s) => <Peach size={s} /> },
  missyou: { label: "Miss you", draw: (s) => <Txt t={"MISS\nYOU"} bg={A} fg="#0b0b0c" rot={4} size={s} /> },
  moon: { label: "Goodnight", draw: (s) => svg(<><path d="M30 5a16 16 0 1013 21A13 13 0 0130 5z" fill={P} stroke={K} strokeWidth="2.5" /><path d="M8 8l1 3 3 1-3 1-1 3-1-3-3-1 3-1z" fill={A} /></>, s) },
  badidea: { label: "Bad idea?", draw: (s) => <Txt t={"BAD\nIDEA?"} bg="#f3d9c6" fg="#0b0b0c" rot={-5} size={s} /> },
  hearts: { label: "Heart", draw: (s) => svg(<><path d="M24 40C10 31 6 24 6 17a8 8 0 0118-3 8 8 0 0118 3c0 7-4 14-18 23z" fill={A} stroke={K} strokeWidth="2.5" /><path d="M16 14c-3 0-5 2-5 5" stroke={P} strokeWidth="2.5" /></>, s) },
  yourplace: { label: "Your place", draw: (s) => <Txt t={"YOUR\nPLACE"} bg="#0b0b0c" fg="#f3d9c6" rot={3} size={s * 0.85} /> },
};

export const GIFTS: Record<string, { label: string; draw: (s: number) => React.ReactNode }> = {
  rose: { label: "Rose", draw: (s) => svg(<><path d="M24 26v18M24 34c-5-1-8-4-9-8M24 38c4-1 7-3 8-7" stroke="#3a7a3a" strokeWidth="2.5" /><path d="M24 8c6 0 11 4 11 10s-5 9-11 9-11-3-11-9 5-10 11-10z" fill="#e2445c" stroke={K} strokeWidth="2.5" /><path d="M18 16c3-3 9-3 12 1M20 21c2 2 6 2 8 0" stroke={K} strokeWidth="2" /></>, s) },
  champagne: { label: "Champagne", draw: (s) => svg(<><path d="M14 6h8l-1 12a4 4 0 01-6 0zM18 18v18M13 36h10" stroke={K} strokeWidth="2.5" fill={P} /><path d="M28 6h8l-1 12a4 4 0 01-6 0zM32 18v18M27 36h10" stroke={K} strokeWidth="2.5" fill={P} /><circle cx="25" cy="4" r="1.5" fill={A} /><circle cx="22" cy="9" r="1" fill={A} /><circle cx="27" cy="10" r="1.2" fill={A} /></>, s) },
  chocolates: { label: "Chocolates", draw: (s) => svg(<><path d="M24 42C10 33 6 26 6 19a8 8 0 0118-3 8 8 0 0118 3c0 7-4 14-18 23z" fill="#6b3a1e" stroke={K} strokeWidth="2.5" /><circle cx="17" cy="21" r="3" fill={P} /><circle cx="24" cy="27" r="3" fill={A} /><circle cx="31" cy="21" r="3" fill={P} /></>, s) },
  lovenote: { label: "Love note", draw: (s) => svg(<><rect x="6" y="12" width="36" height="26" rx="3" fill={P} stroke={K} strokeWidth="2.5" /><path d="M6 14l18 14 18-14" stroke={K} strokeWidth="2.5" /><path d="M24 30c-4-3-6-5-6-7a3 3 0 016-1 3 3 0 016 1c0 2-2 4-6 7z" fill={A} stroke={K} strokeWidth="1.5" /></>, s) },
  fireworks: { label: "Fireworks", draw: (s) => svg(<><path d="M24 24L24 6M24 24L40 14M24 24L42 30M24 24L30 42M24 24L14 42M24 24L6 30M24 24L8 14" stroke={A} strokeWidth="2.5" /><circle cx="24" cy="24" r="4" fill={P} stroke={K} strokeWidth="2" /></>, s) },
  crown: { label: "Crown", draw: (s) => svg(<><path d="M8 36l-3-22 11 9 8-15 8 15 11-9-3 22z" fill={A} stroke={K} strokeWidth="2.5" /><path d="M8 42h32" stroke={K} strokeWidth="3" /></>, s) },
};

export const FLIRTY_EMOJI = ["😘", "🔥", "💋", "😏", "🫦", "😈", "🥵", "💦", "🍑", "🍒", "🫣", "😍", "🥂", "🌙", "❤️‍🔥"];

/** A sticker or gift as it appears in the chat (bigger, with a gentle loop). */
export function StickerView({ id, gift, size = 96 }: { id: string; gift?: boolean; size?: number }) {
  const s = (gift ? GIFTS : STICKERS)[id];
  if (!s) return null;
  return (
    <span role="img" aria-label={`${gift ? "Gift" : "Sticker"}: ${s.label}`} className={`inline-grid place-items-center ${gift ? "desire-bounce" : "desire-pulse"}`}>
      {s.draw(size)}
    </span>
  );
}
