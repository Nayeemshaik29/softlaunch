import { Reveal, RevealItem } from "./Reveal";
import { ButtonLink } from "@/components/ui/Button";
import { API_URL_PUBLIC } from "@/lib/public-env";

function Eyebrow({ children }: { children: React.ReactNode }) {
  return <p className="mb-4 text-xs font-bold tracking-[0.14em] text-accent uppercase">{children}</p>;
}

export function HowItWorks() {
  const steps = [
    { n: "01", title: "Sign in fast", body: "Google or email, five quick questions and a few photos. A live preview of your card as you go." },
    { n: "02", title: "Meet people near you", body: "A short, ranked deck: people close by who want what you want, and share how you spend your time." },
    { n: "03", title: "Talk, then log off", body: "When it's mutual, you'll know. Chats fade after a day, so conversations move to a real plan." },
  ];
  return (
    <section aria-labelledby="how" className="mx-auto max-w-[1120px] px-4 py-24 sm:px-6 sm:py-32">
      <Reveal className="grid gap-12">
        <RevealItem>
          <Eyebrow>How it works</Eyebrow>
          <h2 id="how" className="font-display text-[clamp(3rem,6vw,5.5rem)] leading-[0.9]">Three steps. Then log off.</h2>
        </RevealItem>
        <Reveal as="ol" className="grid gap-6 sm:grid-cols-3 md:col-span-2">
          {steps.map((s, i) => (
            <RevealItem as="li" key={s.n}
              className={`rounded-lg border-2 border-text p-6 shadow-sticker-accent ${i === 1 ? "rotate-1 bg-accent-2 text-on-accent" : "-rotate-1 bg-surface"}`}>
              <span className="font-display text-6xl text-accent tabular" aria-hidden>{s.n}</span>
              <h3 className="mt-3 font-display text-3xl">{s.title}</h3>
              <p className={`mt-3 ${i === 1 ? "" : "text-muted"}`}>{s.body}</p>
            </RevealItem>
          ))}
        </Reveal>
      </Reveal>
    </section>
  );
}

export function Safety() {
  const items = [
    { title: "Verified with DigiLocker", body: "An optional ID check gives a Verified badge. We never see or store your Aadhaar number." },
    { title: "Scam checks on our own servers", body: "New messages are checked for money requests and scam patterns. Nothing is sent to outside AI companies." },
    { title: "Block and report in one tap", body: "Blocking hides you from each other everywhere. Reports go to people, not just a robot." },
    { title: "Leave whenever you like", body: "Delete your account in the app or on the web. Your data is removed from every system within minutes." },
  ];
  return (
    <section aria-labelledby="safety" className="border-y border-line bg-surface">
      <div className="mx-auto max-w-[1120px] px-4 py-24 sm:px-6 sm:py-32">
        <Reveal>
          <RevealItem>
            <Eyebrow>Safety</Eyebrow>
            <h2 id="safety" className="max-w-xl font-display text-[clamp(3rem,6vw,5rem)] leading-[0.9]">Built so you can relax a little.</h2>
          </RevealItem>
          <Reveal as="ul" className="mt-12 grid gap-x-12 gap-y-8 sm:grid-cols-2">
            {items.map((i) => (
              <RevealItem as="li" key={i.title} className="border-t-2 border-text pt-5">
                <h3 className="font-display text-3xl">{i.title}</h3>
                <p className="mt-2 text-muted">{i.body}</p>
              </RevealItem>
            ))}
          </Reveal>
        </Reveal>
      </div>
    </section>
  );
}

export function Features() {
  return (
    <section aria-labelledby="features" className="mx-auto max-w-[1120px] px-4 py-24 sm:px-6 sm:py-32">
      <Reveal>
        <RevealItem>
          <Eyebrow>What&rsquo;s different</Eyebrow>
          <h2 id="features" className="max-w-xl font-display text-[clamp(3rem,6vw,5rem)] leading-[0.9]">Small choices that add up to calmer.</h2>
        </RevealItem>
        <div className="mt-12 grid gap-6 md:grid-cols-6">
          <RevealItem className="rounded-lg border-2 border-text bg-surface p-8 shadow-card md:col-span-4">
            <h3 className="font-display text-3xl">Daily energy, not infinite swipes</h3>
            <p className="mt-4 max-w-md text-muted">
              You get 50 swipes a day. When they&rsquo;re gone, the app rests until midnight. It turns out people choose more carefully
              when choices aren&rsquo;t endless.
            </p>
          </RevealItem>
          <RevealItem className="rounded-lg bg-surface-2 p-8 md:col-span-2">
            <h3 className="font-display text-3xl">Similar vibe first</h3>
            <p className="mt-4 text-muted">People who spend their weekends like you do appear higher.</p>
          </RevealItem>
          <RevealItem className="rounded-lg bg-surface-2 p-8 md:col-span-2">
            <h3 className="font-display text-3xl">A first line, if you want one</h3>
            <p className="mt-4 text-muted">Suggested openers based on what you both wrote. Edit or ignore.</p>
          </RevealItem>
          <RevealItem className="rounded-lg border-2 border-text bg-surface p-8 shadow-card md:col-span-4">
            <h3 className="font-display text-3xl">Chats that fade after 24 hours</h3>
            <p className="mt-4 max-w-md text-muted">
              Conversations are for making a plan, not keeping a pen pal. Messages disappear after a day; your match doesn&rsquo;t.
            </p>
          </RevealItem>
        </div>
      </Reveal>
    </section>
  );
}

export function ClosingCta() {
  return (
    <section className="border-y-2 border-text bg-ripple">
      <div className="mx-auto flex max-w-[1120px] justify-center px-4 py-24 sm:px-6 sm:py-32">
        <Reveal>
          <RevealItem className="flex max-w-2xl flex-col items-center gap-8 rounded-lg border-2 border-text bg-bg p-8 text-center shadow-[8px_8px_0_var(--accent)] sm:p-12">
            <h2 className="font-display text-[clamp(2.75rem,5vw,4.5rem)] leading-[0.9]">Tonight could be good.</h2>
            <ButtonLink href="/signup" size="lg" className="w-full sm:w-auto">Make your profile</ButtonLink>
          </RevealItem>
        </Reveal>
      </div>
    </section>
  );
}

export function Footer() {
  return (
    <footer className="border-t border-line">
      <div className="mx-auto flex max-w-[1120px] flex-col gap-4 px-4 py-8 text-sm text-muted sm:flex-row sm:items-center sm:justify-between sm:px-6">
        <p>© {new Date().getFullYear()} SoftLaunch · 18+ only</p>
        <nav aria-label="Legal" className="flex gap-6">
          <a className="inline-flex min-h-12 items-center hover:text-text" href={`${API_URL_PUBLIC}/privacy`}>Privacy</a>
          <a className="inline-flex min-h-12 items-center hover:text-text" href={`${API_URL_PUBLIC}/delete-account`}>Delete account</a>
        </nav>
      </div>
    </footer>
  );
}
