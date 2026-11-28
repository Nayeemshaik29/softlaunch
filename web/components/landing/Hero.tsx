"use client";

import { motion, useReducedMotion } from "framer-motion";
import { useRouter } from "next/navigation";
import { useCallback, useState } from "react";
import { ButtonLink } from "@/components/ui/Button";
import { Note, Star } from "@/components/ui/Doodles";
import { GoogleButton } from "@/components/ui/GoogleButton";
import { Torn } from "@/components/ui/Torn";
import { ApiError } from "@/lib/api";
import { googleSignIn, setPendingGoogle } from "@/lib/google";
import { spring } from "@/lib/motion";

/** Design v2 hero: the "Afterglow" sunburst, huge condensed caps in Jet Black, sticker CTA. Pure CSS: no 3D, no images. */
export function Hero() {
  const reduce = useReducedMotion();
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);

  const onGoogle = useCallback(async (idToken: string) => {
    setError(null);
    try {
      const r = await googleSignIn(idToken);
      if (r.kind === "in") return router.push("/swipe");
      setPendingGoogle({ idToken: r.idToken, email: r.email, suggestedName: r.suggestedName });
      router.push("/signup");
    } catch (e) {
      setError((e as ApiError).message);
    }
  }, [router]);

  const rise = (delay: number) =>
    reduce
      ? { initial: { opacity: 0 }, animate: { opacity: 1 }, transition: { duration: 0.2, delay } }
      : { initial: { opacity: 0, y: 16 }, animate: { opacity: 1, y: 0 }, transition: { ...spring.soft, delay } };

  return (
    <section className="relative isolate overflow-hidden bg-burst pb-24 pt-28 sm:pb-32 sm:pt-36" aria-labelledby="hero-title">
      <div className="relative mx-auto w-full max-w-[1200px] px-4 sm:px-6">
        <motion.h1
          initial={reduce ? false : { y: 16, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={spring.soft}
          id="hero-title"
          className="max-w-[1000px] font-display text-[clamp(4rem,11vw,10.5rem)] leading-[0.86] text-on-accent"
        >
          Dating that doesn&rsquo;t waste your evening.
        </motion.h1>
        <motion.p {...rise(0.08)} className="mt-6 max-w-xl text-lg font-semibold text-on-accent">
          Fewer, better matches near you. Verified people. Chats that fade after a day.
        </motion.p>
        <motion.div {...rise(0.16)} className="mt-10 flex flex-wrap items-center gap-4">
          <ButtonLink href="/signup" size="lg" className="!border-on-accent !bg-on-accent !text-text">
            Make your profile
          </ButtonLink>
          <div className="w-[280px]"><GoogleButton onCredential={(t) => void onGoogle(t)} onError={setError} /></div>
        </motion.div>
        {error && <p role="alert" className="mt-4 inline-block rounded-md bg-bg px-3 py-2 text-sm text-danger">{error}</p>}

        <div aria-hidden className="pointer-events-none absolute right-6 top-0 hidden rotate-12 md:block"><Star size={84} className="stroke-on-accent" /></div>
        <div aria-hidden className="pointer-events-none absolute -bottom-12 right-8 hidden -rotate-6 md:block">
          <Note className="text-4xl !text-on-accent">fewer swipes,<br />better dates</Note>
        </div>
      </div>
      <Torn className="fill-accent" height={18} />
    </section>
  );
}

/** Amber strip under the hero. Decorative repetition is hidden from screen readers; the list is read once. */
export function Marquee() {
  const items = ["24-hour chats", "Verified people", "Private Mode", "Veiled photos", "No endless swiping", "Made in India"];
  const row = items.map((i) => (
    <span key={i} className="inline-flex items-center gap-6 pr-6">{i}<span aria-hidden>✦</span></span>
  ));
  return (
    <div className="overflow-hidden whitespace-nowrap bg-accent py-3 font-display text-2xl text-on-accent">
      <p className="sr-only">{items.join(", ")}</p>
      <div aria-hidden className="inline-block pl-6">{row}{row}</div>
    </div>
  );
}
