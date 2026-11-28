import { Hero, Marquee } from "@/components/landing/Hero";
import { ClosingCta, Features, Footer, HowItWorks, Safety } from "@/components/landing/Sections";
import { Logo } from "@/components/ui/Logo";
import { ButtonLink } from "@/components/ui/Button";

export default function Landing() {
  return (
    <>
      <header className="absolute inset-x-0 top-0 z-20 bg-bg">
        <div className="mx-auto flex max-w-[1120px] items-center justify-between px-4 py-4 sm:px-6">
          <Logo />
          <ButtonLink href="/login" variant="secondary">Log in</ButtonLink>
        </div>
      </header>
      <main id="main">
        <Hero />
        <Marquee />
        <HowItWorks />
        <Safety />
        <Features />
        <ClosingCta />
      </main>
      <Footer />
    </>
  );
}
