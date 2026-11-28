"use client";

import { motion, useReducedMotion } from "framer-motion";
import { useEffect, useRef, useState } from "react";
import { DesireIcon } from "@/components/desires/DesireIcon";
import { pretty } from "@/components/onboarding/CardPreview";
import { Crown } from "@/components/hearts/Heart";
import { api } from "@/lib/api";
import { desireInfo } from "@/lib/desires";
import { spring } from "@/lib/motion";
import type { FeedCard, SwipeAction } from "@/lib/types";

/** user-service PublicProfileResponse (the fields the full profile shows). */
interface FullProfile {
  displayName: string;
  verified: boolean;
  age: number | null;
  bio: string | null;
  pronouns: string | null;
  heightCm: number | null;
  lookingFor: string[];
  interests: string[];
  hangoutPlaces: string[];
  prompts: { question: string; answer: string }[];
  drinking: string | null;
  smoking: string | null;
  workout: string | null;
  jobTitle: string | null;
  company: string | null;
  education: string | null;
  languages: string[];
  city: string | null;
  photoUrls: string[];
  veiled: boolean;
}

const INTENT: Record<string, string> = {
  LONG_TERM: "A relationship", LONG_TERM_OPEN_TO_SHORT: "Relationship, open to short", SHORT_TERM_OPEN_TO_LONG: "Short-term, open to more",
  CASUAL_DATING: "Casual dating", HOOKUP: "Hookups", FRIENDS_WITH_BENEFITS: "Friends with benefits", NEW_FRIENDS: "New friends",
};
const height = (cm: number) => { const inch = Math.round(cm / 2.54); return `${Math.floor(inch / 12)}′ ${inch % 12}″`; };

/** Tap a card → everything about them. Only desires you BOTH chose are shown (theirs stay private otherwise). */
export function ProfileSheet({ card, onClose, onAction, onNote }: {
  card: FeedCard; onClose: () => void; onAction: (a: SwipeAction) => void; onNote: () => void;
}) {
  const reduce = useReducedMotion();
  const [p, setP] = useState<FullProfile | null>(null);
  const [photo, setPhoto] = useState(0);
  const closeBtn = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    closeBtn.current?.focus();
    let stop = false;
    api<FullProfile>(`/api/users/${card.userId}/profile`).then((x) => !stop && setP(x)).catch(() => {});
    const esc = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", esc);
    return () => { stop = true; window.removeEventListener("keydown", esc); };
  }, [card.userId, onClose]);

  const photos = card.photoUrls.length ? card.photoUrls : p?.photoUrls ?? [];
  const about: [string, string | null][] = p ? [
    ["Height", p.heightCm ? height(p.heightCm) : null],
    ["Works as", [p.jobTitle, p.company].filter(Boolean).join(" at ") || null],
    ["Studied", p.education],
    ["Drinks", p.drinking && pretty(p.drinking)],
    ["Smokes", p.smoking && pretty(p.smoking)],
    ["Workout", p.workout && pretty(p.workout)],
    ["Speaks", p.languages?.length ? p.languages.join(", ") : null],
    ["Hangs out at", p.hangoutPlaces?.length ? p.hangoutPlaces.join(", ") : null],
  ] : [];
  const shared = new Set(card.sharedInterests);

  return (
    <motion.div className="fixed inset-0 z-50 bg-bg/80 backdrop-blur-sm" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}>
      <motion.div role="dialog" aria-modal="true" aria-label={`${card.displayName}'s profile`} onClick={(e) => e.stopPropagation()}
        initial={reduce ? { opacity: 0 } : { y: "100%" }} animate={{ y: 0, opacity: 1 }} exit={reduce ? { opacity: 0 } : { y: "100%" }} transition={spring.gentle}
        className="absolute inset-x-0 bottom-0 top-[max(1.5rem,env(safe-area-inset-top))] mx-auto flex max-w-[480px] flex-col overflow-hidden rounded-t-lg border-2 border-b-0 border-text bg-bg">
        <div className="flex-1 overflow-y-auto">
          {/* photos */}
          <div className="relative aspect-[4/5] w-full bg-surface-2">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            {photos[photo] && <img src={photos[photo]} alt="" className={`size-full object-cover ${card.veiled ? "scale-110 blur-xl" : ""}`} />}
            {photos.length > 1 && (
              <>
                <div className="absolute inset-x-3 top-3 flex gap-1">{photos.map((_, i) => <span key={i} className={`h-1 flex-1 rounded-full ${i === photo ? "bg-text" : "bg-text/35"}`} />)}</div>
                <button type="button" aria-label="Previous photo" className="absolute inset-y-0 left-0 w-1/3" onClick={() => setPhoto((i) => Math.max(0, i - 1))} />
                <button type="button" aria-label="Next photo" className="absolute inset-y-0 right-0 w-1/3" onClick={() => setPhoto((i) => Math.min(photos.length - 1, i + 1))} />
              </>
            )}
            {card.veiled && <p className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 rounded-full bg-bg/60 px-4 py-1.5 text-sm font-semibold">Photos unveil when you match</p>}
            {card.crownedUntil && (
              <span className="absolute left-3 top-6 inline-flex items-center gap-1.5 rounded-full border-2 border-bg bg-accent px-3 py-1 text-xs font-extrabold tracking-[0.06em] text-on-accent"><Crown size={16} fill="var(--bg)" /> KING OF THE HILL</span>
            )}
            <button ref={closeBtn} type="button" onClick={onClose} aria-label="Back to cards" className="absolute right-3 top-6 grid size-11 place-items-center rounded-full bg-bg/70 text-2xl">⌄</button>
            <div className="absolute inset-x-0 bottom-0 bg-[linear-gradient(transparent,rgb(11_11_12/0.95))] px-5 pb-4 pt-16">
              <h2 className="font-display text-6xl leading-[0.9]">{card.displayName} <span className="font-sans text-3xl font-normal normal-case">{card.age ?? ""}</span>{card.verified && <span className="ml-1 text-2xl text-accent" title="Verified">✓</span>}</h2>
              <p className="mt-1">{card.distanceKm <= 1 ? "Less than 1 km away" : `${card.distanceKm} km away`}{card.city ? ` · ${card.city}` : ""}{p?.pronouns ? ` · ${p.pronouns}` : ""}</p>
            </div>
          </div>

          <div className="flex flex-col gap-6 px-5 py-5">
            {card.bio && <p className="text-lg leading-relaxed">{card.bio}</p>}

            {card.mutualDesires.length > 0 && (
              <section>
                <h3 className="text-xs font-bold tracking-[0.14em] text-accent uppercase">You both want <span className="font-[family-name:var(--font-hand)] text-lg normal-case tracking-normal">↓ mutual</span></h3>
                <div className="mt-2 flex flex-wrap gap-2">
                  {card.mutualDesires.map((id) => {
                    const d = desireInfo(id);
                    return (
                      <span key={id} className="inline-flex items-center gap-1.5 rounded-full border-2 border-bg bg-accent py-1.5 pl-2 pr-3 text-sm font-bold text-on-accent shadow-sticker">
                        <DesireIcon id={id} size={20} className={d ? `desire-${d.anim}` : ""} />{d?.label ?? pretty(id)}
                      </span>
                    );
                  })}
                </div>
                <p className="mt-2 text-xs text-muted">Only desires you both picked are shown. Everything else stays private.</p>
              </section>
            )}

            {p && p.lookingFor.length > 0 && (
              <section>
                <h3 className="text-xs font-bold tracking-[0.14em] text-muted uppercase">Looking for</h3>
                <div className="mt-2 flex flex-wrap gap-2">{p.lookingFor.map((l) => <span key={l} className="rounded-full border-[1.5px] border-text px-3 py-1 text-sm font-semibold">{INTENT[l] ?? pretty(l)}</span>)}</div>
              </section>
            )}

            {p?.prompts.map((q) => (
              <section key={q.question} className="rounded-lg border-2 border-line bg-surface p-4">
                <h3 className="text-xs font-bold tracking-[0.14em] text-muted uppercase">{q.question}</h3>
                <p className="mt-2 font-display text-3xl leading-[0.95]">{q.answer}</p>
              </section>
            ))}

            {p && p.interests.length > 0 && (
              <section>
                <h3 className="text-xs font-bold tracking-[0.14em] text-muted uppercase">Interests</h3>
                <div className="mt-2 flex flex-wrap gap-2">
                  {p.interests.map((i) => (
                    <span key={i} className={`rounded-full border-[1.5px] px-3 py-1 text-sm font-semibold ${shared.has(i) ? "border-accent bg-accent text-on-accent" : "border-text"}`}>{pretty(i)}</span>
                  ))}
                </div>
                {shared.size > 0 && <p className="mt-2 text-xs text-muted"><b className="text-accent">Highlighted</b> ones you share</p>}
              </section>
            )}

            {about.some(([, v]) => v) && (
              <section>
                <h3 className="text-xs font-bold tracking-[0.14em] text-muted uppercase">About {card.displayName}</h3>
                <dl className="mt-1">{about.filter(([, v]) => v).map(([k, v]) => (
                  <div key={k} className="flex justify-between gap-4 border-b border-surface-2 py-2.5 text-sm"><dt className="text-muted">{k}</dt><dd className="text-right font-semibold">{v}</dd></div>
                ))}</dl>
              </section>
            )}
            {!p && <p className="text-sm text-muted">Loading more about {card.displayName}…</p>}
          </div>
        </div>

        <div className="grid grid-cols-[1fr_auto_auto_1fr] gap-2 border-t-2 border-surface-2 bg-bg px-4 pb-[max(1rem,env(safe-area-inset-bottom))] pt-3">
          <button type="button" onClick={() => onAction("PASS")} className="min-h-14 rounded-md border-2 border-text font-bold tracking-[0.08em] uppercase">Pass</button>
          <button type="button" onClick={() => onAction("SUPER_LIKE")} aria-label="Super like" className="grid min-h-14 w-14 place-items-center rounded-md border-2 border-bg bg-accent-2 text-xl text-on-accent">★</button>
          <button type="button" onClick={onNote} aria-label="Like with a note (5 Hearts)" className="grid min-h-14 w-14 place-items-center rounded-md border-2 border-accent text-xl">💌</button>
          <button type="button" onClick={() => onAction("LIKE")} className="min-h-14 rounded-md border-2 border-bg bg-accent font-bold tracking-[0.08em] text-on-accent uppercase shadow-sticker">Into it</button>
        </div>
      </motion.div>
    </motion.div>
  );
}
