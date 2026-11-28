"use client";

import { useCallback, useEffect, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { GoogleButton } from "@/components/ui/GoogleButton";
import { Note } from "@/components/ui/Doodles";
import { Skeleton } from "@/components/ui/Skeleton";
import { api, ApiError } from "@/lib/api";
import { useGoogleEnabled } from "@/lib/google";

interface Methods {
  email: string;
  passwordSet: boolean;
  googleLinked: boolean;
  googleAvailable: boolean;
}

/** Settings → Sign-in: link/unlink Google, and a first password for Google-only accounts (so unlinking is safe). */
export function SignInMethods({ onStatus }: { onStatus: (s: string) => void }) {
  const [m, setM] = useState<Methods | null>(null);
  const [busy, setBusy] = useState(false);
  const [password, setPassword] = useState("");
  const [pwError, setPwError] = useState<string | null>(null);
  const canGoogle = useGoogleEnabled();

  const load = useCallback(() => api<Methods>("/api/users/me/sign-in").then(setM), []);
  useEffect(() => {
    load().catch(() => onStatus("Couldn't load your sign-in settings."));
  }, [load, onStatus]);

  const fail = (e: unknown) => onStatus((e as ApiError).message || "That didn't work. Try again.");

  const link = async (idToken: string) => {
    setBusy(true);
    try {
      await api("/api/users/me/google", { method: "POST", body: { idToken } });
      await load();
      onStatus("Google is linked. You can now log in with it.");
    } catch (e) {
      fail(e);
    } finally {
      setBusy(false);
    }
  };

  const unlink = async () => {
    setBusy(true);
    try {
      await api("/api/users/me/google", { method: "DELETE" });
      await load();
      onStatus("Google is unlinked. Log in with your email and password from now on.");
    } catch (e) {
      fail(e);
    } finally {
      setBusy(false);
    }
  };

  const savePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (password.length < 8) return setPwError("Use at least 8 characters.");
    if (password.length > 72) return setPwError("72 characters max, please.");
    setPwError(null);
    setBusy(true);
    try {
      await api("/api/users/me/password", { method: "POST", body: { password } });
      setPassword("");
      await load();
      onStatus("Password saved. You can log in with your email too.");
    } catch (err) {
      fail(err);
    } finally {
      setBusy(false);
    }
  };

  if (!m) return <div className="flex flex-col gap-4 py-6"><Skeleton className="h-14" /></div>;

  return (
    <div className="flex flex-col gap-4 py-4">
      <div className="flex items-center gap-4">
        <svg aria-hidden width="22" height="22" viewBox="0 0 48 48" className="shrink-0"><path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/><path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/><path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/><path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/></svg>
        <div className="min-w-0 flex-1">
          <p className="font-medium">Google</p>
          <p className="truncate text-sm text-muted">{m.googleLinked ? `Linked · ${m.email}` : "Not linked"}</p>
        </div>
        {m.googleLinked && (
          <Button variant="secondary" disabled={busy || !m.passwordSet} onClick={() => void unlink()} aria-describedby={!m.passwordSet ? "unlink-why" : undefined}>
            Unlink
          </Button>
        )}
      </div>
      {!m.googleLinked && m.googleAvailable && canGoogle && (
        <div className="max-w-[400px]"><GoogleButton text="signin_with" onCredential={(t) => void link(t)} onError={onStatus} /></div>
      )}
      {m.googleLinked && !m.passwordSet && (
        <>
          <Note className="text-xl" >set a password first, so you can&rsquo;t get locked out</Note>
          <p id="unlink-why" className="sr-only">Unlink needs a password first, so you can still log in.</p>
          <form onSubmit={savePassword} className="flex flex-col gap-3 sm:flex-row sm:items-end" noValidate>
            <Field className="flex-1" label="New password" type="password" autoComplete="new-password" value={password}
              onChange={(e) => setPassword(e.target.value)} error={pwError} hint="At least 8 characters." />
            <Button type="submit" disabled={busy || !password}>Save password</Button>
          </form>
        </>
      )}
    </div>
  );
}
