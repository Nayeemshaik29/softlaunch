"use client";

import { motion, useReducedMotion } from "framer-motion";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Skeleton } from "@/components/ui/Skeleton";
import { api, ApiError } from "@/lib/api";
import { useEnergy } from "@/lib/energy";
import { reveal } from "@/lib/motion";
import { useMood } from "@/lib/mood";
import { mockPay, rupees, startCheckout } from "@/lib/plus";
import { isNative } from "@/lib/native";
import type { Checkout, Plan, PlusStatus } from "@/lib/types";

const PERKS = [
  { title: "See who liked you", body: "No guessing. Like them back and it's a match." },
  { title: "100 swipes a day", body: "Double the daily energy. Still refills at midnight." },
  { title: "5 super likes a day", body: "Stand out to the people you really mean." },
  { title: "Rewind", body: "Passed too fast? Bring them back." },
];

export default function PlusPage() {
  const reduce = useReducedMotion();
  const { t, palette } = useMood();
  const energy = useEnergy();
  const [plans, setPlans] = useState<Plan[] | null>(null);
  const [status, setStatus] = useState<PlusStatus | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [mock, setMock] = useState<Checkout | null>(null);
  const [message, setMessage] = useState("");

  useEffect(() => {
    void Promise.all([api<Plan[]>("/api/users/billing/plans"), api<PlusStatus>("/api/users/me/billing")])
      .then(([p, s]) => { setPlans(p); setStatus(s); })
      .catch(() => setMessage("Couldn't load plans. Try again in a moment."));
  }, []);

  const done = (s: PlusStatus) => {
    setStatus(s);
    setMock(null);
    setBusy(null);
    void energy.refresh(); // 100 swipes + 5 super likes right away
    setMessage(`You're on Plus until ${new Date(s.activeUntil!).toLocaleDateString("en-IN", { day: "numeric", month: "long" })}.`);
  };

  async function buy(plan: Plan) {
    setBusy(plan.id);
    setMessage("");
    try {
      const r = await startCheckout(plan.id, palette.b);
      if ("mock" in r) setMock(r.mock);
      else if ("status" in r) done(r.status);
      else setBusy(null);
    } catch (e) {
      setMessage((e as ApiError).message);
      setBusy(null);
    }
  }

  const week = plans?.find((p) => p.days === 7);
  const item = reduce ? reveal.itemReduced : reveal.item;

  return (
    <div className="mx-auto max-w-3xl">
      <p className="text-sm font-medium tracking-wide text-accent uppercase">SoftLaunch Plus</p>
      <h1 className="mt-2 font-display text-[clamp(3rem,12vw,5rem)] leading-[0.9]">{t("A little more, for a little less.", "A little more of the night.")}</h1>
      <p className="mt-2 max-w-xl text-muted">
        Safety, privacy, Private Mode and veiled photos are free for everyone, always. Plus just gives you more room.
      </p>
      <p role="status" aria-live="polite" className="mt-4 min-h-6 text-sm text-text">{message}</p>

      {status?.plus && (
        <div className="mt-2 rounded-lg border border-accent bg-surface p-6">
          <p className="font-medium">You&rsquo;re on Plus</p>
          <p className="mt-1 text-sm text-muted">
            Until {new Date(status.activeUntil!).toLocaleDateString("en-IN", { day: "numeric", month: "long", year: "numeric" })}. Buying again adds to it.
          </p>
        </div>
      )}

      <motion.ul variants={reveal.container} initial="hidden" animate="show" className="mt-8 grid gap-4 sm:grid-cols-2">
        {PERKS.map((p) => (
          <motion.li key={p.title} variants={item} className="rounded-lg bg-surface-2 p-6">
            <p className="font-display text-lg">{p.title}</p>
            <p className="mt-1 text-sm text-muted">{p.body}</p>
          </motion.li>
        ))}
      </motion.ul>

      {isNative() ? (
        // App Store / Play rules: digital purchases inside the apps must use Apple IAP / Google Play Billing
        // (coming next). No outside payment links in the apps until then.
        <p className="mt-12 rounded-lg border border-line bg-surface p-6 text-muted">
          Plus can&rsquo;t be bought in the app just yet. If you already have Plus, it works here too.
        </p>
      ) : (<>
      <h2 className="mt-12 font-display text-xl">Choose a pass</h2>
      <p className="mt-1 text-sm text-muted">One-time payment, no auto-renew. Pay with UPI, card or wallet.</p>
      {status?.provider === "mock" && (
        <p className="mt-4 rounded-md border border-dashed border-line px-4 py-2 text-sm text-muted">Test mode: payments here are fake. No money moves.</p>
      )}

      {plans === null ? (
        <div className="mt-6 grid gap-4 sm:grid-cols-3">{[0, 1, 2].map((i) => <Skeleton key={i} className="h-44" />)}</div>
      ) : (
        <ul className="mt-6 grid gap-4 sm:grid-cols-3">
          {plans.map((p) => {
            const popular = p.days === 30;
            const perWeek = Math.round((p.amountPaise / p.days) * 7);
            const saving = week && p.days > 7 ? Math.round(100 - (perWeek / week.amountPaise) * 100) : 0;
            return (
              <li key={p.id} className={`relative flex flex-col rounded-lg border p-6 ${popular ? "border-accent bg-surface shadow-card" : "border-line bg-surface"}`}>
                {popular && <span className="absolute -top-3 left-6 rounded-full bg-accent px-2 py-0.5 text-xs font-semibold text-on-accent">Most chosen</span>}
                <p className="text-sm text-muted">{p.name.replace("Plus · ", "")}</p>
                <p className="mt-2 font-display text-3xl tabular">{rupees(p.amountPaise)}</p>
                <p className="mt-1 text-xs text-muted tabular">
                  {rupees(perWeek)}/week{saving > 0 ? ` · save ${saving}%` : ""}
                </p>
                <Button className="mt-6" variant={popular ? "primary" : "secondary"} disabled={busy !== null} onClick={() => void buy(p)}>
                  {busy === p.id ? "Opening…" : status?.plus ? "Add time" : "Get Plus"}
                </Button>
              </li>
            );
          })}
        </ul>
      )}
      <p className="mt-6 text-xs text-muted">
        Payments are processed by Razorpay. We never see or store your card or UPI details.
      </p>
      </>)}

      {mock && (
        <div role="dialog" aria-modal="true" aria-labelledby="mock-title" className="fixed inset-0 z-50 grid place-items-center bg-bg/80 p-4 backdrop-blur-sm">
          <div className="w-full max-w-sm rounded-lg border border-line bg-surface p-6 shadow-card">
            <p className="text-xs font-semibold tracking-wide text-accent uppercase">Test checkout</p>
            <h2 id="mock-title" className="mt-2 font-display text-xl">{mock.planName}</h2>
            <p className="mt-1 font-display text-3xl tabular">{rupees(mock.amountPaise)}</p>
            <p className="mt-2 text-sm text-muted">Development only: no real payment happens. In production this is Razorpay&rsquo;s secure checkout (UPI, cards, wallets).</p>
            <div className="mt-6 flex gap-2">
              <Button variant="ghost" onClick={() => { setMock(null); setBusy(null); }}>Cancel</Button>
              <Button autoFocus className="flex-1" onClick={() => void mockPay(mock.orderId).then(done).catch((e: ApiError) => setMessage(e.message))}>
                Pay {rupees(mock.amountPaise)} (test)
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
