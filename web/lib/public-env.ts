// Safe to import from server components (no "use client").
export const API_URL_PUBLIC = (process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080").replace(/\/$/, "");
