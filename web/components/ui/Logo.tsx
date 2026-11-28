import Link from "next/link";
import { Peach } from "./Peach";

/**
 * Peach + "SoftLaunch" wordmark (Archivo Black, amber offset shadow).
 * variant "full" (peach with the bow) only inside the app; public pages use "clean". `onPattern`: black on the sunburst.
 */
export function Logo({ href = "/", onPattern = false, variant = "clean" }: { href?: string; onPattern?: boolean; variant?: "full" | "clean" }) {
  return (
    <Link href={href} aria-label="SoftLaunch home"
      className={`inline-flex min-h-12 items-center gap-2 self-center rounded-sm font-[family-name:var(--font-wordmark)] text-[1.4rem] leading-none tracking-[-0.02em] ${onPattern ? "text-on-accent" : "text-text [text-shadow:2px_2px_0_var(--accent)]"}`}>
      <Peach variant={variant} size={34} />
      <span>SoftLaunch</span>
    </Link>
  );
}
