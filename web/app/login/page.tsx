"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { GoogleButton } from "@/components/ui/GoogleButton";
import { Logo } from "@/components/ui/Logo";
import { Torn } from "@/components/ui/Torn";
import { api, ApiError } from "@/lib/api";
import { googleSignIn, setPendingGoogle, useGoogleEnabled } from "@/lib/google";
import { completeLogin, useSession } from "@/lib/session";
import { isNative, nativeAuthHeaders } from "@/lib/native";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [googleError, setGoogleError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const showGoogle = useGoogleEnabled();
  const session = useSession();
  useEffect(() => {
    if (session) router.replace("/swipe"); // still logged in from last time
  }, [session, router]);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const res = await api<{ accessToken: string; refreshToken?: string }>("/api/users/auth/login", {
        method: "POST", body: { email, password }, auth: false,
        credentials: isNative() ? "omit" : "include", headers: nativeAuthHeaders(),
      });
      await completeLogin(res);
      router.push("/swipe");
    } catch (err) {
      const status = err instanceof ApiError ? err.status : 0;
      setError(
        status === 401 ? "That email and password don't match." :
        status === 429 ? "Too many tries. Take a short break and try again in a few minutes." :
        (err as Error).message,
      );
      setBusy(false);
    }
  }

  const onGoogle = useCallback(async (idToken: string) => {
    setGoogleError(null);
    try {
      const r = await googleSignIn(idToken);
      if (r.kind === "in") return router.push("/swipe");
      // new here: one more question (birthday), then the usual profile steps
      setPendingGoogle({ idToken: r.idToken, email: r.email, suggestedName: r.suggestedName });
      router.push("/signup");
    } catch (err) {
      const e = err as ApiError;
      setGoogleError(e.status === 429 ? "Too many tries. Wait a minute and try again." : e.message);
    }
  }, [router]);

  return (
    <div className="mx-auto flex min-h-dvh max-w-md flex-col">
      <header className="relative bg-burst px-4 pb-10 pt-[max(1rem,env(safe-area-inset-top))]">
        <div className="flex items-center justify-between">
          <Logo onPattern />
          <span className="text-xs font-bold tracking-[0.14em] text-on-accent">18+</span>
        </div>
        <h1 className="mt-10 font-display text-[clamp(4.5rem,24vw,6rem)] leading-[0.88] text-on-accent">
          Back<br />again?
        </h1>
        <Torn />
      </header>

      <main id="main" className="flex flex-1 flex-col gap-4 px-4 pb-8 pt-6">
        {showGoogle && (
          <>
            <GoogleButton onCredential={onGoogle} onError={setGoogleError} />
            {googleError && <p role="alert" className="text-sm text-danger">{googleError}</p>}
            <div className="my-1 flex items-center gap-3 text-muted" aria-hidden>
              <span className="h-0.5 flex-1 bg-surface-2" />
              <span className="text-xs font-bold tracking-[0.14em] uppercase">or email</span>
              <span className="h-0.5 flex-1 bg-surface-2" />
            </div>
          </>
        )}
        <form onSubmit={submit} className="flex flex-col gap-5" noValidate>
          <Field label="Email" type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
          <Field label="Password" type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} error={error} />
          <Button type="submit" size="lg" disabled={busy || !email || !password}>
            {busy ? "Logging in…" : "Log in"}
          </Button>
        </form>
        <p className="mt-auto pt-6 text-center text-sm">
          New here?{" "}
          <Link href="/signup" className="inline-flex min-h-12 items-center font-bold text-accent underline-offset-4 hover:underline">
            Make a profile
          </Link>
        </p>
      </main>
    </div>
  );
}
