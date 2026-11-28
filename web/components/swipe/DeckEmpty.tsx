"use client";

import { Button, ButtonLink } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { useMood } from "@/lib/mood";
import { useState } from "react";
import { api } from "@/lib/api";
import { currentPosition } from "@/lib/location";
import { isNative } from "@/lib/native";

export function DeckEmpty({ error, needsLocation, onRetry }: { error: boolean; needsLocation: boolean; onRetry: () => void }) {
  const { t } = useMood();
  const [locating, setLocating] = useState(false);
  const [denied, setDenied] = useState(false);

  const share = async () => {
    setLocating(true);
    try {
      const p = await currentPosition();
      await api("/api/discovery/me/location", { method: "PUT", body: { latitude: p.coords.latitude, longitude: p.coords.longitude } });
      setDenied(false);
      onRetry();
    } catch {
      setDenied(true);
    } finally {
      setLocating(false);
    }
  };

  if (needsLocation) {
    return (
      <div className="absolute inset-0 grid place-items-center rounded-lg border border-line bg-surface">
        <EmptyState
          kind="rings"
          title="Where should we look?"
          body={denied
            ? isNative()
              ? "We couldn't get your location. Turn on Location for SoftLaunch in your phone's Settings (Approximate is enough), then try again."
              : "Location is off for this site. Turn it on in your browser settings, then try again."
            : "Share your location to see people nearby. Others only ever see a rounded distance."}
          action={<Button onClick={() => void share()} disabled={locating}>{locating ? "Finding you…" : "Use my location"}</Button>}
        />
      </div>
    );
  }
  return (
    <div className="absolute inset-0 grid place-items-center rounded-lg border border-line bg-surface">
      {error ? (
        <EmptyState kind="cards" title="We couldn't load your deck." body="Check your connection, then try again." action={<Button variant="secondary" onClick={onRetry}>Try again</Button>} />
      ) : (
        <EmptyState
          kind="cards"
          title={t("That's everyone nearby, for now.", "Quiet night nearby.")}
          body={t("New people join every day. We'll have fresh faces next time you open the app.", "More people come out later. Check back after dark.")}
          action={<ButtonLink href="/matches" variant="secondary">See your matches</ButtonLink>}
        />
      )}
    </div>
  );
}
