import Link from "next/link";

/** Wordmark: Display face, a small two-dot mark (two people, close but not touching). */
export function Logo({ href = "/" }: { href?: string }) {
  return (
    <Link href={href} className="inline-flex min-h-12 items-center gap-2 rounded-sm font-display text-xl text-text" aria-label="SoftLaunch home">
      <svg aria-hidden width="24" height="16" viewBox="0 0 24 16">
        <defs>
          <linearGradient id="lg" x1="0" x2="1">
            <stop offset="0" stopColor="#ff8a6b" />
            <stop offset="1" stopColor="#b79bff" />
          </linearGradient>
        </defs>
        <circle cx="7" cy="8" r="6" fill="url(#lg)" />
        <circle cx="17" cy="8" r="6" fill="none" stroke="url(#lg)" strokeWidth="2" />
      </svg>
      SoftLaunch
    </Link>
  );
}
