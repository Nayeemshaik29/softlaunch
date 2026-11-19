"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Choice } from "@/components/onboarding/parts";
import { Button } from "@/components/ui/Button";
import { Skeleton } from "@/components/ui/Skeleton";
import { api, ApiError } from "@/lib/api";
import { myProfile } from "@/lib/me";
import type { Gender, MyProfile } from "@/lib/types";

const SHOW: { v: Gender; label: string }[] = [
  { v: "WOMAN", label: "Women" },
  { v: "MAN", label: "Men" },
  { v: "NON_BINARY", label: "Non-binary people" },
];

/** Range input styled with tokens; 48px tall hit area. */
function Range(props: { label: string; value: number; min: number; max: number; onChange: (v: number) => void; suffix?: string }) {
  return (
    <label className="block">
      <span className="flex justify-between text-sm">
        <span className="font-medium">{props.label}</span>
        <span className="tabular text-muted">{props.value}{props.suffix ?? ""}</span>
      </span>
      <input type="range" min={props.min} max={props.max} value={props.value}
        onChange={(e) => props.onChange(Number(e.target.value))}
        className="mt-2 h-12 w-full cursor-pointer accent-[var(--accent)]" />
    </label>
  );
}

export default function FiltersPage() {
  const router = useRouter();
  const [loaded, setLoaded] = useState(false);
  const [ageMin, setAgeMin] = useState(21);
  const [ageMax, setAgeMax] = useState(40);
  const [distance, setDistance] = useState(25);
  const [show, setShow] = useState<Gender[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void myProfile(true).then((p: MyProfile | null) => {
      if (p) {
        setAgeMin(p.ageMin ?? 18);
        setAgeMax(p.ageMax ?? 60);
        setDistance(Math.min(100, p.maxDistanceKm ?? 25));
        setShow(p.interestedIn ?? []);
      }
      setLoaded(true);
    });
  }, []);

  async function save() {
    if (!show.length) return setError("Choose who you'd like to see.");
    setBusy(true);
    setError(null);
    try {
      await api("/api/users/me/profile/preferences", {
        method: "PUT",
        body: { ageMin: Math.min(ageMin, ageMax), ageMax: Math.max(ageMin, ageMax), maxDistanceKm: distance, interestedIn: show },
      });
      await myProfile(true);
      router.push("/swipe?refresh=1");
    } catch (e) {
      setError((e as ApiError).message);
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto max-w-xl">
      <h1 className="font-display text-2xl font-normal">Filters</h1>
      <p className="mt-2 text-muted">Who shows up in your deck. They only see you if you also fit what they&rsquo;re looking for.</p>
      {!loaded ? (
        <div className="mt-8 flex flex-col gap-4"><Skeleton className="h-20" /><Skeleton className="h-20" /><Skeleton className="h-28" /></div>
      ) : (
        <div className="mt-8 flex flex-col gap-8 rounded-lg border border-line bg-surface p-6">
          <fieldset>
            <legend className="mb-2 text-sm font-medium">Show me</legend>
            <div className="grid gap-2 sm:grid-cols-3">
              {SHOW.map((g) => (
                <Choice key={g.v} role="checkbox" selected={show.includes(g.v)}
                  onClick={() => setShow((s) => (s.includes(g.v) ? s.filter((x) => x !== g.v) : [...s, g.v]))}>
                  {g.label}
                </Choice>
              ))}
            </div>
          </fieldset>
          <div>
            <p className="mb-2 text-sm font-medium">Age <span className="tabular text-muted">{Math.min(ageMin, ageMax)}–{Math.max(ageMin, ageMax)}</span></p>
            <div className="grid gap-4 sm:grid-cols-2">
              <Range label="From" value={ageMin} min={18} max={100} onChange={setAgeMin} />
              <Range label="To" value={ageMax} min={18} max={100} onChange={setAgeMax} />
            </div>
          </div>
          <Range label="Distance" value={distance} min={1} max={100} onChange={setDistance} suffix=" km" />
          {error && <p role="alert" className="text-sm text-danger">{error}</p>}
          <div className="flex justify-end gap-4">
            <Button variant="ghost" onClick={() => router.back()}>Cancel</Button>
            <Button onClick={() => void save()} disabled={busy}>{busy ? "Saving…" : "Show people"}</Button>
          </div>
        </div>
      )}
    </div>
  );
}
