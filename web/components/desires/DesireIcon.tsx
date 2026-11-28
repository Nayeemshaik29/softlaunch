/* One hand-drawn line symbol per desire (48×48, stroke = currentColor). Decorative: always aria-hidden. */

const P: Record<string, React.ReactNode> = {
  // flame
  TAKING_IT_SLOW: <path d="M24 42c-7 0-12-5-12-11 0-7 6-10 7-17 4 3 6 7 6 11 2-2 3-4 3-7 5 4 8 8 8 13 0 6-5 11-12 11zM24 42c-3 0-5-2-5-5 0-3 3-5 4-8 3 2 6 5 6 8 0 3-2 5-5 5z" />,
  // sparks
  INSTANT_CHEMISTRY: <><path d="M24 6l3 12 12 3-12 3-3 12-3-12-12-3 12-3z" /><path d="M38 6v6M35 9h6M10 36v6M7 39h6" /></>,
  // lips
  MAKING_OUT: <><path d="M6 24c5-6 9-9 13-9 2 0 4 2 5 3 1-1 3-3 5-3 4 0 8 3 13 9-5 6-11 10-18 10S11 30 6 24z" /><path d="M6 24c6 2 12 3 18 3s12-1 18-3" /></>,
  // feather
  PLAYFUL_TEASING: <><path d="M38 8C24 8 12 18 12 34l2 2c16 0 26-12 26-26z" /><path d="M10 40l20-20M18 30h8M22 24h8" /></>,
  // phone + heart
  FLIRTY_TEXTS: <><rect x="14" y="5" width="20" height="38" rx="4" /><path d="M21 38h6" /><path d="M24 29c-5-3-7-6-7-8a3.5 3.5 0 017-1 3.5 3.5 0 017 1c0 2-2 5-7 8z" /></>,
  // moon + speech
  DEEP_TALKS: <><path d="M30 6a14 14 0 1012 18A12 12 0 0130 6z" /><path d="M10 34h14v6l-5-3h-9z" /></>,
  // two intertwined curves
  CUDDLING: <><path d="M10 30c0-10 8-16 14-16s8 6 4 10-12 4-12 10 6 8 12 8 10-6 10-12" /><path d="M38 18c0-6-4-10-8-10" /></>,
  // music notes
  SLOW_DANCING: <><path d="M18 36V12l20-5v24" /><circle cx="14" cy="36" r="4" /><circle cx="34" cy="31" r="4" /><path d="M18 19l20-5" /></>,
  // open hand with warmth lines
  MASSAGES: <><path d="M16 40c-4-4-6-9-6-14v-6a2.5 2.5 0 015 0v4V10a2.5 2.5 0 015 0v12V8a2.5 2.5 0 015 0v14V11a2.5 2.5 0 015 0v17c0 6-4 12-10 12z" /><path d="M38 14c2 2 2 4 0 6M42 11c3 3 3 9 0 12" /></>,
  // candle
  ROMANCE: <><path d="M24 6c3 4 4 6 4 8a4 4 0 01-8 0c0-2 1-4 4-8z" /><rect x="18" y="20" width="12" height="20" rx="2" /><path d="M12 42h24" /></>,
  // stiletto
  DRESSING_UP: <><path d="M6 34c8 0 14-4 18-12l4-6c2 4 6 6 10 6 2 0 4 2 4 4v8H22c-4 0-8 2-10 4" /><path d="M38 38v4M6 34v6" /></>,
  // masquerade mask
  ROLE_PLAY: <><path d="M6 18c6-2 12-2 18 2 6-4 12-4 18-2 0 8-4 14-10 14-4 0-6-3-8-6-2 3-4 6-8 6-6 0-10-6-10-14z" /><circle cx="15" cy="23" r="2.5" /><circle cx="33" cy="23" r="2.5" /><path d="M24 32v10" /></>,
  // crown
  TAKING_THE_LEAD: <><path d="M8 36l-2-20 10 8 8-14 8 14 10-8-2 20z" /><path d="M8 42h32" /></>,
  // ribbon / reins
  BEING_LED: <><path d="M24 22c-6-8-14-10-16-6s6 8 16 6zM24 22c6-8 14-10 16-6s-6 8-16 6z" /><path d="M24 22l-6 20M24 22l6 20" /><circle cx="24" cy="22" r="2.5" /></>,
  // gift
  BEING_SPOILED: <><rect x="8" y="18" width="32" height="8" rx="1" /><path d="M11 26v16h26V26M24 18v24" /><path d="M24 18c-4-8-12-8-12-3 0 3 6 3 12 3zM24 18c4-8 12-8 12-3 0 3-6 3-12 3z" /></>,
  // compass
  TRYING_NEW_THINGS: <><circle cx="24" cy="24" r="17" /><path d="M30 18l-4 8-8 4 4-8z" /><path d="M24 7v3M24 38v3M7 24h3M38 24h3" /></>,
  // door hanger
  WEEKEND_GETAWAYS: <><path d="M14 6h20v36H14z" /><circle cx="24" cy="13" r="3.5" /><path d="M19 26h10M19 31h10M19 36h6" /></>,
  // crescent + road
  LATE_NIGHT_DRIVES: <><path d="M32 6a8 8 0 108 10 7 7 0 01-8-10z" /><path d="M12 42l8-18M36 42l-8-18M24 30v3M24 37v4" /></>,
  // dice
  SPONTANEOUS_PLANS: <><rect x="8" y="8" width="32" height="32" rx="6" /><circle cx="17" cy="17" r="2.2" fill="currentColor" /><circle cx="31" cy="31" r="2.2" fill="currentColor" /><circle cx="24" cy="24" r="2.2" fill="currentColor" /><circle cx="31" cy="17" r="2.2" fill="currentColor" /><circle cx="17" cy="31" r="2.2" fill="currentColor" /></>,
};

export function DesireIcon({ id, size = 40, className = "" }: { id: string; size?: number; className?: string }) {
  return (
    <svg aria-hidden width={size} height={size} viewBox="0 0 48 48" fill="none" stroke="currentColor" strokeWidth="2.5"
      strokeLinecap="round" strokeLinejoin="round" className={className}>
      {P[id] ?? <circle cx="24" cy="24" r="14" />}
    </svg>
  );
}
