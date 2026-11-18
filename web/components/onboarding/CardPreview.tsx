"use client";

import { motion } from "framer-motion";
import { spring } from "@/lib/motion";

export const pretty = (s: string) => s.charAt(0) + s.slice(1).toLowerCase().replace(/_/g, " ");

/** How your card will look in other people's deck (same layout as the swipe card). */
export function CardPreview({ name, age, city, bio, interests, photo }: {
  name: string; age: number | null; city: string; bio: string; interests: string[]; photo: string | null;
}) {
  return (
    <motion.figure layout transition={spring.gentle} className="relative aspect-[3/4] w-full overflow-hidden rounded-lg bg-surface-2 shadow-card">
      {photo ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={photo} alt="" className="absolute inset-0 h-full w-full object-cover" />
      ) : (
        <div className="absolute inset-0 grid place-items-center text-center text-sm text-muted" aria-hidden>
          <span>Your main photo<br />goes here</span>
        </div>
      )}
      <div className="absolute inset-0 bg-scrim" aria-hidden />
      <figcaption className="absolute inset-x-0 bottom-0 p-6 text-white">
        <p className="flex items-baseline gap-2">
          <span className="font-display text-2xl font-medium">{name || "Your name"}</span>
          {age !== null && age >= 18 && <span className="text-xl text-white/90">{age}</span>}
        </p>
        <p className="mt-1 text-sm text-white/85">Nearby{city ? ` · ${city}` : ""}</p>
        {bio && <p className="mt-2 line-clamp-2 text-base text-white/90">{bio}</p>}
        {interests.length > 0 && (
          <ul className="mt-4 flex flex-wrap gap-2">
            {interests.slice(0, 4).map((i) => <li key={i} className="rounded-full border border-white/40 px-2 py-1 text-xs">{pretty(i)}</li>)}
          </ul>
        )}
      </figcaption>
      <span className="sr-only">Preview of your card</span>
    </motion.figure>
  );
}
