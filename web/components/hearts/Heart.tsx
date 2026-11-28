/** The ❤ currency icon. */
export function Heart({ size = 18, className = "" }: { size?: number; className?: string }) {
  return (
    <svg aria-hidden width={size} height={size} viewBox="0 0 24 24" className={className}>
      <path d="M12 21C5 16 2 12 2 8.5A4.5 4.5 0 0112 6a4.5 4.5 0 0110 2.5C22 12 19 16 12 21z" fill="var(--accent)" stroke="var(--bg)" strokeWidth="1.6" />
    </svg>
  );
}

export function Crown({ size = 26, fill = "var(--accent)" }: { size?: number; fill?: string }) {
  return (
    <svg aria-hidden width={size} height={size} viewBox="0 0 48 48">
      <path d="M8 36l-3-22 11 9 8-15 8 15 11-9-3 22z" fill={fill} stroke="var(--bg)" strokeWidth="2.5" strokeLinejoin="round" />
      <path d="M8 42h32" stroke="var(--bg)" strokeWidth="3" strokeLinecap="round" />
    </svg>
  );
}

/** "❤ 7" pill. */
export function WalletPill({ balance }: { balance: number | null }) {
  return (
    <span className="inline-flex min-h-9 items-center gap-1.5 rounded-full border-2 border-accent bg-surface px-3 font-bold tabular">
      <Heart size={16} />{balance ?? "–"}<span className="sr-only"> Hearts</span>
    </span>
  );
}
