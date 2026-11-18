"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { Logo } from "@/components/ui/Logo";
import { api, ApiError } from "@/lib/api";
import { startSession, useSession } from "@/lib/session";
import { useEffect } from "react";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const session = useSession();
  useEffect(() => {
    if (session) router.replace("/swipe"); // still logged in from last time
  }, [session, router]);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const res = await api<{ accessToken: string }>("/api/users/auth/login", { method: "POST", body: { email, password }, auth: false, credentials: "include" });
      startSession(res.accessToken);
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

  return (
    <div className="mx-auto flex min-h-dvh max-w-md flex-col px-4 py-6">
      <Logo />
      <main id="main" className="flex flex-1 flex-col justify-center py-12">
        <h1 className="font-display text-3xl font-normal">Welcome back</h1>
        <p className="mt-2 text-muted">Log in to see who&rsquo;s new nearby.</p>
        <form onSubmit={submit} className="mt-8 flex flex-col gap-6" noValidate>
          <Field label="Email" type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
          <Field label="Password" type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} error={error} />
          <Button type="submit" size="lg" disabled={busy || !email || !password}>
            {busy ? "Logging in…" : "Log in"}
          </Button>
        </form>
        <p className="mt-8 text-sm text-muted">
          New here?{" "}
          <Link href="/signup" className="inline-flex min-h-12 items-center font-medium text-accent underline-offset-4 hover:underline">
            Create your profile
          </Link>
        </p>
      </main>
    </div>
  );
}
