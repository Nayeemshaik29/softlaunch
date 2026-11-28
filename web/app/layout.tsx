import type { Metadata, Viewport } from "next";
import { Archivo_Black, Bricolage_Grotesque, Caveat, Instrument_Sans } from "next/font/google";
import "./globals.css";

// Display: condensed, extra-bold caps. Hand: little handwritten notes. Sans: everything you read.
const bricolage = Bricolage_Grotesque({ subsets: ["latin"], axes: ["wdth", "opsz"], variable: "--font-bricolage", display: "swap" });
// Wordmark only ("SoftLaunch" next to the peach)
const archivo = Archivo_Black({ subsets: ["latin"], weight: "400", variable: "--font-wordmark", display: "swap" });
const caveat = Caveat({ subsets: ["latin"], weight: "600", variable: "--font-caveat", display: "swap" });
const instrument = Instrument_Sans({ subsets: ["latin"], variable: "--font-instrument", display: "swap" });

export const metadata: Metadata = {
  title: { default: "SoftLaunch: dating that doesn't waste your evening", template: "%s · SoftLaunch" },
  description: "A calmer dating app. Fewer, better matches, verified profiles, and chats that respect your time.",
};

export const viewport: Viewport = {
  viewportFit: "cover", // phone apps: draw under the notch / home bar; padding comes from env(safe-area-inset-*)
  themeColor: "#0b0b0c",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={`${bricolage.variable} ${caveat.variable} ${archivo.variable} ${instrument.variable}`}>
      {process.env.NEXT_PUBLIC_MOBILE_CSP && (
        <head>
          {/* phone apps only: static files can't send a CSP header */}
          <meta httpEquiv="Content-Security-Policy" content={process.env.NEXT_PUBLIC_MOBILE_CSP} />
        </head>
      )}
      <body>
        <a href="#main" className="sr-only-focusable fixed left-4 top-4 z-50 rounded-md bg-surface px-4 py-2">
          Skip to content
        </a>
        {children}
      </body>
    </html>
  );
}
