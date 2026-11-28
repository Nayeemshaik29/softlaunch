"use client";

import { useBackToClose } from "@/lib/native";
import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import { useEffect, useRef, useState } from "react";
import { ApiError } from "@/lib/api";
import { mockPay, rupees, startCheckout } from "@/lib/plus";
import { spring } from "@/lib/motion";
import type { Checkout as Order, Plan } from "@/lib/types";

type Stage = "summary" | "paying" | "mock" | "success" | "error";

/** A little burst of hearts / sparks for the success screen. Decorative. */
function Burst() {
  const reduce = useReducedMotion();
  if (reduce) return null;
  return (
    <div aria-hidden className="pointer-events-none absolute inset-0 overflow-hidden">
      {Array.from({ length: 16 }, (_, i) => {
        const a = (i / 16) * Math.PI * 2;
        return (
          <motion.span key={i} className="absolute left-1/2 top-1/3 text-2xl"
            initial={{ x: 0, y: 0, opacity: 1, scale: 0.4 }}
            animate={{ x: Math.cos(a) * (120 + (i % 3) * 30), y: Math.sin(a) * (120 + (i % 3) * 30), opacity: 0, scale: 1.2 }}
            transition={{ duration: 1.1, ease: "easeOut", delay: (i % 4) * 0.04 }}>
            {i % 3 === 0 ? "✦" : "❤"}
          </motion.span>
        );
      })}
    </div>
  );
}

/**
 * SoftLaunch checkout: our summary screen → Razorpay's own payment window (UPI / cards / wallets; the card or UPI
 * details never touch our servers) → the server verifies Razorpay's signature → animated success.
 * In local/CI "mock" mode a clearly-labelled test window stands in for Razorpay.
 */
export function CheckoutSheet({ plan, perks, onClose, onPaid }: {
  plan: Plan; perks: string[]; onClose: () => void; onPaid: () => void;
}) {
  const reduce = useReducedMotion();
  const [stage, setStage] = useState<Stage>("summary");
  useBackToClose(() => { if (stage !== "paying") onClose(); });
  const [order, setOrder] = useState<Order | null>(null);
  const [error, setError] = useState("");
  const first = useRef<HTMLButtonElement>(null);
  useEffect(() => first.current?.focus(), [stage]);

  const pay = async () => {
    setStage("paying");
    try {
      const r = await startCheckout(plan.id, "#f28c28");
      if ("mock" in r) { setOrder(r.mock); setStage("mock"); return; }
      if ("cancelled" in r) { setStage("summary"); return; }
      setStage("success");
      onPaid();
    } catch (e) {
      setError((e as ApiError).message || "Payment didn't go through.");
      setStage("error");
    }
  };

  const finishMock = async () => {
    if (!order) return;
    setStage("paying");
    try {
      await mockPay(order.orderId);
      setStage("success");
      onPaid();
    } catch (e) {
      setError((e as ApiError).message);
      setStage("error");
    }
  };

  const isHearts = plan.kind === "hearts";
  return (
    <motion.div className="fixed inset-0 z-50 flex items-end justify-center bg-bg/75 backdrop-blur-sm sm:items-center"
      initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => stage !== "paying" && onClose()}>
      <motion.div role="dialog" aria-modal="true" aria-labelledby="co-title" onClick={(e) => e.stopPropagation()}
        initial={reduce ? { opacity: 0 } : { y: 80, opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={reduce ? { opacity: 0 } : { y: 80, opacity: 0 }}
        transition={spring.gentle}
        className="relative w-full max-w-md overflow-hidden rounded-t-lg border-2 border-text bg-bg pb-[max(1.25rem,env(safe-area-inset-bottom))] sm:rounded-lg">
        <div className="relative grid h-36 place-items-center bg-burst">
          <motion.span className="font-display text-6xl text-on-accent" key={stage}
            initial={reduce ? false : { scale: 0.6, rotate: -8 }} animate={{ scale: 1, rotate: 0 }} transition={spring.snappy}>
            {stage === "success" ? "Done!" : rupees(plan.amountPaise)}
          </motion.span>
          <span className="absolute bottom-3 text-xs font-bold tracking-[0.12em] text-on-accent uppercase">{plan.name}</span>
        </div>
        {stage === "success" && <Burst />}
        <div className="px-5 pt-5">
          <AnimatePresence mode="wait">
            {stage === "summary" || stage === "paying" ? (
              <motion.div key="sum" initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }}>
                <h2 id="co-title" className="font-display text-4xl">{isHearts ? `${plan.hearts} Hearts` : plan.name}</h2>
                <ul className="mt-3 grid gap-1.5 text-sm">
                  {perks.map((p) => <li key={p} className="flex gap-2"><span className="font-bold text-accent">✓</span>{p}</li>)}
                </ul>
                <div className="mt-4 flex justify-between border-t-2 border-surface-2 pt-3 text-lg font-bold"><span>Total</span><span className="tabular">{rupees(plan.amountPaise)}</span></div>
                <button ref={first} type="button" disabled={stage === "paying"} onClick={() => void pay()}
                  className="mt-4 min-h-14 w-full rounded-md border-2 border-bg bg-accent font-bold tracking-[0.08em] text-on-accent uppercase shadow-sticker disabled:opacity-60">
                  {stage === "paying" ? "Opening Razorpay…" : `Pay ${rupees(plan.amountPaise)}`}
                </button>
                <p className="mt-3 text-center text-xs text-muted">UPI, cards, net banking and wallets via Razorpay. One-time payment, no auto-renew. We never see your card or UPI details.</p>
              </motion.div>
            ) : stage === "mock" ? (
              <motion.div key="mock" initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }}>
                <p className="inline-block rounded-sm bg-accent-2 px-2 py-0.5 text-xs font-bold text-on-accent uppercase">Test mode · no real money</p>
                <h2 id="co-title" className="mt-2 font-display text-4xl">Test checkout</h2>
                <p className="mt-2 text-sm text-muted">Razorpay keys aren&rsquo;t set up yet, so this stands in for Razorpay&rsquo;s payment window.</p>
                <button ref={first} type="button" onClick={() => void finishMock()} className="mt-4 min-h-14 w-full rounded-md border-2 border-bg bg-accent font-bold tracking-[0.08em] text-on-accent uppercase shadow-sticker">Pay (test)</button>
                <button type="button" onClick={() => setStage("summary")} className="mt-2 min-h-11 w-full text-sm text-muted">Back</button>
              </motion.div>
            ) : stage === "success" ? (
              <motion.div key="ok" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}>
                <h2 id="co-title" className="font-display text-4xl">{isHearts ? `+${plan.hearts} ❤` : "You're on Plus"}</h2>
                <p className="mt-2 text-muted">{isHearts ? "Your Hearts are in your wallet. Go make someone's evening." : "Everything's unlocked. Enjoy it."}</p>
                <button ref={first} type="button" onClick={onClose} className="mt-4 min-h-14 w-full rounded-md border-2 border-bg bg-accent font-bold tracking-[0.08em] text-on-accent uppercase shadow-sticker">Nice</button>
              </motion.div>
            ) : (
              <motion.div key="err" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
                <h2 id="co-title" className="font-display text-4xl">Not charged</h2>
                <p role="alert" className="mt-2 text-danger">{error}</p>
                <button ref={first} type="button" onClick={() => setStage("summary")} className="mt-4 min-h-12 w-full rounded-md border-2 border-text font-bold uppercase">Try again</button>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </motion.div>
    </motion.div>
  );
}
