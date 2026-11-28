"use client";

import { motion, useReducedMotion } from "framer-motion";
import { useEffect, useRef, useState } from "react";
import { Heart } from "@/components/hearts/Heart";
import { spring } from "@/lib/motion";

/** "Note with a like" (5 ❤): they read it, and can only reply by liking you back. */
export function NoteSheet({ name, onClose, onSend }: { name: string; onClose: () => void; onSend: (note: string) => void }) {
  const reduce = useReducedMotion();
  const [text, setText] = useState("");
  const box = useRef<HTMLTextAreaElement>(null);
  useEffect(() => box.current?.focus(), []);
  const ok = text.trim().length >= 2;

  return (
    <motion.div className="fixed inset-0 z-[55] flex items-end justify-center bg-bg/75 backdrop-blur-sm sm:items-center"
      initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}>
      <motion.form role="dialog" aria-modal="true" aria-labelledby="note-h" onClick={(e) => e.stopPropagation()}
        onSubmit={(e) => { e.preventDefault(); if (ok) onSend(text.trim()); }}
        initial={reduce ? { opacity: 0 } : { y: 60 }} animate={{ y: 0, opacity: 1 }} exit={reduce ? { opacity: 0 } : { y: 60 }} transition={spring.gentle}
        className="w-full max-w-md rounded-t-lg border-2 border-text bg-bg p-5 pb-[max(1.25rem,env(safe-area-inset-bottom))] sm:rounded-lg">
        <p className="text-xs font-bold tracking-[0.14em] text-accent uppercase">💌 Like {name} with a note</p>
        <h2 id="note-h" className="mt-1 font-display text-4xl leading-[0.95]">Say something they&rsquo;ll remember</h2>
        <label htmlFor="note" className="sr-only">Your note</label>
        <textarea id="note" ref={box} value={text} onChange={(e) => setText(e.target.value.slice(0, 200))} rows={4}
          placeholder="Reference something from their profile. It works better than 'hey'."
          className="mt-3 w-full resize-none rounded-md border-2 border-text bg-bg p-3 text-base outline-none placeholder:text-muted focus-visible:border-accent" />
        <p className="text-right text-xs text-muted tabular">{text.length} / 200</p>
        <p className="mt-1 text-sm text-muted">They can read it, but can only reply if they like you back. Notes are checked for scams and abuse.</p>
        <button type="submit" disabled={!ok}
          className="mt-4 flex min-h-14 w-full items-center justify-center gap-2 rounded-md border-2 border-bg bg-accent font-bold tracking-[0.08em] text-on-accent uppercase shadow-sticker disabled:opacity-50">
          Send like + note · 5 <Heart size={16} />
        </button>
      </motion.form>
    </motion.div>
  );
}
