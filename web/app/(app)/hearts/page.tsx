"use client";

import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import { useEffect, useState } from "react";
import { CrownSheet } from "@/components/hearts/CrownSheet";
import { Crown, Heart, WalletPill } from "@/components/hearts/Heart";
import { CheckoutSheet } from "@/components/pay/Checkout";
import { Skeleton } from "@/components/ui/Skeleton";
import { api } from "@/lib/api";
import { useWallet } from "@/lib/hearts";
import { buyingBlockedInApp } from "@/lib/native";
import { rupees } from "@/lib/plus";
import type { Plan } from "@/lib/types";

const SPEND = [
  { icon: "👑", name: "King of the Hill", what: "Top of everyone's deck nearby, 30 min", key: "CROWN" },
  { icon: "💌", name: "Note with a like", what: "They read it; they reply only by liking back", key: "NOTE" },
  { icon: "🎁", name: "Gift", what: "An animated gift in chat", key: "GIFT" },
  { icon: "⏳", name: "Keep chat", what: "24 more hours before it fades", key: "KEEP_CHAT" },
  { icon: "📞", name: "Call day pass", what: "Calls with one match until midnight", key: "CALL_PASS" },
] as const;
const TAG: Record<string, string> = { "hearts-30": "SAVE 12%", "hearts-80": "SAVE 24%", "hearts-200": "BEST VALUE" };
const REASON: Record<string, string> = {
  monthly: "Monthly Hearts", plus_bonus: "Plus bonus", crown: "Crown", crown_free: "Free crown", note: "Note",
  gift: "Gift", keep_chat: "Keep chat", call_pass: "Call pass",
};

export default function HeartsPage() {
  const reduce = useReducedMotion();
  const { wallet, refresh } = useWallet();
  const [packs, setPacks] = useState<Plan[] | null>(null);
  const [chosen, setChosen] = useState("hearts-30");
  const [paying, setPaying] = useState<Plan | null>(null);
  const [crown, setCrown] = useState(false);

  useEffect(() => {
    void api<Plan[]>("/api/users/billing/plans").then((p) => setPacks(p.filter((x) => x.kind === "hearts")));
  }, []);
  const pack = packs?.find((p) => p.id === chosen);

  return (
    <div className="mx-auto max-w-xl">
      <div className="flex items-center justify-between">
        <p className="text-xs font-bold tracking-[0.14em] text-accent uppercase">Your hearts</p>
        <motion.span key={wallet?.balance} initial={reduce ? false : { scale: 1.3 }} animate={{ scale: 1 }}><WalletPill balance={wallet?.balance ?? null} /></motion.span>
      </div>
      <h1 className="mt-2 font-display text-6xl">Get Hearts</h1>
      {wallet?.freeTier && <p className="mt-1 text-sm text-muted">You get 20 ❤ free every month with your verified Plus.</p>}

      {buyingBlockedInApp() ? (
        <p className="mt-6 rounded-lg border-2 border-line bg-surface p-5 text-muted">Hearts can&rsquo;t be bought in the app just yet. Buy them on the website and they show up here.</p>
      ) : packs === null ? (
        <div className="mt-6 grid grid-cols-2 gap-3">{[0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-28" />)}</div>
      ) : (
        <>
          <div role="radiogroup" aria-label="Choose a Hearts pack" className="mt-6 grid grid-cols-2 gap-x-3 gap-y-5">
            {packs.map((p) => {
              const sel = p.id === chosen;
              return (
                <motion.button key={p.id} type="button" role="radio" aria-checked={sel} onClick={() => setChosen(p.id)} whileTap={reduce ? undefined : { scale: 0.96 }}
                  className={`relative flex flex-col items-center gap-1 rounded-lg border-2 py-4 ${sel ? "border-accent bg-surface-2 shadow-sticker-accent" : "border-line bg-surface"}`}>
                  {TAG[p.id] && <span className="absolute -top-2.5 rounded-full bg-accent px-2 text-[10px] font-extrabold tracking-[0.08em] text-on-accent">{TAG[p.id]}</span>}
                  <span className="flex items-center gap-1"><Heart size={26} /><span className="font-display text-5xl tabular">{p.hearts}</span></span>
                  <span className="font-bold">{rupees(p.amountPaise)}</span>
                </motion.button>
              );
            })}
          </div>
          <button type="button" disabled={!pack} onClick={() => pack && setPaying(pack)}
            className="mt-5 min-h-14 w-full rounded-md border-2 border-bg bg-accent font-bold tracking-[0.08em] text-on-accent uppercase shadow-sticker">
            Buy {pack?.hearts} Hearts · {pack ? rupees(pack.amountPaise) : ""}
          </button>
        </>
      )}

      <h2 className="mt-8 text-xs font-bold tracking-[0.14em] text-muted uppercase">Spend them on</h2>
      <ul className="mt-2">
        {SPEND.map((s) => (
          <li key={s.key} className="flex items-center gap-3 border-b border-surface-2 py-3">
            <span aria-hidden className="grid size-10 place-items-center rounded-full bg-surface-2 text-xl">{s.icon}</span>
            <span className="flex-1"><b className="block">{s.name}</b><span className="text-xs text-muted">{s.what}</span></span>
            <span className="flex items-center gap-1 font-extrabold tabular">{wallet?.prices[s.key] ?? "–"}<Heart size={14} /></span>
          </li>
        ))}
      </ul>
      {wallet && (
        <button type="button" onClick={() => setCrown(true)}
          className="mt-5 flex min-h-14 w-full items-center justify-center gap-2 rounded-md border-2 border-accent bg-bg font-bold tracking-[0.08em] uppercase">
          <Crown size={22} /> {wallet.crownedUntil ? "You're wearing the crown" : "Wear the crown"}
        </button>
      )}

      {wallet && wallet.recent.length > 0 && (
        <>
          <h2 className="mt-8 text-xs font-bold tracking-[0.14em] text-muted uppercase">Recent</h2>
          <ul className="mt-2 text-sm">
            {wallet.recent.slice(0, 8).map((r, i) => (
              <li key={i} className="flex justify-between border-b border-surface-2 py-2">
                <span>{REASON[r.reason] ?? (r.reason.startsWith("buy:") ? "Bought Hearts" : r.reason)}</span>
                <span className={`font-bold tabular ${r.delta > 0 ? "text-accent" : "text-muted"}`}>{r.delta > 0 ? "+" : ""}{r.delta} ❤</span>
              </li>
            ))}
          </ul>
        </>
      )}

      <AnimatePresence>
        {paying && (
          <CheckoutSheet plan={paying} perks={[`${paying.hearts} ❤ Hearts in your wallet`, "Crowns, notes, gifts, keep-chat and call passes", "Never expire"]}
            onClose={() => setPaying(null)} onPaid={() => void refresh()} />
        )}
        {crown && wallet && <CrownSheet wallet={wallet} onClose={() => setCrown(false)} onChange={() => void refresh()} />}
      </AnimatePresence>
    </div>
  );
}
