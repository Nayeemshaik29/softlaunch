// Labels for the Desire ids (user-service model/Desire.java). Suggestive, never explicit (store rules).
export const DESIRES: { id: string; label: string }[] = [
  { id: "TAKING_IT_SLOW", label: "Taking it slow" },
  { id: "INSTANT_CHEMISTRY", label: "Instant chemistry" },
  { id: "ROMANCE", label: "Romance" },
  { id: "CUDDLING", label: "Cuddling" },
  { id: "DEEP_TALKS", label: "Deep talks" },
  { id: "LATE_NIGHT_DRIVES", label: "Late-night drives" },
  { id: "SPONTANEOUS_PLANS", label: "Spontaneous plans" },
  { id: "WEEKEND_GETAWAYS", label: "Weekend getaways" },
  { id: "PLAYFUL_TEASING", label: "Playful teasing" },
  { id: "DRESSING_UP", label: "Dressing up" },
  { id: "SLOW_DANCING", label: "Slow dancing" },
  { id: "MASSAGES", label: "Massages" },
  { id: "TRYING_NEW_THINGS", label: "Trying new things" },
  { id: "TAKING_THE_LEAD", label: "Taking the lead" },
  { id: "BEING_LED", label: "Being led" },
  { id: "BEING_SPOILED", label: "Being spoiled" },
];

export const desireLabel = (id: string) => DESIRES.find((d) => d.id === id)?.label ?? id;

export type Level = "none" | "into" | "openTo" | "notInto";
export const LIMIT = { into: 3, openTo: 3 } as const;

/** Tap cycles: none → into → open to → not into → none (full lists are skipped). */
export function nextLevel(current: Level, counts: { into: number; openTo: number }): Level {
  const order: Level[] = ["none", "into", "openTo", "notInto"];
  for (let i = 1; i <= order.length; i++) {
    const cand = order[(order.indexOf(current) + i) % order.length];
    if (cand === "into" && counts.into >= LIMIT.into) continue;
    if (cand === "openTo" && counts.openTo >= LIMIT.openTo) continue;
    return cand;
  }
  return "none";
}
