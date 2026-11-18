const rtf = new Intl.RelativeTimeFormat("en", { numeric: "auto" });

/** "just now", "5 min ago", "yesterday" */
export function ago(iso: string, now = Date.now()) {
  const s = Math.round((Date.parse(iso) - now) / 1000);
  const a = Math.abs(s);
  if (a < 45) return "just now";
  if (a < 3600) return rtf.format(Math.round(s / 60), "minute");
  if (a < 86400) return rtf.format(Math.round(s / 3600), "hour");
  return rtf.format(Math.round(s / 86400), "day");
}

export const stripEmoji = (s: string) => s.replace(/[\p{Extended_Pictographic}️]/gu, "").trim();
