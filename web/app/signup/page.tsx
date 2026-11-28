"use client";

import { AnimatePresence, LayoutGroup, motion, useReducedMotion } from "framer-motion";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useRef, useState } from "react";
import { CardPreview, pretty } from "@/components/onboarding/CardPreview";
import { Choice, Chip, CompletionRing, ProgressArc } from "@/components/onboarding/parts";
import { PhotoGrid } from "@/components/onboarding/PhotoGrid";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { Arrow, Note, Star } from "@/components/ui/Doodles";
import { GoogleButton } from "@/components/ui/GoogleButton";
import { Logo } from "@/components/ui/Logo";
import { GOOGLE_CLIENT_ID, googleSignIn, peekPendingGoogle, takePendingGoogle, type PendingGoogle } from "@/lib/google";
import { api, ApiError } from "@/lib/api";
import { currentPosition } from "@/lib/location";
import { spring } from "@/lib/motion";
import { completeLogin } from "@/lib/session";
import { isNative, nativeAuthHeaders } from "@/lib/native";
import type { Gender, PhotoRef } from "@/lib/types";

const STEPS = 5;
const GENDERS: { v: Gender; label: string }[] = [
  { v: "WOMAN", label: "Woman" }, { v: "MAN", label: "Man" }, { v: "NON_BINARY", label: "Non-binary" }, { v: "OTHER", label: "Something else" },
];
const INTENTS: { v: string; label: string }[] = [
  { v: "LONG_TERM", label: "A relationship" },
  { v: "LONG_TERM_OPEN_TO_SHORT", label: "Relationship, open to short" },
  { v: "SHORT_TERM_OPEN_TO_LONG", label: "Short-term, open to more" },
  { v: "CASUAL_DATING", label: "Casual dating" },
  { v: "HOOKUP", label: "Hookups" },
  { v: "FRIENDS_WITH_BENEFITS", label: "Friends with benefits" },
  { v: "NEW_FRIENDS", label: "New friends" },
];
const INTERESTS = ["TRAVEL", "MUSIC", "MOVIES", "GYM", "YOGA", "CRICKET", "FOOTBALL", "GAMING", "COOKING", "FOODIE", "COFFEE", "READING",
  "PHOTOGRAPHY", "DANCING", "HIKING", "ART", "TECH", "FASHION", "PETS", "ANIME", "STANDUP_COMEDY", "PARTYING", "MEDITATION", "VOLUNTEERING"];

function ageOn(dob: string): number | null {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(dob)) return null;
  const d = new Date(dob + "T00:00:00");
  if (Number.isNaN(d.getTime())) return null;
  const now = new Date();
  let a = now.getFullYear() - d.getFullYear();
  if (now.getMonth() < d.getMonth() || (now.getMonth() === d.getMonth() && now.getDate() < d.getDate())) a--;
  return a;
}

/** Mirrors user-service ProfileCompletion (the fields onboarding can fill). */
function completion(p: { bio: string; gender: string; interestedIn: string[]; lookingFor: string[]; interests: string[]; photos: PhotoRef[]; city: string }) {
  const checks = [!!p.bio.trim(), !!p.gender, p.interestedIn.length > 0, p.lookingFor.length > 0, p.interests.length >= 3,
    false /* places */, false /* prompts */, p.photos.length > 0, false /* job/education */, !!p.city.trim(), false /* habits */];
  return Math.floor((checks.filter(Boolean).length * 100) / checks.length);
}

export default function Signup() {
  const router = useRouter();
  const reduce = useReducedMotion();
  // step 0 = "Get in": Google or email. Without Google configured we start at the first question.
  // Arrived from /login (or the landing page) with a Google account we don't know yet: straight to the birthday.
  const [arrived] = useState(peekPendingGoogle);
  const [step, setStep] = useState(arrived ? (arrived.suggestedName ? 2 : 1) : GOOGLE_CLIENT_ID ? 0 : 1);
  // signed in with Google but new here: we only still need the birthday (Google doesn't share age)
  const [google, setGoogle] = useState<PendingGoogle | null>(arrived);
  const [dir, setDir] = useState(1);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [touched, setTouched] = useState(false);

  const [name, setName] = useState(arrived?.suggestedName ?? "");
  const [dob, setDob] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [gender, setGender] = useState<Gender | "">("");
  const [interestedIn, setInterestedIn] = useState<Gender[]>([]);
  const [lookingFor, setLookingFor] = useState<string[]>([]);
  const [city, setCity] = useState("");
  const [coords, setCoords] = useState<{ lat: number; lon: number } | null>(null);
  const [locating, setLocating] = useState(false);
  const [bio, setBio] = useState("");
  const [interests, setInterests] = useState<string[]>([]);
  const [photos, setPhotos] = useState<PhotoRef[]>([]);
  const heading = useRef<HTMLHeadingElement>(null);

  const age = ageOn(dob);
  const percent = completion({ bio, gender, interestedIn, lookingFor, interests, photos, city });

  useEffect(() => {
    takePendingGoogle(); // used once: the token now lives only in this page's state
  }, []);

  function startGoogle(p: PendingGoogle) {
    setGoogle(p);
    setName(p.suggestedName);
    setDir(1);
    setError(null);
    setStep(p.suggestedName ? 2 : 1);
  }

  async function onGoogle(idToken: string) {
    setError(null);
    try {
      const r = await googleSignIn(idToken);
      if (r.kind === "in") return router.push("/swipe"); // already has an account: just logged in
      startGoogle(r);
    } catch (e) {
      const err = e as ApiError;
      setError(err.status === 429 ? "Too many tries from this network. Wait a minute and try again." : err.message);
    }
  }

  // move focus to each new question (screen readers + keyboard users)
  useEffect(() => {
    heading.current?.focus();
  }, [step]);

  const toggle = <T,>(list: T[], v: T, set: (l: T[]) => void, max = 99) =>
    set(list.includes(v) ? list.filter((x) => x !== v) : list.length < max ? [...list, v] : list);

  // inline validation, one message per step
  const problem = useMemo(() => {
    if (step === 1) return name.trim().length === 0 ? "Tell us a name to show on your profile." : name.length > 50 ? "That's a bit long. 50 characters max." : null;
    if (step === 2) {
      if (!dob) return "Pick your date of birth.";
      if (age === null || age < 0 || new Date(dob) > new Date()) return "That date doesn't look right.";
      if (age < 18) return `SoftLaunch is for adults, so you'll need to be 18. We'd love to meet you in ${18 - age} year${18 - age === 1 ? "" : "s"}.`;
      if (age > 100) return "That date doesn't look right.";
      return null;
    }
    if (step === 3) {
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return "Enter an email address like name@example.com.";
      if (password.length < 8) return "Use at least 8 characters for your password.";
      if (password.length > 72) return "72 characters max, please.";
      return null;
    }
    if (step === 4) {
      if (!gender) return "Choose how you identify.";
      if (!interestedIn.length) return "Choose who you'd like to meet.";
      if (!lookingFor.length) return "Choose what you're looking for (up to 3).";
      if (!city.trim()) return "Add your city.";
      return null;
    }
    return null;
  }, [step, name, dob, age, email, password, gender, interestedIn, lookingFor, city]);

  // the 18+ message is friendly and live; others appear after a first try
  const shown = step === 2 && dob ? problem : touched ? problem : null;

  const go = (to: number) => {
    setDir(to > step ? 1 : -1);
    setTouched(false);
    setError(null);
    setStep(to);
  };

  const profileBody = () => ({
    gender, interestedIn, lookingFor, city: city.trim(), bio: bio.trim() || null, interests,
    latitude: coords?.lat ?? null, longitude: coords?.lon ?? null, ageMin: 18, ageMax: 60, maxDistanceKm: 50,
  });

  async function next() {
    setTouched(true);
    if (problem) return;
    setBusy(true);
    setError(null);
    try {
      if (step === 2 && google) {
        // Google: create the account with the birthday now, skip the email/password step
        const r = await googleSignIn(google.idToken, { dateOfBirth: dob, displayName: name.trim() || google.suggestedName });
        if (r.kind !== "in") throw new Error("Something went wrong. Try Google again.");
        setGoogle(null);
        go(4);
        return;
      }
      if (step === 3) {
        // create the account, then log in: from here on, steps save to the profile
        await api("/api/users/auth/signup", { method: "POST", auth: false, body: { email: email.trim(), password, displayName: name.trim(), dateOfBirth: dob } });
        const res = await api<{ accessToken: string; refreshToken?: string }>("/api/users/auth/login", {
          method: "POST", auth: false, body: { email: email.trim(), password },
          credentials: isNative() ? "omit" : "include", headers: nativeAuthHeaders(),
        });
        await completeLogin(res);
      }
      if (step === 4) {
        await api("/api/users/me/profile", { method: "PUT", body: profileBody() });
        if (coords) await api("/api/discovery/me/location", { method: "PUT", body: { latitude: coords.lat, longitude: coords.lon } }).catch(() => {});
      }
      if (step === 5) {
        await api("/api/users/me/profile", { method: "PUT", body: profileBody() });
        router.push("/swipe");
        return;
      }
      go(step + 1);
    } catch (e) {
      const err = e as ApiError;
      setError(
        err.status === 409 && !google && step === 3 ? "That email already has an account." :
        err.status === 429 ? "Too many tries from this network. Wait a minute and try again." :
        err.problem?.errors ? Object.values(err.problem.errors)[0] : err.message,
      );
    } finally {
      setBusy(false);
    }
  }

  const locate = () => {
    setLocating(true);
    currentPosition()
      .then((p) => setCoords({ lat: p.coords.latitude, lon: p.coords.longitude }))
      .catch(() => setError("No problem. Type your city; you can share location later for nearby matches."))
      .finally(() => setLocating(false));
  };


  const variants = {
    enter: (d: number) => (reduce ? { opacity: 0 } : { opacity: 0, x: 48 * d }),
    center: { opacity: 1, x: 0 },
    exit: (d: number) => (reduce ? { opacity: 0 } : { opacity: 0, x: -32 * d }),
  };

  const titles = [
    "First, what should we call you?",
    "When's your birthday?",
    "Create your login",
    "You, and who you'd like to meet",
    "Show them who you are",
  ];

  if (step === 0) {
    return (
      <div className="mx-auto flex min-h-dvh max-w-md flex-col px-4 pb-[max(1.5rem,env(safe-area-inset-bottom))] pt-[max(1rem,env(safe-area-inset-top))]">
        <div className="flex items-center justify-between">
          <Logo />
          <Link href="/login" className="inline-flex min-h-12 items-center text-xs font-bold tracking-[0.14em] text-accent uppercase">Log in</Link>
        </div>
        <main id="main" className="flex flex-1 flex-col">
          <div className="relative mt-6">
            <h1 className="font-display text-[clamp(6rem,32vw,8rem)] leading-[0.86]">Get<br /><span className="text-accent">in.</span></h1>
            <span className="absolute right-4 top-6 rotate-12"><Star size={56} /></span>
            <Note className="absolute right-2 top-36 -rotate-6 text-right">no endless<br />swiping, promise</Note>
          </div>
          <p className="mt-6 text-lg text-muted">Fewer, better matches. Chats that fade in 24 hours. Verified people.</p>
          <div className="mt-auto flex flex-col gap-4 pt-10">
            <GoogleButton text="signup_with" onCredential={(t) => void onGoogle(t)} onError={setError} />
            <Button type="button" variant="secondary" size="lg" onClick={() => go(1)}>Use email instead</Button>
            {error && <p role="alert" className="text-sm text-danger">{error}</p>}
            <p className="text-center text-xs text-muted">
              18+ only. By continuing you agree to our{" "}
              <a className="text-accent underline underline-offset-4" href={`${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"}/privacy`} target="_blank" rel="noreferrer">Privacy Policy</a>.
            </p>
          </div>
        </main>
      </div>
    );
  }

  return (
    <LayoutGroup>
      <div className="mx-auto grid min-h-dvh max-w-[1120px] gap-12 px-4 py-6 sm:px-6 lg:grid-cols-[1fr_380px]">
        <div className="flex flex-col">
          <div className="flex items-center justify-between">
            <Logo />
            <ProgressArc step={step} total={STEPS} />
          </div>

          <main id="main" className="flex flex-1 flex-col py-8 sm:py-12">
            <AnimatePresence mode="wait" custom={dir} initial={false}>
              <motion.form
                key={step}
                custom={dir}
                variants={variants}
                initial="enter"
                animate="center"
                exit="exit"
                transition={reduce ? { duration: 0.15 } : spring.gentle}
                onSubmit={(e) => { e.preventDefault(); void next(); }}
                noValidate
                className="flex flex-1 flex-col"
              >
                {google && step <= 2 && (
                  <div className="mb-6 flex flex-col items-start gap-4">
                    <span className="inline-flex items-center gap-2 rounded-full bg-white py-1.5 pl-2 pr-3 text-sm font-medium text-[#1f1f1f]">
                      <svg aria-hidden width="16" height="16" viewBox="0 0 48 48"><path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/><path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/><path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/><path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/></svg>
                      {google.email}
                    </span>
                    {step === 2 && <p className="text-xs font-bold tracking-[0.14em] text-accent uppercase">One thing Google doesn&rsquo;t tell us</p>}
                  </div>
                )}
                <h1 ref={heading} tabIndex={-1} className="font-display text-4xl outline-none sm:text-[4.5rem] sm:leading-[0.9]">{titles[step - 1]}</h1>

                <div className="mt-8 flex flex-col gap-6">
                  {step === 1 && (
                    <Field big label="Your first name" autoComplete="given-name" autoFocus value={name} maxLength={60}
                      onChange={(e) => setName(e.target.value)} error={shown} hint="This is how you'll appear. You can change it later." />
                  )}

                  {step === 2 && google && (
                    <div className="-mb-2 flex items-start gap-2" aria-hidden>
                      <Arrow />
                      <Note className="mt-2">we only show your age, never the date</Note>
                    </div>
                  )}
                  {step === 2 && (
                    <Field big label="Date of birth" type="date" value={dob} max={new Date().toISOString().slice(0, 10)}
                      onChange={(e) => setDob(e.target.value)} error={shown}
                      hint={age !== null && age >= 18 ? `You're ${age}. Only your age is shown, never your birthday.` : "Only your age is shown, never your birthday."} />
                  )}

                  {step === 3 && (
                    <>
                      <Field label="Email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} error={touched && problem?.includes("email") ? problem : null} />
                      <Field label="Password" type="password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)}
                        error={touched && problem?.includes("character") ? problem : null} hint="At least 8 characters." />
                      <p className="text-sm text-muted">
                        By continuing you agree to our{" "}
                        <a className="text-accent underline underline-offset-4" href={`${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"}/privacy`} target="_blank" rel="noreferrer">Privacy Policy</a>{" "}
                        and confirm you&rsquo;re 18 or older.
                      </p>
                    </>
                  )}

                  {step === 4 && (
                    <>
                      <fieldset>
                        <legend className="mb-2 text-sm font-medium">I am a</legend>
                        <div role="radiogroup" className="grid grid-cols-2 gap-2">
                          {GENDERS.map((g) => <Choice key={g.v} selected={gender === g.v} onClick={() => setGender(g.v)}>{g.label}</Choice>)}
                        </div>
                      </fieldset>
                      <fieldset>
                        <legend className="mb-2 text-sm font-medium">I&rsquo;d like to meet</legend>
                        <div className="grid grid-cols-2 gap-2">
                          {GENDERS.slice(0, 3).map((g) => (
                            <Choice role="checkbox" key={g.v} selected={interestedIn.includes(g.v)} onClick={() => toggle(interestedIn, g.v, setInterestedIn)}>
                              {g.v === "WOMAN" ? "Women" : g.v === "MAN" ? "Men" : "Non-binary people"}
                            </Choice>
                          ))}
                        </div>
                      </fieldset>
                      <fieldset>
                        <legend className="mb-2 text-sm font-medium">I&rsquo;m looking for <span className="text-muted">(up to 3)</span></legend>
                        <div className="flex flex-wrap gap-2">
                          {INTENTS.map((i) => <Chip key={i.v} selected={lookingFor.includes(i.v)} disabled={lookingFor.length >= 3} onClick={() => toggle(lookingFor, i.v, setLookingFor, 3)}>{i.label}</Chip>)}
                        </div>
                      </fieldset>
                      <div className="grid gap-2 sm:grid-cols-[1fr_auto] sm:items-end">
                        <Field label="City" autoComplete="address-level2" value={city} onChange={(e) => setCity(e.target.value)} />
                        <Button type="button" variant="secondary" onClick={locate} disabled={locating} aria-describedby="loc-hint">
                          {coords ? "Location added" : locating ? "Finding you…" : "Use my location"}
                        </Button>
                      </div>
                      <p id="loc-hint" className="-mt-4 text-xs text-muted">We only show others a rounded distance, never where you are.</p>
                      {touched && problem && <p role="alert" className="text-sm text-danger">{problem}</p>}
                    </>
                  )}

                  {step === 5 && (
                    <>
                      <CompletionRing percent={percent} />
                      <PhotoGrid photos={photos} setPhotos={setPhotos} />
                      <div>
                        <label htmlFor="bio" className="mb-2 block text-sm font-medium">A line about you</label>
                        <textarea id="bio" rows={3} maxLength={500} value={bio} onChange={(e) => setBio(e.target.value)}
                          placeholder="Weekend treks, filter coffee, and a book I'll never finish."
                          className="w-full rounded-md border border-line bg-surface-2 px-4 py-4 text-base outline-none placeholder:text-muted focus-visible:border-focus" />
                        <p className="mt-1 text-right text-xs text-muted tabular">{bio.length}/500</p>
                      </div>
                      <fieldset>
                        <legend className="mb-2 text-sm font-medium">Things you love <span className="text-muted">(pick 3 or more)</span></legend>
                        <div className="flex flex-wrap gap-2">
                          {INTERESTS.map((i) => <Chip key={i} selected={interests.includes(i)} disabled={interests.length >= 10} onClick={() => toggle(interests, i, setInterests, 10)}>{pretty(i)}</Chip>)}
                        </div>
                      </fieldset>
                      <div className="lg:hidden">
                        <p className="mb-2 text-sm font-medium">Your card</p>
                        <div className="mx-auto max-w-[280px]">
                          <CardPreview name={name} age={age} city={city} bio={bio} interests={interests} photo={photos[0]?.url ?? null} />
                        </div>
                      </div>
                    </>
                  )}
                </div>

                {error && <p role="alert" className="mt-6 text-sm text-danger">{error}{error.includes("already has an account") && <> <Link href="/login" className="underline">Log in instead</Link></>}</p>}

                <motion.div layoutId="onboarding-cta" className="mt-auto flex items-center justify-between gap-4 pt-12">
                  {step > 1 && step !== 4 ? (
                    <Button type="button" variant="ghost" onClick={() => {
                      if (google) { setGoogle(null); return go(0); }
                      go(step - 1);
                    }}>Back</Button>
                  ) : step === 1 && GOOGLE_CLIENT_ID ? (
                    <Button type="button" variant="ghost" onClick={() => go(0)}>Back</Button>
                  ) : <span />}
                  <Button type="submit" size="lg" disabled={busy || (step === 2 && !!problem && !!dob)}>
                    {busy ? "One moment…" : step === 3 ? "Create account" : step === 5 ? (photos.length ? "Start discovering" : "Skip photos for now") : "Continue"}
                  </Button>
                </motion.div>
              </motion.form>
            </AnimatePresence>
          </main>
        </div>

        <aside className="hidden pt-24 lg:block" aria-label="Preview of your card">
          <div className="sticky top-24">
            <p className="mb-4 text-sm text-muted">This is how you&rsquo;ll appear in the deck</p>
            <CardPreview name={name} age={age} city={city} bio={bio} interests={interests} photo={photos[0]?.url ?? null} />
          </div>
        </aside>
      </div>
    </LayoutGroup>
  );
}
