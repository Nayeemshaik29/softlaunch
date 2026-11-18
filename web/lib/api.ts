"use client";

import { endSession, getSession } from "./session";
import type { Problem } from "./types";

export const API_URL = (process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080").replace(/\/$/, "");

export class ApiError extends Error {
  constructor(public status: number, public problem: Problem) {
    super(problem.detail ?? `HTTP ${status}`);
  }
  /** The daily swipe allowance ran out (match-service). NOT the gateway's anti-spam 429, which has no body. */
  get isSwipeQuota() {
    return this.status === 429 && this.problem.code === "SWIPE_QUOTA";
  }
}

interface Options {
  method?: string;
  body?: unknown;
  signal?: AbortSignal;
  auth?: boolean;
}

export async function api<T>(path: string, { method = "GET", body, signal, auth = true }: Options = {}): Promise<T> {
  const headers: Record<string, string> = {};
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const session = auth ? getSession() : null;
  if (session) headers.Authorization = `Bearer ${session.token}`;

  let res: Response;
  try {
    res = await fetch(`${API_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      // never wait forever: 12s, then the UI rolls back (the gateway itself gives up after 10s)
      signal: signal ? AbortSignal.any([signal, AbortSignal.timeout(12_000)]) : AbortSignal.timeout(12_000),
      credentials: "omit",
    });
  } catch (e) {
    if ((e as Error).name === "AbortError") throw e;
    if ((e as Error).name === "TimeoutError") throw new ApiError(0, { detail: "SoftLaunch is taking too long. Try again." });
    throw new ApiError(0, { detail: "Can't reach SoftLaunch. Check your connection." });
  }

  if (res.status === 401 && auth && session) endSession(); // expired or revoked token: back to login
  if (!res.ok) {
    let problem: Problem = {};
    try {
      problem = await res.json();
    } catch {
      /* empty body (e.g. gateway 401/429) */
    }
    throw new ApiError(res.status, problem);
  }
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
