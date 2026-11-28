"use client";

import { useBackToClose } from "@/lib/native";
import { motion, useReducedMotion } from "framer-motion";
import { useState } from "react";
import { Heart } from "@/components/hearts/Heart";
import { FLIRTY_EMOJI, GIFTS, STICKERS } from "./Stickers";

type Tab = "stickers" | "gifts" | "emoji";

/** Flirty stickers (free), gifts (3 ❤) and a quick emoji bar. */
export function StickerPicker({ onSticker, onGift, onEmoji, onClose }: {
  onSticker: (id: string) => void; onGift: (id: string) => void; onEmoji: (e: string) => void; onClose: () => void;
}) {
  useBackToClose(onClose);
  const reduce = useReducedMotion();
  const [tab, setTab] = useState<Tab>("stickers");
  const tabBtn = (t: Tab, label: string) => (
    <button type="button" role="tab" aria-selected={tab === t} onClick={() => setTab(t)}
      className={`min-h-10 border-b-2 px-1 text-xs font-extrabold tracking-[0.1em] uppercase ${tab === t ? "border-accent text-accent" : "border-transparent text-muted"}`}>{label}</button>
  );
  return (
    <motion.div initial={reduce ? { opacity: 0 } : { y: 20, opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={reduce ? { opacity: 0 } : { y: 20, opacity: 0 }}
      className="rounded-t-lg border-2 border-b-0 border-text bg-bg">
      <div className="flex gap-1 overflow-x-auto px-3 pt-2" aria-label="Flirty emoji">
        {FLIRTY_EMOJI.map((e) => (
          <button key={e} type="button" onClick={() => onEmoji(e)} className="grid size-11 shrink-0 place-items-center rounded-md text-2xl hover:bg-surface-2" aria-label={`Add ${e}`}>{e}</button>
        ))}
      </div>
      <div role="tablist" className="flex items-center gap-4 px-4">
        {tabBtn("stickers", "Stickers")}{tabBtn("gifts", "Gifts · 3 ❤")}
        <button type="button" onClick={onClose} className="ml-auto min-h-10 px-2 text-sm text-muted" aria-label="Close stickers">✕</button>
      </div>
      <div role="tabpanel" className="grid max-h-56 grid-cols-4 gap-2 overflow-y-auto p-3">
        {Object.entries(tab === "gifts" ? GIFTS : STICKERS).map(([id, s]) => (
          <button key={id} type="button" onClick={() => (tab === "gifts" ? onGift(id) : onSticker(id))} aria-label={`Send ${s.label}${tab === "gifts" ? " gift for 3 Hearts" : ""}`}
            className="relative grid aspect-square place-items-center rounded-lg border-2 border-line bg-surface hover:border-text">
            <span className={tab === "gifts" ? "desire-bounce" : "desire-pulse"}>{s.draw(54)}</span>
            {tab === "gifts" && <span className="absolute bottom-1 right-1 flex items-center text-[10px] font-extrabold">3<Heart size={10} /></span>}
          </button>
        ))}
      </div>
    </motion.div>
  );
}
