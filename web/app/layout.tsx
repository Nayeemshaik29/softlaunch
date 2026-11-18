import type { Metadata, Viewport } from "next";
import { Fraunces, Instrument_Sans } from "next/font/google";
import "./globals.css";

const fraunces = Fraunces({
  subsets: ["latin"],
  axes: ["SOFT", "opsz"],
  variable: "--font-fraunces",
  display: "swap",
});
const instrument = Instrument_Sans({ subsets: ["latin"], variable: "--font-instrument", display: "swap" });

export const metadata: Metadata = {
  title: { default: "SoftLaunch: dating that doesn't waste your evening", template: "%s · SoftLaunch" },
  description: "A calmer dating app. Fewer, better matches, verified profiles, and chats that respect your time.",
};

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: dark)", color: "#0e0c10" },
    { media: "(prefers-color-scheme: light)", color: "#fbf7f3" },
  ],
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={`${fraunces.variable} ${instrument.variable}`}>
      <body>
        <a href="#main" className="sr-only-focusable fixed left-4 top-4 z-50 rounded-md bg-surface px-4 py-2">
          Skip to content
        </a>
        {children}
      </body>
    </html>
  );
}
