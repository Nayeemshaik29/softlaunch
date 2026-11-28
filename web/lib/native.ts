"use client";

import { useEffect, useRef } from "react";

import { Capacitor } from "@capacitor/core";

/**
 * Bridge to the iOS / Android apps (Capacitor). Everything here is a no-op on the website.
 * Plugins are imported lazily, so the web bundle doesn't carry them.
 */
export const isNative = () => typeof window !== "undefined" && Capacitor.isNativePlatform();
export const platform = () => (typeof window === "undefined" ? "web" : Capacitor.getPlatform()); // "ios" | "android" | "web"

/* ---------------------------------------------------------------- refresh token (Keychain / Keystore) */

const REFRESH_KEY = "sl_refresh";

/** The apps can't use the web's HttpOnly cookie (different site), so the refresh token lives in encrypted storage. */
export const refreshStore = {
  async get(): Promise<string | null> {
    if (!isNative()) return null;
    const { SecureStorage } = await import("@aparajita/capacitor-secure-storage");
    return SecureStorage.getItem(REFRESH_KEY);
  },
  async set(token: string) {
    if (!isNative()) return;
    const { SecureStorage } = await import("@aparajita/capacitor-secure-storage");
    await SecureStorage.setItem(REFRESH_KEY, token);
  },
  async remove() {
    if (!isNative()) return;
    const { SecureStorage } = await import("@aparajita/capacitor-secure-storage");
    await SecureStorage.removeItem(REFRESH_KEY);
  },
};

/** Headers that ask the server for the refresh token in the body (it only agrees for the apps' own origins). */
export function nativeAuthHeaders(token?: string | null): Record<string, string> {
  if (!isNative()) return {};
  return { "X-Refresh-Transport": "body", ...(token ? { "X-Refresh-Token": token } : {}) };
}

/* ---------------------------------------------------------------- feel */

export async function haptic(kind: "tap" | "swipe" | "match") {
  if (!isNative()) return;
  try {
    const { Haptics, ImpactStyle, NotificationType } = await import("@capacitor/haptics");
    if (kind === "match") await Haptics.notification({ type: NotificationType.Success });
    else await Haptics.impact({ style: kind === "swipe" ? ImpactStyle.Medium : ImpactStyle.Light });
  } catch {
    /* no haptics on this device */
  }
}

/* ---------------------------------------------------------------- privacy */

/**
 * Screens with private content (chats, veiled photos). Android: screenshots and screen recording are BLOCKED
 * (FLAG_SECURE). iOS can't block screenshots; the plugin hides the content in the app switcher and recordings,
 * and chat-service still warns the other person when a capture is detected.
 */
export async function protectScreen(on: boolean) {
  if (!isNative()) return;
  try {
    const { PrivacyScreen } = await import("@capacitor-community/privacy-screen");
    await (on ? PrivacyScreen.enable() : PrivacyScreen.disable());
  } catch {
    /* plugin missing: nothing to do */
  }
}

/** Discreet app icon: a plain, neutral "SL" mark, so the home screen doesn't shout "dating app". Never imitates another app. */
export const DISCREET_ICON = "discreet";
// iPhone only for now: Apple's alternate-icon API is well defined. Android's version disables the main launcher
// entry, which needs testing on a real device first (a mistake there removes the app icon).
const discreetSupported = () => isNative() && platform() === "ios";

export async function setDiscreetIcon(on: boolean) {
  if (!discreetSupported()) return false;
  const { AppIcon } = await import("@capacitor-community/app-icon");
  if (!(await AppIcon.isSupported()).value) return false;
  if (on) await AppIcon.change({ name: DISCREET_ICON, suppressNotification: true, disable: [] });
  else await AppIcon.reset({ suppressNotification: true, disable: [DISCREET_ICON] });
  return true;
}
/** null = this platform doesn't offer it (the Settings switch is hidden). */
export async function discreetIconOn(): Promise<boolean | null> {
  if (!discreetSupported()) return null;
  const { AppIcon } = await import("@capacitor-community/app-icon");
  return (await AppIcon.getName()).value === DISCREET_ICON;
}

/* ---------------------------------------------------------------- app shell */

/** Android back button: the newest open overlay (e.g. the match moment) closes first. */
const backHandlers: (() => void)[] = [];

/** Android Back closes this sheet/dialog (instead of leaving the screen or the app) while it's open. */
export function useBackToClose(onClose: () => void) {
  const ref = useRef(onClose);
  useEffect(() => { ref.current = onClose; });
  useEffect(() => onBackButton(() => ref.current()), []);
}
export function onBackButton(handler: () => void) {
  backHandlers.push(handler);
  return () => {
    const i = backHandlers.lastIndexOf(handler);
    if (i >= 0) backHandlers.splice(i, 1);
  };
}

/** Called once in the app layout: status bar colours, Android back button, hide the splash screen. */
export async function setupNative() {
  const onBack = () => {
    const top = backHandlers[backHandlers.length - 1];
    if (!top) return false;
    top();
    return true;
  };
  if (!isNative()) return () => {};
  const [{ StatusBar, Style }, { App }, { SplashScreen }] = await Promise.all([
    import("@capacitor/status-bar"),
    import("@capacitor/app"),
    import("@capacitor/splash-screen"),
  ]);
  await StatusBar.setStyle({ style: Style.Dark }).catch(() => {});
  if (platform() === "android") {
    await StatusBar.setBackgroundColor({ color: getComputedStyle(document.documentElement).getPropertyValue("--bg").trim() || "#0b0b0c" }).catch(() => {});
  }
  // Android back: close what's open (e.g. the match moment), else go back, else leave the app in the background
  const handle = await App.addListener("backButton", ({ canGoBack }) => {
    if (onBack()) return;
    if (canGoBack) window.history.back();
    else void App.minimizeApp();
  });
  await SplashScreen.hide({ fadeOutDuration: 250 }).catch(() => {});
  return () => void handle.remove();
}

/**
 * Store rules: real app builds can't sell digital items through Razorpay (Apple IAP / Google Play Billing come next).
 * Test builds made with NEXT_PUBLIC_DEV_BUY=1 (never shipped) show the normal checkout so payments can be tried.
 */
export const buyingBlockedInApp = () => isNative() && process.env.NEXT_PUBLIC_DEV_BUY !== "1";
