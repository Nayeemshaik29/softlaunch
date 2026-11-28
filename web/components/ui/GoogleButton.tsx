"use client";

import { useEffect, useRef } from "react";
import { GOOGLE_CLIENT_ID, loadGis, useGoogleEnabled } from "@/lib/google";

/**
 * Google's own "Continue with Google" button (rendered by Google, so it always follows their branding rules).
 * Renders nothing when Google sign-in isn't configured, or inside the phone apps.
 */
export function GoogleButton({
  onCredential,
  text = "continue_with",
  onError,
}: {
  onCredential: (idToken: string) => void;
  text?: "continue_with" | "signin_with" | "signup_with";
  onError?: (message: string) => void;
}) {
  const box = useRef<HTMLDivElement>(null);
  const cb = useRef(onCredential);
  useEffect(() => {
    cb.current = onCredential;
  });
  const enabled = useGoogleEnabled();

  useEffect(() => {
    if (!enabled) return;
    let cancelled = false;
    loadGis()
      .then((g) => {
        if (cancelled || !box.current) return;
        g.accounts.id.initialize({
          client_id: GOOGLE_CLIENT_ID,
          callback: (r) => cb.current(r.credential),
          ux_mode: "popup",
          use_fedcm_for_button: true,
          auto_select: false,
        });
        const width = Math.min(400, Math.max(200, Math.round(box.current.getBoundingClientRect().width)));
        g.accounts.id.renderButton(box.current, { theme: "outline", size: "large", text, shape: "rectangular", logo_alignment: "center", width });
      })
      .catch((e: Error) => onError?.(e.message));
    return () => {
      cancelled = true;
    };
  }, [enabled, text, onError]);

  if (!enabled) return null;
  // fixed height while Google's iframe loads, so nothing jumps
  return <div ref={box} className="flex min-h-11 w-full justify-center [color-scheme:light]" />;
}
