"use client";

import dynamic from "next/dynamic";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Skeleton } from "@/components/ui/Skeleton";
import { api, ApiError } from "@/lib/api";
import { DESIRES, LIMIT, nextLevel, type Level } from "@/lib/desires";
import { canRun3D } from "@/lib/device";
import { useMood } from "@/lib/mood";
import type { Desires } from "@/lib/types";

const DesireScene = dynamic(() => import("@/components/desires/DesireScene"), { ssr: false });

const LEVEL_TEXT: Record<Level, string> = { none: "Not set", into: "Into", openTo: "Open to", notInto: "Not into" };

export default function DesiresPage() {
  const router = useRouter();
  const { t, palette } = useMood();
  const [levels, setLevels] = useState<Record<string, Level> | null>(null);
  const [three, setThree] = useState(false);
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState("");

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- device check needs the browser
    setThree(canRun3D());
    void api<Desires>("/api/users/me/profile/desires").then((d) => {
      const l: Record<string, Level> = {};
      d.into.forEach((x) => (l[x] = "into"));
      d.openTo.forEach((x) => (l[x] = "openTo"));
      d.notInto.forEach((x) => (l[x] = "notInto"));
      setLevels(l);
    }).catch(() => setLevels({}));
  }, []);

  const counts = useMemo(() => {
    const v = Object.values(levels ?? {});
    return { into: v.filter((x) => x === "into").length, openTo: v.filter((x) => x === "openTo").length, notInto: v.filter((x) => x === "notInto").length };
  }, [levels]);

  const toggle = (id: string) =>
    setLevels((l) => {
      const current = l?.[id] ?? "none";
      const others = { into: counts.into - (current === "into" ? 1 : 0), openTo: counts.openTo - (current === "openTo" ? 1 : 0) };
      const next = nextLevel(current, others);
      const label = DESIRES.find((d) => d.id === id)?.label;
      setStatus(`${label}: ${LEVEL_TEXT[next]}`);
      return { ...(l ?? {}), [id]: next };
    });

  async function save() {
    if (!levels) return;
    setBusy(true);
    const pick = (lv: Level) => Object.entries(levels).filter(([, v]) => v === lv).map(([k]) => k);
    try {
      await api("/api/users/me/profile/desires", { method: "PUT", body: { into: pick("into"), openTo: pick("openTo"), notInto: pick("notInto") } });
      setStatus("Saved. People who share your desires will see them glow on your card.");
      setTimeout(() => router.push("/swipe"), 900);
    } catch (e) {
      setStatus((e as ApiError).message);
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="font-display text-2xl">{t("Desires", "What you're into")}</h1>
      <p className="mt-2 max-w-xl text-muted">
        Tap to choose: <strong className="text-accent">into</strong> (up to {LIMIT.into}), <strong className="text-accent-2">open to</strong> (up to {LIMIT.openTo}),
        or <span className="line-through">not into</span>. Others only ever see the ones you <em>both</em> chose. Nobody sees your &ldquo;not into&rdquo;.
      </p>
      <p role="status" aria-live="polite" className="mt-2 min-h-6 text-sm text-muted">{status}</p>

      {levels === null ? (
        <Skeleton className="mt-4 aspect-square w-full rounded-lg sm:aspect-[16/10]" />
      ) : (
        <>
          {three && (
            <div className="relative mt-4 aspect-square w-full overflow-hidden rounded-lg border border-line bg-surface sm:aspect-[16/10]" aria-hidden>
              <DesireScene levels={levels} palette={palette} onToggle={toggle} />
              <div className="pointer-events-none absolute bottom-4 left-4 flex gap-4 text-xs text-muted">
                <span className="tabular">Into {counts.into}/{LIMIT.into}</span>
                <span className="tabular">Open to {counts.openTo}/{LIMIT.openTo}</span>
                <span className="tabular">Not into {counts.notInto}</span>
              </div>
            </div>
          )}
          {/* the same choices as a list: keyboard + screen readers, and the whole picker when 3D is off */}
          <details className="mt-6" open={!three}>
            <summary className="min-h-12 cursor-pointer py-2 text-sm font-medium">{three ? "Choose from a list instead" : "Your desires"}</summary>
            <ul className="mt-2 grid gap-2 sm:grid-cols-2">
              {DESIRES.map((d) => {
                const lv = levels[d.id] ?? "none";
                return (
                  <li key={d.id}>
                    <button type="button" onClick={() => toggle(d.id)} aria-label={`${d.label}: ${LEVEL_TEXT[lv]}. Tap to change.`}
                      className={`flex min-h-12 w-full items-center justify-between rounded-md border px-4 text-left ${
                        lv === "into" ? "border-accent bg-surface-2" : lv === "openTo" ? "border-accent-2 bg-surface-2" : "border-line"}`}>
                      <span className={lv === "notInto" ? "text-muted line-through" : ""}>{d.label}</span>
                      <span className="text-xs text-muted">{LEVEL_TEXT[lv]}</span>
                    </button>
                  </li>
                );
              })}
            </ul>
          </details>
          <div className="mt-6 flex justify-end gap-4">
            <Button variant="ghost" onClick={() => router.back()}>Cancel</Button>
            <Button onClick={() => void save()} disabled={busy}>{busy ? "Saving…" : "Save desires"}</Button>
          </div>
        </>
      )}
    </div>
  );
}
