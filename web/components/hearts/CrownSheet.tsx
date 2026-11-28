"use client";

import { motion, useReducedMotion } from "framer-motion";
import Link from "next/link";
import { useEffect, useState } from "react";
import { ApiError } from "@/lib/api";
import { needsHearts, spendHearts } from "@/lib/hearts";
import { spring } from "@/lib/motion";
import type { Wallet } from "@/lib/types";
import { Crown, Heart } from "./Heart";

function Countdown({ until }: { until: string }) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => { const id = window.setInterval(() => setNow(Date.now()), 1000); return () => window.clearInterval(id); }, []);
  const s = Math.max(0, Math.floor((Date.parse(until) - now) / 1000));
  return <span className="tabular">{String(Math.floor(s / 60)).padStart(2, "0")}:{String(s % 60).padStart(2, "0")}</span>;
}

/** King of the Hill: 30 minutes at the top of everyone's deck nearby (15 ❤, or the weekly free crown). */
export function CrownSheet({ wallet, onClose, onChange }: { wallet: Wallet; onClose: () => void; onChange: () => void }) {
  const reduce = useReducedMotion();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [until, setUntil] = useState<string | null>(wallet.crownedUntil);
  const cost = wallet.prices.CROWN;

  const wear = async () => {
    setBusy(true);
    setError("");
    try {
      const r = await spendHearts("CROWN");
      setUntil(r.until);
      onChange();
    } catch (e) {
      setError(needsHearts(e) ? "Not enough Hearts." : (e as ApiError).message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <motion.div className="fixed inset-0 z-50 flex items-end justify-center bg-bg/75 backdrop-blur-sm sm:items-center"
      initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}>
      <motion.div role="dialog" aria-modal="true" aria-labelledby="crown-h" onClick={(e) => e.stopPropagation()}
        initial={reduce ? { opacity: 0 } : { y: 80 }} animate={{ y: 0, opacity: 1 }} exit={reduce ? { opacity: 0 } : { y: 80 }} transition={spring.gentle}
        className="w-full max-w-md overflow-hidden rounded-t-lg border-2 border-text bg-ember pb-[max(1.25rem,env(safe-area-inset-bottom))] text-center sm:rounded-lg">
        <motion.div className="mx-auto mt-8 grid size-36 place-items-center rounded-full border-[3px] border-accent-2 bg-bg shadow-[6px_6px_0_var(--bg)]"
          animate={reduce ? undefined : { rotate: [-6, 6, -6] }} transition={{ duration: 2.4, repeat: Infinity, ease: "easeInOut" }}>
          <Crown size={92} />
        </motion.div>
        <div className="mx-5 mt-5 rounded-lg bg-bg px-4 py-4">
          <h2 id="crown-h" className="font-display text-5xl leading-[0.9]">King of<br /><span className="text-accent">the Hill</span></h2>
          {until ? (
            <p className="mt-3 text-lg">You&rsquo;re on top of nearby decks for <b className="text-accent"><Countdown until={until} /></b></p>
          ) : (
            <p className="mt-3 text-muted">Your card goes to the <b className="text-text">top of everyone&rsquo;s deck</b> nearby for <b className="text-text">30 minutes</b>, with a crown on it.</p>
          )}
        </div>
        <div className="px-5 pt-4">
          {!until && (
            <button type="button" disabled={busy} onClick={() => void wear()}
              className="flex min-h-14 w-full items-center justify-center gap-2 rounded-md border-2 border-bg bg-bg font-bold tracking-[0.08em] text-text uppercase shadow-sticker disabled:opacity-60">
              {wallet.freeCrownAvailable ? "Wear your free crown" : <>Wear the crown · {cost} <Heart size={16} /></>}
            </button>
          )}
          {error && (
            <p role="alert" className="mt-3 rounded-sm bg-accent-2 px-3 py-2 text-sm font-semibold text-on-accent">
              {error} <Link href="/hearts" className="underline">Get Hearts</Link>
            </p>
          )}
          <p className="mt-3 inline-block rounded-sm bg-accent-2 px-3 py-1 text-xs font-bold text-on-accent">
            You have {wallet.balance} ❤{wallet.freeCrownAvailable ? " · 1 free crown this week" : ""}
          </p>
          <button type="button" onClick={onClose} className="mt-2 block min-h-11 w-full text-sm font-bold text-on-accent">Close</button>
        </div>
      </motion.div>
    </motion.div>
  );
}
