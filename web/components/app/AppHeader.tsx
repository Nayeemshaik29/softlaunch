"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Logo } from "@/components/ui/Logo";
import { logout } from "@/lib/session";
import { MoodToggle } from "@/lib/mood";
import { EnergyMeter } from "./EnergyMeter";

const NAV = [
  { href: "/swipe", label: "Discover" },
  { href: "/matches", label: "Matches" },
  { href: "/notifications", label: "Activity" },
];

export function AppHeader({ unread = 0 }: { unread?: number }) {
  const path = usePathname();
  const router = useRouter();
  return (
    <header className="sticky top-0 z-20 border-b border-line bg-bg/90 backdrop-blur">
      <div className="mx-auto flex max-w-[1120px] items-center justify-between gap-4 px-4 sm:px-6">
        <Logo href="/swipe" />
        <nav aria-label="Main" className="hidden items-center gap-1 md:flex">
          {NAV.map((n) => {
            const active = path.startsWith(n.href);
            return (
              <Link
                key={n.href}
                href={n.href}
                aria-current={active ? "page" : undefined}
                className={`relative inline-flex min-h-12 items-center rounded-md px-4 text-sm font-medium ${active ? "text-text" : "text-muted hover:text-text"}`}
              >
                {n.label}
                {n.href === "/notifications" && unread > 0 && (
                  <span className="ml-2 rounded-full bg-accent px-2 text-xs text-on-accent tabular" aria-label={`${unread} unread`}>{unread}</span>
                )}
                {active && <span aria-hidden className="absolute inset-x-4 bottom-2 h-0.5 rounded-full bg-accent" />}
              </Link>
            );
          })}
        </nav>
        <div className="flex items-center gap-1 sm:gap-2">
          <MoodToggle />
          <EnergyMeter />
          <button
            type="button"
            onClick={async () => {
              await logout(); // revokes the refresh token on the server too
              router.push("/");
            }}
            aria-label="Log out"
            className="inline-flex min-h-12 min-w-12 items-center justify-center gap-2 whitespace-nowrap rounded-md px-2 text-sm text-muted hover:text-text lg:px-4"
          >
            <svg aria-hidden width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
              <path d="M8 4H5a1 1 0 00-1 1v10a1 1 0 001 1h3M12 14l4-4-4-4M16 10H8" />
            </svg>
            <span className="hidden lg:inline">Log out</span>
          </button>
        </div>
      </div>
    </header>
  );
}

/** Phone: bottom tab bar (thumb reach). */
export function BottomNav({ unread = 0 }: { unread?: number }) {
  const path = usePathname();
  return (
    <nav aria-label="Main" className="fixed inset-x-0 bottom-0 z-20 border-t border-line bg-bg/95 backdrop-blur md:hidden">
      <ul className="grid grid-cols-3">
        {NAV.map((n) => {
          const active = path.startsWith(n.href);
          return (
            <li key={n.href}>
              <Link
                href={n.href}
                aria-current={active ? "page" : undefined}
                className={`flex min-h-14 items-center justify-center gap-2 text-sm font-medium ${active ? "text-accent" : "text-muted"}`}
              >
                {n.label}
                {n.href === "/notifications" && unread > 0 && (
                  <span className="rounded-full bg-accent px-2 text-xs text-on-accent tabular">{unread}</span>
                )}
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
