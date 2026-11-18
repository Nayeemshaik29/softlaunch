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
import { Logo } from "@/components/ui/Logo";
import { api, ApiError } from "@/lib/api";
import { spring } from "@/lib/motion";
import { startSession } from "@/lib/session";
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
  const [step, setStep] = useState(1);
  const [dir, setDir] = useState(1);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [touched, setTouched] = useState(false);

  const [name, setName] = useState("");
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
      if (step === 3) {
        // create the account, then log in: from here on, steps save to the profile
        await api("/api/users/auth/signup", { method: "POST", auth: false, body: { email: email.trim(), password, displayName: name.trim(), dateOfBirth: dob } });
        const res = await api<{ accessToken: string }>("/api/users/auth/login", { method: "POST", auth: false, body: { email: email.trim(), password } });
        startSession(res.accessToken);
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
        err.status === 409 ? "That email already has an account." :
        err.status === 429 ? "Too many tries from this network. Wait a minute and try again." :
        err.problem?.errors ? Object.values(err.problem.errors)[0] : err.message,
      );
    } finally {
      setBusy(false);
    }
  }

  const locate = () => {
    if (!navigator.geolocation) return setError("Your browser can't share location. Type your city instead.");
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      (p) => { setCoords({ lat: p.coords.latitude, lon: p.coords.longitude }); setLocating(false); },
      () => { setLocating(false); setError("No problem. Type your city; you can share location later for nearby matches."); },
      { enableHighAccuracy: false, timeout: 10_000, maximumAge: 600_000 },
    );
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
                <h1 ref={heading} tabIndex={-1} className="font-display text-3xl font-normal outline-none sm:text-4xl">{titles[step - 1]}</h1>

                <div className="mt-8 flex flex-col gap-6">
                  {step === 1 && (
                    <Field big label="Your first name" autoComplete="given-name" autoFocus value={name} maxLength={60}
                      onChange={(e) => setName(e.target.value)} error={shown} hint="This is how you'll appear. You can change it later." />
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
                    <Button type="button" variant="ghost" onClick={() => go(step - 1)}>Back</Button>
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
