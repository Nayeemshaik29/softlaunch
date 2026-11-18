"use client";

import { useSyncExternalStore } from "react";
import { API_URL_PUBLIC as API_URL } from "./public-env";

// Two tokens:
//  - access token (JWT, 15 min): in sessionStorage + memory, sent as "Authorization: Bearer".
//  - refresh token (30 days): an HttpOnly cookie set by user-service. This script can't read it (XSS-safe);
//    the browser sends it only to /api/users/auth/*. We use it to get new access tokens.
const KEY = "softlaunch.session";
const REFRESH_EARLY_MS = 60_000; // renew 1 minute before the access token expires

export interface Session {
  token: string;
  userId: string;
  expiresAt: number; // ms
}

type State = Session | null | undefined; // undefined = still finding out (restoring from the cookie)
type Listener = () => void;
const listeners = new Set<Listener>();
let state: State = undefined;
let booted = false;
let timer: number | undefined;
let inFlight: Promise<Session | null> | null = null;

function emit() {
  listeners.forEach((l) => l());
}

function set(next: Session | null) {
  state = next;
  try {
    if (next) sessionStorage.setItem(KEY, JSON.stringify(next));
    else sessionStorage.removeItem(KEY);
  } catch {
    /* private mode: memory only */
  }
  window.clearTimeout(timer);
  if (next) {
    // keep the session alive while the app is open
    timer = window.setTimeout(() => void refreshSession(), Math.max(5_000, next.expiresAt - Date.now() - REFRESH_EARLY_MS));
  }
  emit();
}

/** Reads the JWT's subject + expiry (not verified here: the gateway verifies every request). */
export function startSession(token: string): Session {
  const payload = JSON.parse(atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
  const session = { token, userId: String(payload.sub), expiresAt: Number(payload.exp) * 1000 };
  booted = true;
  set(session);
  return session;
}

/**
 * Trade the refresh cookie for a new access token. One at a time: within this tab (shared promise) and across
 * tabs (Web Locks). That matters because every refresh ROTATES the cookie, and the server treats a second use
 * of an old token as theft and ends the session.
 */
export function refreshSession(): Promise<Session | null> {
  if (inFlight) return inFlight;
  const run = async (): Promise<Session | null> => {
    try {
      const res = await fetch(`${API_URL}/api/users/auth/refresh`, { method: "POST", credentials: "include", signal: AbortSignal.timeout(12_000) });
      if (res.status === 401) {
        set(null); // session over: log in again
        return null;
      }
      if (!res.ok) return state ?? null; // server hiccup (429/5xx): keep what we have, try again later
      const body = (await res.json()) as { accessToken: string };
      return startSession(body.accessToken);
    } catch {
      return state ?? null; // offline: keep the current token until it really fails
    }
  };
  const locks = typeof navigator !== "undefined" ? (navigator as Navigator & { locks?: LockManager }).locks : undefined;
  // (DOM types say Promise<Promise<…>>; at runtime the lock's promise resolves to run()'s value)
  const locked = locks ? (locks.request("softlaunch-refresh", run) as unknown as Promise<Session | null>) : run();
  const p: Promise<Session | null> = locked.finally(() => {
    inFlight = null;
  });
  inFlight = p;
  return p;
}

/** Ends the session on the server (revokes the refresh token) and here. */
export async function logout() {
  try {
    await fetch(`${API_URL}/api/users/auth/logout`, { method: "POST", credentials: "include", signal: AbortSignal.timeout(8_000) });
  } catch {
    /* still log out locally */
  }
  booted = true;
  set(null);
}

/** Local-only end (e.g. a 401 that refreshing couldn't fix). */
export function endSession() {
  booted = true;
  set(null);
}

/** First look in this tab: a still-valid access token, or else try the refresh cookie (stay logged in for 30 days). */
function boot() {
  if (booted || typeof window === "undefined") return;
  booted = true;
  let saved: Session | null = null;
  try {
    const raw = sessionStorage.getItem(KEY);
    saved = raw ? (JSON.parse(raw) as Session) : null;
  } catch {
    saved = null;
  }
  if (saved && saved.expiresAt - Date.now() > REFRESH_EARLY_MS) {
    set(saved);
  } else {
    void refreshSession().then((s) => {
      if (state === undefined) set(s);
    });
  }
}

export function getSession(): Session | null {
  if (typeof window === "undefined") return null;
  boot();
  return state ?? null;
}

function subscribe(l: Listener) {
  listeners.add(l);
  boot();
  return () => listeners.delete(l);
}

/** undefined = not known yet (server render, or restoring from the cookie), null = logged out */
export function useSession(): Session | null | undefined {
  return useSyncExternalStore(subscribe, () => state, () => undefined);
}
