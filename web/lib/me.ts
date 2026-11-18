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
export function profileOf(userId: string): Promise<PublicProfile | null> {
  if (!people.has(userId)) people.set(userId, api<PublicProfile>(`/api/users/${userId}/profile`).catch(() => null));
  return people.get(userId)!;
}
