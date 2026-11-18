"use client";

import dynamic from "next/dynamic";
import { motion, useReducedMotion } from "framer-motion";
import { useEffect, useState } from "react";
import { ButtonLink } from "@/components/ui/Button";
import { canRun3D, whenIdle } from "@/lib/device";
import { spring } from "@/lib/motion";

// three.js (~600 KB) is never in the first load: it's fetched after the page is idle, only on capable devices.
const HeroScene = dynamic(() => import("./HeroScene"), { ssr: false });

export function Hero() {
  const reduce = useReducedMotion();
  const [load3D, setLoad3D] = useState(false);
  const [sceneReady, setSceneReady] = useState(false);

  useEffect(() => {
    if (!canRun3D()) return;
    // The scene starts on the visitor's first interaction (mouse move, touch, scroll, key): people move within
    // a second, and until then the static image looks the same. Keeps the first load light (Lighthouse 90+).
    const events = ["pointermove", "pointerdown", "touchstart", "scroll", "keydown"] as const;
    let cancelIdle = () => {};
    const go = () => {
      events.forEach((e) => window.removeEventListener(e, go));
      cancelIdle = whenIdle(() => setLoad3D(true), 400);
    };
    events.forEach((e) => window.addEventListener(e, go, { once: true, passive: true }));
    return () => {
      cancelIdle();
      events.forEach((e) => window.removeEventListener(e, go));
    };
  }, []);

  const rise = (delay: number) =>
    reduce
      ? { initial: { opacity: 0 }, animate: { opacity: 1 }, transition: { duration: 0.2, delay } }
      : { initial: { opacity: 0, y: 16 }, animate: { opacity: 1, y: 0 }, transition: { ...spring.soft, delay } };

  return (
    <section className="relative isolate flex min-h-[100svh] items-end overflow-hidden pb-16 sm:items-center sm:pb-0" aria-labelledby="hero-title">
      {/* static image: first paint, weak devices, reduced motion, no WebGL */}
      <div
        aria-hidden
        className="absolute inset-0 -z-10 bg-cover bg-center transition-opacity duration-700"
        style={{ backgroundImage: "url(/hero-fallback.svg)", opacity: sceneReady ? 0 : 1 }}
      />
      {load3D && (
        <div className="absolute inset-0 -z-10 transition-opacity duration-700" style={{ opacity: sceneReady ? 1 : 0 }}>
          <HeroScene onReady={() => setSceneReady(true)} />
        </div>
      )}
      {/* keeps the headline readable over any frame of the scene */}
      <div aria-hidden className="absolute inset-0 -z-10 bg-[linear-gradient(90deg,var(--bg)_0%,color-mix(in_srgb,var(--bg)_70%,transparent)_38%,transparent_70%)]" />

      <div className="mx-auto w-full max-w-[1120px] px-4 sm:px-6">
        <div className="max-w-2xl">
          <motion.p {...rise(0)} className="mb-4 text-sm font-medium tracking-wide text-muted uppercase">
            A calmer dating app
          </motion.p>
          <motion.h1
            initial={reduce ? false : { y: 12 }}
            animate={{ y: 0 }}
            transition={spring.soft}
            id="hero-title" className="font-display text-display font-normal text-text">
            Dating that doesn&rsquo;t waste your evening
          </motion.h1>
          <motion.p initial={reduce ? false : { y: 12 }} animate={{ y: 0 }} transition={{ ...spring.soft, delay: 0.06 }} className="mt-6 max-w-xl text-lg text-muted">
            Fewer, better matches near you. Verified profiles, chats that fade after a day, and a gentle daily limit so
            you swipe with intent, then close the app and live your life.
          </motion.p>
          <motion.div {...rise(0.18)} className="mt-8 flex flex-wrap items-center gap-4">
            <ButtonLink href="/signup" size="lg">
              Create your profile
            </ButtonLink>
            <ButtonLink href="/login" variant="ghost" size="lg">
              Log in
            </ButtonLink>
          </motion.div>
          <motion.p {...rise(0.24)} className="mt-6 text-sm text-muted">
            Like the big swipe apps, but calmer.
          </motion.p>
        </div>
      </div>
    </section>
  );
}
