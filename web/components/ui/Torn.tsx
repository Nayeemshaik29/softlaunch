/** Torn-paper edge between a pattern and the page below it. Put it at the bottom of a `relative` section. */
export function Torn({ className = "fill-bg", height = 16 }: { className?: string; height?: number }) {
  return (
    <svg
      aria-hidden
      viewBox="0 0 390 16"
      preserveAspectRatio="none"
      className={`pointer-events-none absolute inset-x-0 -bottom-px block w-full -scale-y-100 ${className}`}
      style={{ height }}
    >
      <path d="M0 0H390V7L372 13 355 5 338 14 320 6 301 12 285 3 266 13 248 6 230 14 212 4 195 11 177 3 160 13 141 6 124 14 106 4 88 12 70 3 52 13 35 6 17 14 0 8Z" />
    </svg>
  );
}
