"use client";

import { motion, useReducedMotion } from "framer-motion";
import { useRouter } from "next/navigation";
import { useEffect, useId, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Skeleton } from "@/components/ui/Skeleton";
import { api } from "@/lib/api";
import { myProfile } from "@/lib/me";
import { spring } from "@/lib/motion";
import { useMood } from "@/lib/mood";
import { API_URL_PUBLIC } from "@/lib/public-env";
import { logout } from "@/lib/session";
import type { PrivacySettings } from "@/lib/types";

/** Accessible on/off switch (role="switch"), 48px tall row. */
function Switch({ label, description, checked, busy, onChange }: {
  label: string; description: string; checked: boolean; busy?: boolean; onChange: (v: boolean) => void;
}) {
  const reduce = useReducedMotion();
  const id = useId();
  return (
    <div className="flex items-start justify-between gap-6 py-4">
      <div>
        <p id={`${id}-l`} className="font-medium">{label}</p>
        <p id={`${id}-d`} className="mt-1 max-w-md text-sm text-muted">{description}</p>
      </div>
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        aria-labelledby={`${id}-l`}
        aria-describedby={`${id}-d`}
        aria-busy={busy || undefined}
        disabled={busy}
        onClick={() => onChange(!checked)}
        className={`relative mt-1 inline-flex h-8 w-14 shrink-0 items-center rounded-full p-1 transition-colors disabled:opacity-60 ${checked ? "bg-accent" : "bg-surface-2 ring-1 ring-line"}`}
      >
        <motion.span
          layout={!reduce}
          transition={spring.snappy}
          className={`h-6 w-6 rounded-full shadow ${checked ? "ml-auto bg-on-accent" : "bg-muted"}`}
        />
      </button>
    </div>
  );
}

export default function SettingsPage() {
  const router = useRouter();
  const { mood, setMood, t } = useMood();
  const [privacy, setPrivacy] = useState<PrivacySettings | null>(null);
  const [busy, setBusy] = useState<keyof PrivacySettings | null>(null);
  const [status, setStatus] = useState("");

  useEffect(() => {
    void api<PrivacySettings>("/api/users/me/privacy").then(setPrivacy).catch(() => setStatus("Couldn't load your settings."));
  }, []);

  const change = async (key: keyof PrivacySettings, value: boolean) => {
    if (!privacy) return;
    const before = privacy;
    setPrivacy({ ...privacy, [key]: value }); // optimistic
    setBusy(key);
    try {
      setPrivacy(await api<PrivacySettings>("/api/users/me/privacy", { method: "PUT", body: { [key]: value } }));
      void myProfile(true); // refresh the cached copy
      setStatus(
        key === "privateMode"
          ? value ? "Private Mode is on. Only people you like can see you." : "Private Mode is off."
          : value ? "Your photos are veiled until you match." : "Your photos are visible to everyone in Discover.",
      );
    } catch {
      setPrivacy(before);
      setStatus("That didn't save. Try again.");
    } finally {
      setBusy(null);
    }
  };

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="font-display text-2xl font-normal">Settings</h1>
      <p role="status" aria-live="polite" className="mt-2 min-h-6 text-sm text-muted">{status}</p>

      <section aria-labelledby="privacy" className="mt-4 rounded-lg border border-line bg-surface px-6">
        <h2 id="privacy" className="pt-6 text-sm font-medium tracking-wide text-accent uppercase">{t("Privacy", "Discretion")}</h2>
        {privacy === null ? (
          <div className="flex flex-col gap-4 py-6"><Skeleton className="h-14" /><Skeleton className="h-14" /></div>
        ) : (
          <div className="divide-y divide-line">
            <Switch
              label="Private Mode"
              description="Only people you've liked can see you in Discover. Your online status and last seen are hidden."
              checked={privacy.privateMode}
              busy={busy === "privateMode"}
              onChange={(v) => void change("privateMode", v)}
            />
            <Switch
              label="Veiled photos"
              description={t(
                "Others see a soft blur instead of your photos until you match. Your profile is still real and verified.",
                "Keep a little mystery: your photos stay behind a soft veil until it's mutual.",
              )}
              checked={privacy.veiledPhotos}
              busy={busy === "veiledPhotos"}
              onChange={(v) => void change("veiledPhotos", v)}
            />
          </div>
        )}
      </section>

      <section aria-labelledby="look" className="mt-6 rounded-lg border border-line bg-surface px-6">
        <h2 id="look" className="pt-6 text-sm font-medium tracking-wide text-accent uppercase">Look</h2>
        <Switch
          label="After dark"
          description="A moodier night theme: deep wine colours, candle light and flirtier words. Only you see it."
          checked={mood === "after-dark"}
          onChange={(v) => setMood(v ? "after-dark" : "calm")}
        />
      </section>

      <section aria-labelledby="discover" className="mt-6 rounded-lg border border-line bg-surface p-6">
        <h2 id="discover" className="text-sm font-medium tracking-wide text-accent uppercase">Discover</h2>
        <ul className="mt-2 divide-y divide-line">
          {[
            { href: "/filters", title: "Filters", body: "Age, distance and who you'd like to see" },
            { href: "/desires", title: "Desires", body: "What you're into. Only shared ones are ever shown" },
            { href: "/plus", title: "SoftLaunch Plus", body: "From ₹49 a week: see who liked you, more energy, rewind" },
          ].map((l) => (
            <li key={l.href}>
              <a href={l.href} className="flex min-h-14 items-center justify-between gap-4 py-2 hover:text-accent">
                <span><span className="block font-medium">{l.title}</span><span className="block text-sm text-muted">{l.body}</span></span>
                <svg aria-hidden width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"><path d="M6 3l5 5-5 5" /></svg>
              </a>
            </li>
          ))}
        </ul>
      </section>

      <section aria-labelledby="account" className="mt-6 rounded-lg border border-line bg-surface p-6">
        <h2 id="account" className="text-sm font-medium tracking-wide text-accent uppercase">Account</h2>
        <div className="mt-4 flex flex-wrap gap-4">
          <Button variant="secondary" onClick={async () => { await logout(); router.push("/"); }}>Log out</Button>
          <a href={`${API_URL_PUBLIC}/privacy`} className="inline-flex min-h-12 items-center rounded-md px-4 text-sm text-muted hover:text-text">Privacy Policy</a>
          <a href={`${API_URL_PUBLIC}/delete-account`} className="inline-flex min-h-12 items-center rounded-md px-4 text-sm text-danger hover:underline">Delete account</a>
        </div>
      </section>
    </div>
  );
}
