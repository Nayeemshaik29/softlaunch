// Desires (ids = user-service model/Desire.java). Flirty, suggestive, NEVER explicit: no body parts, no sex acts.
// That keeps the app inside App Store 1.1.4 and Google Play's sexual-content policy.
// anim = the little animation its symbol plays when you open it (see globals.css, "desire-*").

export type DesireAnim = "flicker" | "pulse" | "sway" | "float" | "spin" | "spark" | "bounce";

export interface DesireInfo {
  id: string;
  label: string;
  line: string; // one sentence: what it means
  anim: DesireAnim;
}

export const DESIRES: DesireInfo[] = [
  { id: "TAKING_IT_SLOW", label: "Slow Burn", line: "Long looks, longer build-up. The wait is half the fun.", anim: "flicker" },
  { id: "INSTANT_CHEMISTRY", label: "Sparks on Sight", line: "That pull you feel from across the room.", anim: "spark" },
  { id: "MAKING_OUT", label: "Make-Out Sessions", line: "Kissing like it's the only plan for the night.", anim: "pulse" },
  { id: "PLAYFUL_TEASING", label: "Tease Me", line: "A little push, a little pull, and a lot of grinning.", anim: "sway" },
  { id: "FLIRTY_TEXTS", label: "Late-Night Texts", line: "Messages you'd never read out loud.", anim: "bounce" },
  { id: "DEEP_TALKS", label: "Pillow Talk", line: "3 a.m. confessions, whispered in the dark.", anim: "float" },
  { id: "CUDDLING", label: "Tangled Up", line: "Lazy mornings, nowhere to be, no space between.", anim: "pulse" },
  { id: "SLOW_DANCING", label: "Close Dancing", line: "Low lights, one slow song, hands on hips.", anim: "sway" },
  { id: "MASSAGES", label: "Warm Hands", line: "Candles, oil, and taking turns.", anim: "float" },
  { id: "ROMANCE", label: "Candlelight", line: "Flowers, fairy lights, being swept off your feet.", anim: "flicker" },
  { id: "DRESSING_UP", label: "Dressed to Kill", line: "Getting ready for each other. Outfits that start conversations.", anim: "spark" },
  { id: "ROLE_PLAY", label: "Role Play", line: "New names, new stories, same chemistry.", anim: "spin" },
  { id: "TAKING_THE_LEAD", label: "In Charge", line: "You set the pace. They follow.", anim: "bounce" },
  { id: "BEING_LED", label: "Your Rules", line: "Hand over the reins and enjoy the ride.", anim: "sway" },
  { id: "BEING_SPOILED", label: "Spoil Me", line: "Breakfast in bed, surprises, all the attention.", anim: "bounce" },
  { id: "TRYING_NEW_THINGS", label: "Curious", line: "Open mind, new ideas, always asking first.", anim: "spin" },
  { id: "WEEKEND_GETAWAYS", label: "Do Not Disturb", line: "A weekend away, phones off, door locked.", anim: "float" },
  { id: "LATE_NIGHT_DRIVES", label: "Midnight Drive", line: "City lights, windows down, no destination.", anim: "float" },
  { id: "SPONTANEOUS_PLANS", label: "No Plans", line: "Say yes first, figure it out later.", anim: "spin" },
];

export const desireInfo = (id: string) => DESIRES.find((d) => d.id === id);
export const desireLabel = (id: string) => desireInfo(id)?.label ?? id;

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
