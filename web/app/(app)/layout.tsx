"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { AppHeader, BottomNav } from "@/components/app/AppHeader";
import { NotificationWatcher } from "@/components/app/NotificationWatcher";
import { EnergyProvider } from "@/lib/energy";
import { MatchMomentProvider } from "@/lib/match-moment";
import { MoodProvider } from "@/lib/mood";
import { useSession } from "@/lib/session";
import { setupNative } from "@/lib/native";

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const session = useSession();
  const router = useRouter();
  const [unread, setUnread] = useState(0);

  useEffect(() => {
    if (session === null) router.replace("/login");
  }, [session, router]);

  // phone apps: status bar, Android back button, splash screen (no-op on the website)
  useEffect(() => {
    let cleanup = () => {};
    void setupNative().then((c) => (cleanup = c));
    return () => cleanup();
  }, []);

  if (!session) {
    return <div className="min-h-dvh" aria-busy="true" />;
  }
  return (
    <MoodProvider>
    <EnergyProvider>
      <MatchMomentProvider>
        <NotificationWatcher onUnread={setUnread} />
        <AppHeader unread={unread} />
        <main id="main" className="mx-auto max-w-[1120px] px-4 pb-24 pt-6 sm:px-6 md:pb-12">
          {children}
        </main>
        <BottomNav unread={unread} />
      </MatchMomentProvider>
    </EnergyProvider>
    </MoodProvider>
  );
}
