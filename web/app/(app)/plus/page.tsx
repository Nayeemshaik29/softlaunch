"use client";

import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import Link from "next/link";
import { useEffect, useState } from "react";
import { CheckoutSheet } from "@/components/pay/Checkout";
import { Skeleton } from "@/components/ui/Skeleton";
import { Torn } from "@/components/ui/Torn";
import { api, ApiError } from "@/lib/api";
import { useEnergy } from "@/lib/energy";
import { myProfile } from "@/lib/me";
import { reveal } from "@/lib/motion";
import { isNative } from "@/lib/native";
import { rupees } from "@/lib/plus";
import type { MyProfile, Plan, PlusStatus } from "@/lib/types";

const PERKS = [
  "Chat with your matches", "See who liked you", "100 swipes a day", "5 super likes a day",
  "Rewind", "20 ❤ Hearts every month", "1 free crown a week", "Calls: ₹50 day pass, or 10 ❤",
];
const FREE_GENDERS = ["WOMAN", "FEMALE", "NON_BINARY", "OTHER"];

/** Verified women / non-binary: Plus is on us. */
function FreeForYou() {
  const reduce = useReducedMotion();
  return (
    <div className="relative -mx-4 overflow-hidden bg-burst px-5 py-10 text-center sm:mx-0 sm:rounded-lg">
      <motion.div initial={reduce ? false : { scale: 0.85, rotate: -3, opacity: 0 }} animate={{ scale: 1, rotate: 0, opacity: 1 }} transition={{ type: "spring", stiffness: 260, damping: 18 }}
        className="mx-auto max-w-md rounded-lg bg-bg px-5 py-6">
        <p className="inline-block rounded-full bg-accent px-3 py-1 text-xs font-bold tracking-[0.08em] text-on-accent uppercase">✓ Verified</p>
        <h2 className="mt-3 font-display text-6xl leading-[0.88]">Plus is<br /><span className="text-accent">on us.</span></h2>
        <p className="mt-3 text-muted">Every Plus feature is free for verified women and non-binary members, for as long as you&rsquo;re here.</p>
      </motion.div>
      <ul className="mx-auto mt-5 grid max-w-md gap-1 rounded-lg border-2 border-bg bg-accent-2 p-4 text-left text-sm font-semibold text-on-accent">
        <li>✓ See who liked you · 100 swipes · rewind</li><li>✓ 5 super likes a day</li><li>✓ Calls free, every day</li><li>✓ 20 ❤ a month for gifts and crowns</li>
      </ul>
      <Link href="/swipe" className="mx-auto mt-6 flex min-h-14 max-w-md items-center justify-center rounded-md border-2 border-bg bg-bg font-bold tracking-[0.08em] text-text uppercase shadow-sticker">Start exploring</Link>
    </div>
  );
}

/** Eligible but not verified yet: the badge unlocks free Plus. */
function VerifyToUnlock() {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const start = async () => {
    setBusy(true);
    try {
      const r = await api<{ authUrl: string }>("/api/verification/digilocker/start", { method: "POST" });
      window.location.href = r.authUrl;
    } catch (e) {
      setError((e as ApiError).message || "Verification is unavailable right now.");
      setBusy(false);
    }
  };
  return (
    <div className="rounded-lg border-2 border-accent bg-surface p-5 shadow-sticker-accent">
      <p className="text-xs font-bold tracking-[0.12em] text-accent uppercase">Plus is free for you</p>
      <h2 className="mt-2 font-display text-4xl">Get verified to unlock it</h2>
      <p className="mt-2 text-muted">Women and non-binary members get Plus free once they have the Verified badge, so nobody can fake it. It takes a minute with DigiLocker, and we never store your Aadhaar number.</p>
      <button type="button" disabled={busy} onClick={() => void start()} className="mt-4 min-h-14 w-full rounded-md border-2 border-bg bg-accent font-bold tracking-[0.08em] text-on-accent uppercase shadow-sticker disabled:opacity-60">
        {busy ? "Opening DigiLocker…" : "Verify with DigiLocker"}
      </button>
      {error && <p role="alert" className="mt-2 text-sm text-danger">{error}</p>}
    </div>
  );
}

export default function PlusPage() {
  const reduce = useReducedMotion();
  const energy = useEnergy();
  const [plans, setPlans] = useState<Plan[] | null>(null);
  const [status, setStatus] = useState<PlusStatus | null>(null);
  const [me, setMe] = useState<MyProfile | null>(null);
  const [chosen, setChosen] = useState<string>("plus-month");
  const [paying, setPaying] = useState<Plan | null>(null);
  const [message, setMessage] = useState("");

  const load = () =>
    Promise.all([api<Plan[]>("/api/users/billing/plans"), api<PlusStatus>("/api/users/me/billing"), myProfile(true)])
      .then(([p, s, m]) => { setPlans(p.filter((x) => x.kind === "plus")); setStatus(s); setMe(m); })
      .catch(() => setMessage("Couldn't load plans. Try again in a moment."));
  useEffect(() => { void load(); }, []);

  const eligible = !!me?.gender && FREE_GENDERS.includes(me.gender);
  const plan = plans?.find((p) => p.id === chosen);
  const week = plans?.find((p) => p.days === 7);

  return (
    <div className="mx-auto max-w-xl">
      {status?.freeTier ? <FreeForYou /> : (
        <>
          <div className="relative -mx-4 bg-burst px-5 pb-8 pt-6 sm:mx-0 sm:rounded-t-lg">
            <p className="text-xs font-bold tracking-[0.14em] text-on-accent uppercase">SoftLaunch</p>
            <motion.h1 initial={reduce ? false : { y: 12, opacity: 0 }} animate={{ y: 0, opacity: 1 }} className="font-display text-[6rem] leading-[0.85] text-on-accent">Plus</motion.h1>
            <p className="mt-1 font-bold text-on-accent">Everything, from ₹49.</p>
            <Torn />
          </div>
          <p role="status" aria-live="polite" className="mt-3 min-h-5 text-sm">{message}</p>

          {eligible && !me?.verified && <div className="mb-6"><VerifyToUnlock /></div>}

          {status?.plus && status.activeUntil && (
            <p className="mb-4 rounded-md border-2 border-accent bg-surface px-4 py-3 text-sm">
              You&rsquo;re on Plus until <b>{new Date(status.activeUntil).toLocaleDateString("en-IN", { day: "numeric", month: "long", year: "numeric" })}</b>. Buying again adds time.
            </p>
          )}

          <motion.ul variants={reveal.container} initial="hidden" animate="show" className="grid grid-cols-2 gap-x-3 gap-y-1.5 text-sm">
            {PERKS.map((p) => (
              <motion.li key={p} variants={reduce ? reveal.itemReduced : reveal.item} className="flex gap-2"><span className="font-bold text-accent">✓</span>{p}</motion.li>
            ))}
          </motion.ul>

          {isNative() ? (
            // Store rules: digital purchases inside the apps must use Apple IAP / Google Play Billing (coming next).
            <p className="mt-8 rounded-lg border-2 border-line bg-surface p-5 text-muted">Plus can&rsquo;t be bought in the app just yet. Buy it on the website and it works here too.</p>
          ) : plans === null ? (
            <div className="mt-6 grid gap-3">{[0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-16" />)}</div>
          ) : (
            <>
              <div role="radiogroup" aria-label="Choose a pass" className="mt-6 grid gap-3">
                {plans.map((p) => {
                  const sel = p.id === chosen;
                  const perWeek = Math.round((p.amountPaise / p.days) * 7);
                  const saving = week && p.days > 7 ? Math.round(100 - (perWeek / week.amountPaise) * 100) : 0;
                  return (
                    <motion.button key={p.id} type="button" role="radio" aria-checked={sel} onClick={() => setChosen(p.id)}
                      whileTap={reduce ? undefined : { scale: 0.98 }}
                      className={`flex min-h-16 items-center justify-between rounded-md border-2 px-4 text-left transition-shadow ${sel ? "border-accent bg-surface-2 shadow-sticker-accent" : "border-line bg-surface"}`}>
                      <span>
                        <b className="block">{p.name.replace("Plus · ", "")}</b>
                        {p.days === 30 ? <span className="text-[11px] font-bold tracking-[0.1em] text-accent">MOST CHOSEN</span>
                          : saving > 0 ? <span className="text-[11px] font-bold tracking-[0.1em] text-accent">SAVE {saving}%</span> : null}
                      </span>
                      <span className="font-display text-4xl tabular">{rupees(p.amountPaise)}</span>
                    </motion.button>
                  );
                })}
              </div>
              <button type="button" disabled={!plan} onClick={() => plan && setPaying(plan)}
                className="mt-5 min-h-14 w-full rounded-md border-2 border-bg bg-accent font-bold tracking-[0.08em] text-on-accent uppercase shadow-sticker">
                Get Plus · {plan ? rupees(plan.amountPaise) : ""}
              </button>
              <p className="mt-3 text-center text-xs text-muted">One-time payment, no auto-renew. Women and non-binary members get Plus free once verified. Privacy and safety stay free for everyone.</p>
            </>
          )}
        </>
      )}

      <AnimatePresence>
        {paying && (
          <CheckoutSheet plan={paying} perks={[`${paying.name}`, `+${paying.hearts} ❤ Hearts`, "See who liked you · 100 swipes · rewind"]}
            onClose={() => setPaying(null)}
            onPaid={() => { void load(); void energy.refresh(); }} />
        )}
      </AnimatePresence>
    </div>
  );
}
