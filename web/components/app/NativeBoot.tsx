"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { isNative } from "@/lib/native";

/**
 * Phone apps only (no-op on the website). Runs on EVERY page, signed in or not:
 *  - hides the native splash screen once the first page is ready (it waits for us: launchAutoHide is off)
 *  - the apps never show the marketing landing page: "/" goes straight to Log in (which forwards to Discover when signed in)
 *  - the apps' local file server answers every folder URL (/plus/, /chat/…) with the home page's index.html, so a full
 *    page load anywhere but "/" shows the landing page under the wrong address: re-open the real page in-app instead
 */
export function NativeBoot() {
  const path = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (!isNative()) return;
    void import("@capacitor/splash-screen").then(({ SplashScreen }) => SplashScreen.hide({ fadeOutDuration: 250 }).catch(() => {}));
  }, []);

  useEffect(() => {
    if (!isNative()) return;
    if (path === "/") router.replace("/login");
    else if (document.querySelector("[data-landing]")) router.replace(location.pathname + location.search);
  }, [path, router]);

  return null;
}
