"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { isNative } from "@/lib/native";

/**
 * Phone apps only (no-op on the website). Runs on EVERY page, signed in or not:
 *  - hides the native splash screen once the first page is ready (it waits for us: launchAutoHide is off)
 *  - the apps never show the marketing landing page: "/" goes straight to Log in (which forwards to Discover when signed in)
 */
export function NativeBoot() {
  const path = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (!isNative()) return;
    void import("@capacitor/splash-screen").then(({ SplashScreen }) => SplashScreen.hide({ fadeOutDuration: 250 }).catch(() => {}));
  }, []);

  useEffect(() => {
    if (isNative() && path === "/") router.replace("/login");
  }, [path, router]);

  return null;
}
