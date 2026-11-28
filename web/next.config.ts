import type { NextConfig } from "next";

const api = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const apiWs = api.replace(/^http/, "ws"); // calls: the signalling WebSocket (ws:// locally, wss:// in production)
const media = process.env.NEXT_PUBLIC_MEDIA_ORIGIN ?? "http://localhost:9000";
const dev = process.env.NODE_ENV !== "production";
// BUILD_TARGET=mobile → static files for the Capacitor apps (iOS/Android). Default → the normal web server.
const mobile = process.env.BUILD_TARGET === "mobile";

// Content-Security-Policy: the page may only talk to our API and load photos from our storage.
// Next's bootstrap needs inline scripts; dev mode also needs eval for fast refresh.
// Sign in with Google (Google Identity Services): its script, button iframe, styles and token calls.
const gis = "https://accounts.google.com/gsi/";
const csp = [
  "default-src 'self'",
  `script-src 'self' 'unsafe-inline' https://checkout.razorpay.com ${gis}client${dev ? " 'unsafe-eval'" : ""}`,
  `style-src 'self' 'unsafe-inline' ${gis}style`,
  `img-src 'self' data: blob: ${media}`,
  "font-src 'self'",
  `connect-src 'self' ${api} ${apiWs} ${media} https://api.razorpay.com https://lumberjack.razorpay.com ${gis}${dev ? " ws:" : ""}`,
  `frame-src https://api.razorpay.com https://checkout.razorpay.com ${gis}`, // Razorpay Checkout; Google's sign-in button
  "media-src 'self' blob:",
  "worker-src 'self' blob:",
  "object-src 'none'",
  "base-uri 'none'",
  "form-action 'self'",
  "frame-ancestors 'none'",
].join("; ");

const securityHeaders = [
  { key: "Content-Security-Policy", value: csp },
  { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
  { key: "X-Content-Type-Options", value: "nosniff" },
  { key: "Permissions-Policy", value: "camera=(self), microphone=(self), geolocation=(self), payment=()" }, // camera/mic: calls only
];

const nextConfig: NextConfig = {
  reactStrictMode: true,
  poweredByHeader: false,
  images: { unoptimized: true }, // photos are short-lived signed links; resizing happens on upload (user-service)
  env: { NEXT_PUBLIC_MOBILE_CSP: mobile ? csp.replace("frame-ancestors 'none'", "").replace(/;\s*;/g, ";") : "" },
  ...(mobile
    ? {
        // Static export: no server, so no HTTP headers. The CSP goes in a <meta> tag instead (app/layout.tsx);
        // frame-ancestors is ignored in <meta>, and the app can't be framed by websites anyway.
        output: "export" as const,
        trailingSlash: true, // /swipe/index.html: what the apps' local file server resolves
      }
    : {
        async headers() {
          return [{ source: "/:path*", headers: securityHeaders }];
        },
      }),
};

export default nextConfig;
