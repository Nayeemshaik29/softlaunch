"use client";

import { api } from "./api";
import type { MyProfile, PublicProfile } from "./types";

let mine: Promise<MyProfile | null> | null = null;

/** My profile, fetched once per page load (null if I haven't made one yet). */
export function myProfile(force = false): Promise<MyProfile | null> {
  if (!mine || force) mine = api<MyProfile>("/api/users/me/profile").catch(() => null);
  return mine;
}

const people = new Map<string, Promise<PublicProfile | null>>();
/** fresh = skip the cache (e.g. right after a match, when veiled photos become real). */
export function profileOf(userId: string, fresh = false): Promise<PublicProfile | null> {
  if (fresh || !people.has(userId)) people.set(userId, api<PublicProfile>(`/api/users/${userId}/profile`).catch(() => null));
  return people.get(userId)!;
}
