/** Hand-drawn bits (design v2). Decorative only. */
export function Star({ size = 36, className = "stroke-accent" }: { size?: number; className?: string }) {
  return (
    <svg aria-hidden width={size} height={size} viewBox="0 0 40 40" className={className}>
      <path d="M20 3 L23 16 L37 20 L23 24 L20 37 L17 24 L3 20 L17 16Z" fill="none" strokeWidth="2.5" strokeLinejoin="round" />
    </svg>
  );
}

export function Arrow({ className = "stroke-accent" }: { className?: string }) {
  return (
    <svg aria-hidden width="44" height="34" viewBox="0 0 44 34" className={`shrink-0 ${className}`}>
      <path d="M3 4 C14 26, 28 30, 38 22 M30 20 L39 22 L37 13" fill="none" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

/** Handwritten note in Caveat. */
export function Note({ children, className = "" }: { children: React.ReactNode; className?: string }) {
  return <p className={`m-0 font-[family-name:var(--font-hand)] text-2xl leading-none text-accent ${className}`}>{children}</p>;
}
