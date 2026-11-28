"use client";

import { SwipeDeck } from "@/components/swipe/SwipeDeck";
import { RestState } from "@/components/swipe/RestState";
import Link from "next/link";
import { AnimatePresence } from "framer-motion";
import { useState } from "react";
import { CrownSheet } from "@/components/hearts/CrownSheet";
import { Crown, Heart } from "@/components/hearts/Heart";
import { useWallet } from "@/lib/hearts";
import { useEnergy } from "@/lib/energy";
import { useMood } from "@/lib/mood";

export default function SwipePage() {
  const { quota } = useEnergy();
  const { mood } = useMood();
  const pill = "inline-flex min-h-12 items-center gap-2 rounded-full border border-line px-4 text-sm text-muted hover:text-text";
  const { wallet, refresh } = useWallet();
  const [crown, setCrown] = useState(false);
  return (
    <>
      <h1 className="sr-only">Discover people near you</h1>
      <div className="mx-auto mb-4 flex max-w-[420px] justify-between gap-2">
        <Link href="/filters" className={pill}>
          <svg aria-hidden width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round"><path d="M2 4h12M4 8h8M6 12h4" /></svg>
          Filters
        </Link>
        <span className="flex gap-2">
          <button type="button" onClick={() => setCrown(true)} disabled={!wallet} aria-label="King of the Hill"
            className={`grid size-12 place-items-center rounded-full border ${wallet?.crownedUntil ? "border-accent bg-accent/15" : "border-line"}`}>
            <Crown size={22} />
          </button>
          <Link href="/hearts" aria-label={`${wallet?.balance ?? 0} Hearts`} className={`${pill} !gap-1 !px-3 font-bold !text-text`}><Heart size={16} />{wallet?.balance ?? "–"}</Link>
        </span>
        {mood === "after-dark" && (
          <Link href="/desires" className={pill}>
            <svg aria-hidden width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><circle cx="5" cy="8" r="3" /><circle cx="11" cy="8" r="3" opacity=".5" /></svg>
            Desires
          </Link>
        )}
      </div>
      <SwipeDeck />
      <AnimatePresence>{crown && wallet && <CrownSheet wallet={wallet} onClose={() => setCrown(false)} onChange={() => void refresh()} />}</AnimatePresence>
      {quota?.remaining === 0 && <RestState resetsAt={quota.resetsAt} />}
    </>
  );
}
