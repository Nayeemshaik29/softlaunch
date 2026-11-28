"use client";

import dynamic from "next/dynamic";
import { motion, useReducedMotion } from "framer-motion";
import { useEffect, useState } from "react";
import { ButtonLink } from "@/components/ui/Button";
import { canRun3D } from "@/lib/device";
import { untilText } from "@/lib/energy";
import { spring } from "@/lib/motion";
import { useMood } from "@/lib/mood";

const RestScene = dynamic(() => import("./RestScene"), { ssr: false });

/** Energy at zero: a calm pause until midnight, not an error. */
export function RestState({ resetsAt }: { resetsAt: string }) {
  const reduce = useReducedMotion();
  const { t, palette } = useMood();
  const [now, setNow] = useState(() => Date.now());
  const [three, setThree] = useState(false);
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- device check needs the browser
    setThree(canRun3D());
    const id = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(id);
  }, []);

  const end = new Date(resetsAt).getTime();
  const start = end - 24 * 3600_000;
  const fill = Math.min(1, Math.max(0, (now - start) / (end - start)));
  const left = end - now;
  const hh = String(Math.floor(Math.max(0, left) / 3600_000)).padStart(2, "0");
  const mm = String(Math.floor((Math.max(0, left) % 3600_000) / 60_000)).padStart(2, "0");
  const ss = String(Math.floor((Math.max(0, left) % 60_000) / 1000)).padStart(2, "0");

  return (
    <motion.section
      aria-labelledby="rest-title"
      className="fixed inset-0 z-10 flex flex-col items-center justify-center bg-bg px-6 pt-16 text-center"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.4 }}
    >
      <div className="h-56 w-56 sm:h-72 sm:w-72" aria-hidden>
        {three ? (
          <RestScene fill={fill} palette={palette} />
        ) : (
          <svg viewBox="0 0 200 200" className="h-full w-full">
            <circle cx="100" cy="100" r="80" fill="none" stroke="var(--surface-2)" strokeWidth="8" />
            <circle cx="100" cy="100" r="80" fill="none" stroke="var(--accent)" strokeWidth="8" strokeLinecap="round"
              strokeDasharray={2 * Math.PI * 80} strokeDashoffset={2 * Math.PI * 80 * (1 - fill)} transform="rotate(-90 100 100)" />
            <circle cx="100" cy="100" r="30" fill="var(--accent-2)" opacity="0.6" />
          </svg>
        )}
      </div>
      <motion.div initial={reduce ? { opacity: 0 } : { opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ ...spring.soft, delay: 0.15 }}>
        <h1 id="rest-title" className="mt-8 font-display text-3xl">{t("That\u2019s today\u2019s energy.", "That\u2019s enough for tonight.")}</h1>
        <p className="mx-auto mt-4 max-w-md text-lg text-muted">
          {t(
            "You\u2019ve seen your swipes for today. Close the app, enjoy your evening. Your energy refills at midnight.",
            "Leave a little mystery for tomorrow. Your energy refills at midnight, and so does the crowd.",
          )}
        </p>
        <p className="mt-8 font-display text-4xl tabular" aria-label={`Refills in ${untilText(left)}`}>
          {hh}:{mm}:{ss}
        </p>
        <div className="mt-8 flex flex-wrap justify-center gap-4">
          <ButtonLink href="/matches" variant="secondary">Talk to your matches</ButtonLink>
        </div>
      </motion.div>
    </motion.section>
  );
}
