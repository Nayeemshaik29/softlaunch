"use client";

import { useSyncExternalStore } from "react";

// The access token lives in sessionStorage: survives a reload, gone when the tab closes.
// Tokens last 15 minutes (user-service); there is no refresh token yet, so expiry means "log in again".
const KEY = "softlaunch.session";

export interface Session {
  token: string;
  userId: string;
  expiresAt: number; // ms
}

type Listener = () => void;
const listeners = new Set<Listener>();
let cached: Session | null | undefined;

function read(): Session | null {
  if (cached !== undefined) return cached;
  try {
    const raw = sessionStorage.getItem(KEY);
    cached = raw ? (JSON.parse(raw) as Session) : null;
  } catch {
    cached = null;
  }
  if (cached && cached.expiresAt <= Date.now()) cached = null;
  return cached;
}

function emit() {
  listeners.forEach((l) => l());
}

/** Reads the JWT's subject + expiry (not verified here: the gateway verifies every request). */
export function startSession(token: string): Session {
  const payload = JSON.parse(atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
  const session = { token, userId: String(payload.sub), expiresAt: Number(payload.exp) * 1000 };
  cached = session;
  try {
    sessionStorage.setItem(KEY, JSON.stringify(session));
  } catch {
    /* private mode: memory only */
  }
  emit();
  return session;
}

export function endSession() {
  cached = null;
  try {
    sessionStorage.removeItem(KEY);
  } catch {
    /* ignore */
  }
  emit();
}

export function getSession(): Session | null {
  return typeof window === "undefined" ? null : read();
}

function subscribe(l: Listener) {
  listeners.add(l);
  return () => listeners.delete(l);
}

/** undefined = not known yet (server render / first paint), null = logged out */
export function useSession(): Session | null | undefined {
  return useSyncExternalStore(subscribe, () => read(), () => undefined);
}
