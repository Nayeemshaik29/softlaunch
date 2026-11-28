"use client";

import { useCallback, useEffect, useState } from "react";
import { api, ApiError } from "./api";
import type { HeartItem, Wallet } from "./types";

/** The ❤ wallet. Prices come from the server (the app can't change them). */
export function useWallet() {
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const refresh = useCallback(() => api<Wallet>("/api/users/me/hearts").then((w) => { setWallet(w); return w; }), []);
  useEffect(() => {
    let stop = false;
    api<Wallet>("/api/users/me/hearts").then((w) => !stop && setWallet(w)).catch(() => {});
    return () => { stop = true; };
  }, []);
  return { wallet, refresh, setWallet };
}

export interface Spent { balance: number; item: HeartItem; free: boolean; until: string | null }

/** Throws ApiError; status 402 = not enough hearts (open the store). */
export const spendHearts = (item: HeartItem, ref?: string) =>
  api<Spent>("/api/users/me/hearts/spend", { method: "POST", body: { item, ref } });

export const needsHearts = (e: unknown) => e instanceof ApiError && e.status === 402;
