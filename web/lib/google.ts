"use client";

import { useSyncExternalStore } from "react";
import { api } from "./api";
import { isNative, nativeAuthHeaders } from "./native";
import { completeLogin } from "./session";

/**
 * Sign in with Google (Google Identity Services). The browser gets an ID token from Google and we send it to
 * user-service, which checks it itself (signature, audience, expiry). No Google secret ever touches this app.
 * The client ID is public. Not in the phone apps yet: Google blocks its sign-in inside app web views,
 * so the apps need a native Google plugin first.
 */
export const GOOGLE_CLIENT_ID = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID ?? "";
export const googleEnabled = () => GOOGLE_CLIENT_ID !== "" && !isNative();
const noop = () => () => {};
/** true when the Google button can show (configured, and not inside the phone apps). */
export const useGoogleEnabled = () => useSyncExternalStore(noop, googleEnabled, () => GOOGLE_CLIENT_ID !== "");

interface GisButtonOptions {
  theme: "outline" | "filled_blue" | "filled_black";
  size: "large";
  text: "continue_with" | "signin_with" | "signup_with";
  shape: "rectangular";
  logo_alignment: "left" | "center";
  width: number;
}
interface Gis {
  accounts: {
    id: {
      initialize(o: { client_id: string; callback: (r: { credential: string }) => void; ux_mode: "popup"; use_fedcm_for_button?: boolean; auto_select?: boolean }): void;
      renderButton(el: HTMLElement, o: GisButtonOptions): void;
      disableAutoSelect(): void;
    };
  };
}
declare global {
  interface Window {
    google?: Gis;
  }
}

let loading: Promise<Gis> | null = null;
export function loadGis(): Promise<Gis> {
  if (window.google?.accounts?.id) return Promise.resolve(window.google);
  loading ??= new Promise<Gis>((resolve, reject) => {
    const s = document.createElement("script");
    s.src = "https://accounts.google.com/gsi/client";
    s.async = true;
    s.onload = () => (window.google ? resolve(window.google) : reject(new Error("Google sign-in didn't load.")));
    s.onerror = () => {
      loading = null;
      reject(new Error("Google sign-in didn't load. Check your connection."));
    };
    document.head.appendChild(s);
  });
  return loading;
}

export interface PendingGoogle {
  idToken: string;
  email: string;
  suggestedName: string;
}

/**
 * A new person who still has to tell us their birthday. Kept in memory only (never storage): the ID token is a
 * credential, valid for about an hour, and survives only client-side navigation from /login to /signup.
 */
let pending: PendingGoogle | null = null;
export const setPendingGoogle = (p: PendingGoogle | null) => (pending = p);
export const peekPendingGoogle = () => pending;
export const takePendingGoogle = () => {
  const p = pending;
  pending = null;
  return p;
};

type AuthBody = { accessToken: string; refreshToken?: string };
type NeedsBirthday = { status: "NEEDS_BIRTHDAY"; email: string; suggestedName: string };

/** "in" = logged in (session started). "birthday" = new person: ask their birthday, then call again with it. */
export async function googleSignIn(
  idToken: string,
  extra?: { dateOfBirth: string; displayName: string },
): Promise<{ kind: "in" } | ({ kind: "birthday" } & PendingGoogle)> {
  const res = await api<AuthBody | NeedsBirthday>("/api/users/auth/google", {
    method: "POST",
    auth: false,
    body: { idToken, ...(extra ?? {}) },
    credentials: isNative() ? "omit" : "include",
    headers: nativeAuthHeaders(),
  });
  if ("accessToken" in res) {
    await completeLogin(res);
    return { kind: "in" };
  }
  return { kind: "birthday", idToken, email: res.email, suggestedName: res.suggestedName };
}
