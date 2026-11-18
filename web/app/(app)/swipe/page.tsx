"use client";

import { SwipeDeck } from "@/components/swipe/SwipeDeck";
import { RestState } from "@/components/swipe/RestState";
import { useEnergy } from "@/lib/energy";

export default function SwipePage() {
  const { quota } = useEnergy();
  return (
    <>
      <h1 className="sr-only">Discover people near you</h1>
      <SwipeDeck />
      {quota?.remaining === 0 && <RestState resetsAt={quota.resetsAt} />}
    </>
  );
}
