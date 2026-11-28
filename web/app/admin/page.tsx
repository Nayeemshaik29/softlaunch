"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { Peach } from "@/components/ui/Peach";
import { api, ApiError } from "@/lib/api";
import { useSession } from "@/lib/session";

/**
 * Admin dashboard. The server checks the admin flag on EVERY call (and logs it); this page only hides itself.
 * Shows activity and counts. Never message text, call content, exact location, date of birth or ID documents.
 */

interface DayCount { day: string; count: number }
interface Overview { totalUsers: number; newUsers: number; newToday: number; activeToday: number; active7d: number; loginsToday: number; logins14d: DayCount[]; newUsers14d: DayCount[] }
interface Row {
  id: string; name: string; maskedEmail: string; age: number | null; city: string | null; photo: string | null;
  verified: boolean; suspended: boolean; admin: boolean; joined: string; lastActive: string | null;
  logins7d: number; swipes: number; likes: number; matches: number; messages: number; calls: number; callMinutes: number;
  reports: number; plusUntil: string | null; google: boolean; password: boolean;
}
interface Feed { userId: string; name: string; photo: string | null; kind: string; at: string }
interface Audit { adminName: string; action: string; targetName: string | null; at: string }

type View = "overview" | "users" | "reported" | "audit";

const FEED: Record<string, string> = {
  login: "logged in", signup_email: "signed up with email", signup_google: "signed up with Google", match: "matched",
  call_video: "started a video call", call_audio: "started an audio call", reported: "was reported",
};

function ago(iso: string | null) {
  if (!iso) return "never";
  const s = Math.max(0, (Date.now() - Date.parse(iso)) / 1000);
  if (s < 60) return "just now";
  if (s < 3600) return `${Math.floor(s / 60)} min ago`;
  if (s < 86400) return `${Math.floor(s / 3600)} h ago`;
  if (s < 172800) return "yesterday";
  return `${Math.floor(s / 86400)} days ago`;
}
const fmt = new Intl.NumberFormat("en-IN");
const isLive = (iso: string | null) => !!iso && Date.now() - Date.parse(iso) < 15 * 60_000;

function Avatar({ src, size = 34 }: { src: string | null; size?: number }) {
  // eslint-disable-next-line @next/next/no-img-element
  return src ? <img src={src} alt="" className="shrink-0 rounded-md border-2 border-text object-cover" style={{ width: size, height: size }} />
    : <span aria-hidden className="block shrink-0 rounded-md border-2 border-line bg-surface-2" style={{ width: size, height: size }} />;
}

function Kpi({ label, value, note, hl }: { label: string; value: number; note: string; hl?: boolean }) {
  return (
    <div className={`rounded-lg border-2 p-4 ${hl ? "border-bg bg-accent text-on-accent shadow-sticker" : "border-text bg-surface"}`}>
      <p className="text-xs font-bold tracking-[0.12em] uppercase opacity-85">{label}</p>
      <p className="mt-1 font-display text-5xl tabular">{fmt.format(value)}</p>
      <p className="mt-1 text-sm font-semibold">{note}</p>
    </div>
  );
}

function Bars({ data, label }: { data: DayCount[]; label: string }) {
  const max = Math.max(1, ...data.map((d) => d.count));
  return (
    <figure className="rounded-lg border-2 border-text bg-surface p-4">
      <figcaption className="flex justify-between text-xs font-bold tracking-[0.12em] text-accent uppercase">
        <span>{label}</span><span className="font-normal normal-case tracking-normal text-muted">today highlighted</span>
      </figcaption>
      <div className="mt-3 flex h-36 items-end gap-2" role="img" aria-label={`${label}: ${data.map((d) => `${d.day} ${d.count}`).join(", ")}`}>
        {data.map((d, i) => (
          <div key={d.day} title={`${d.day}: ${d.count}`} className="flex flex-1 flex-col items-center justify-end gap-1">
            <span className="text-[10px] text-muted tabular">{d.count || ""}</span>
            <div className={`w-full rounded-t-sm ${i === data.length - 1 ? "bg-accent" : "bg-accent-2"}`} style={{ height: `${Math.max(2, (d.count / max) * 112)}px` }} />
          </div>
        ))}
      </div>
    </figure>
  );
}

export default function AdminPage() {
  const session = useSession();
  const router = useRouter();
  const [allowed, setAllowed] = useState<boolean | null>(null);
  const [view, setView] = useState<View>("overview");
  const [days, setDays] = useState(1);
  const [overview, setOverview] = useState<Overview | null>(null);
  const [rows, setRows] = useState<Row[] | null>(null);
  const [filter, setFilter] = useState<"active" | "new">("active");
  const [q, setQ] = useState("");
  const [selected, setSelected] = useState<Row | null>(null);
  const [feed, setFeed] = useState<Feed[]>([]);
  const [audit, setAudit] = useState<Audit[] | null>(null);
  const [status, setStatus] = useState("");

  useEffect(() => {
    if (session === null) router.replace("/login");
    if (!session) return;
    api("/api/admin/me").then(() => setAllowed(true)).catch((e: ApiError) => {
      setAllowed(false);
      if (e.status !== 404) setStatus(e.message);
    });
  }, [session, router]);

  const loadUsers = useCallback(async () => {
    const f = view === "reported" ? "reported" : filter;
    const params = new URLSearchParams({ filter: f });
    if (q.trim()) params.set("q", q.trim());
    const page = await api<{ users: Row[] }>(`/api/admin/users?${params}`);
    setRows(page.users);
    setSelected((s) => page.users.find((u) => u.id === s?.id) ?? s ?? page.users[0] ?? null);
  }, [view, filter, q]);

  // overview + live feed refresh every 30s while the page is open
  useEffect(() => {
    if (!allowed) return;
    const load = () => {
      void api<Overview>(`/api/admin/overview?days=${days}`).then(setOverview).catch(() => setStatus("Couldn't load the overview."));
      void api<Feed[]>("/api/admin/feed").then(setFeed).catch(() => {});
    };
    load();
    const id = window.setInterval(load, 30_000);
    return () => window.clearInterval(id);
  }, [allowed, days]);

  useEffect(() => {
    if (!allowed) return;
    if (view === "audit") {
      void api<Audit[]>("/api/admin/audit").then(setAudit).catch(() => setStatus("Couldn't load the audit log."));
      return;
    }
    const id = window.setTimeout(() => void loadUsers().catch(() => setStatus("Couldn't load users.")), q ? 300 : 0);
    return () => window.clearTimeout(id);
  }, [allowed, view, loadUsers, q]);

  const suspend = async (u: Row) => {
    const verb = u.suspended ? "unsuspend" : "suspend";
    if (!u.suspended && !window.confirm(`Suspend ${u.name}? They're logged out everywhere and can't log back in until you unsuspend them.`)) return;
    try {
      const updated = await api<Row>(`/api/admin/users/${u.id}/${verb}`, { method: "POST" });
      setSelected(updated);
      setRows((r) => r?.map((x) => (x.id === updated.id ? updated : x)) ?? r);
      setStatus(updated.suspended ? `${u.name} is suspended.` : `${u.name} can log in again.`);
    } catch (e) {
      setStatus((e as Error).message);
    }
  };

  if (allowed === null) return <div className="min-h-dvh" aria-busy="true" />;
  if (!allowed) {
    return (
      <main id="main" className="grid min-h-dvh place-items-center px-4 text-center">
        <div>
          <h1 className="font-display text-5xl">Not here</h1>
          <p className="mt-2 text-muted">This page is only for SoftLaunch admins.</p>
          <a href="/swipe" className="mt-6 inline-flex min-h-12 items-center font-bold text-accent">Back to the app</a>
        </div>
      </main>
    );
  }

  const nav: [View, string][] = [["overview", "Overview"], ["users", "Users"], ["reported", "Reported"], ["audit", "Audit log"]];
  const showUsers = view === "overview" || view === "users" || view === "reported";

  return (
    <div className="grid min-h-dvh lg:grid-cols-[220px_1fr_340px]">
      <aside className="flex flex-col gap-1 border-b-2 border-surface-2 p-4 lg:border-b-0 lg:border-r-2">
        <a href="/swipe" className="mb-3 flex items-center gap-2 px-2 font-[family-name:var(--font-wordmark)] text-xl [text-shadow:2px_2px_0_var(--accent)]">
          <Peach size={32} />SoftLaunch
        </a>
        <span className="mx-2 mb-3 w-fit rounded-full bg-accent px-3 py-0.5 text-[11px] font-bold tracking-[0.1em] text-on-accent">ADMIN</span>
        <nav aria-label="Admin" className="flex gap-1 overflow-x-auto lg:flex-col">
          {nav.map(([v, label]) => (
            <button key={v} type="button" onClick={() => setView(v)} aria-current={view === v ? "page" : undefined}
              className={`min-h-11 shrink-0 rounded-md px-3 text-left text-sm font-bold ${view === v ? "bg-accent text-on-accent" : "hover:bg-surface-2"}`}>{label}</button>
          ))}
        </nav>
        <p className="mt-auto hidden px-2 pt-6 text-xs leading-relaxed text-muted lg:block">Every page you open here is written to the audit log.</p>
      </aside>

      <main id="main" className="min-w-0 p-5 lg:p-7">
        <p role="status" aria-live="polite" className="min-h-5 text-sm text-accent">{status}</p>
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="text-xs font-bold tracking-[0.14em] text-muted uppercase">
              {new Date().toLocaleDateString("en-IN", { weekday: "long", day: "numeric", month: "short", timeZone: "Asia/Kolkata" })} · IST
            </p>
            <h1 className="mt-1 font-display text-6xl">{nav.find(([v]) => v === view)?.[1]}</h1>
          </div>
          {view === "overview" && (
            <div className="flex gap-2" role="group" aria-label="Range for new users">
              {[[1, "Today"], [7, "7 days"], [30, "30 days"]].map(([d, l]) => (
                <button key={d} type="button" aria-pressed={days === d} onClick={() => setDays(d as number)}
                  className={`min-h-9 rounded-full border-[1.5px] px-4 text-sm font-semibold ${days === d ? "border-accent bg-accent text-on-accent" : "border-text"}`}>{l}</button>
              ))}
            </div>
          )}
        </div>

        {view === "overview" && overview && (
          <>
            <div className="mt-5 grid grid-cols-2 gap-3 md:grid-cols-5">
              <Kpi label="Total users" value={overview.totalUsers} note={`+${fmt.format(overview.newUsers)} in range`} />
              <Kpi label="New today" value={overview.newToday} note="accounts created" hl />
              <Kpi label="Active today" value={overview.activeToday} note={`DAU · ${overview.totalUsers ? Math.round((overview.activeToday * 100) / overview.totalUsers) : 0}%`} />
              <Kpi label="Active 7 days" value={overview.active7d} note={`WAU · ${overview.totalUsers ? Math.round((overview.active7d * 100) / overview.totalUsers) : 0}%`} />
              <Kpi label="Logins today" value={overview.loginsToday} note="password + Google" />
            </div>
            <div className="mt-4 grid gap-4 xl:grid-cols-2">
              <Bars data={overview.logins14d} label="Logins · last 14 days" />
              <Bars data={overview.newUsers14d} label="New users · last 14 days" />
            </div>
          </>
        )}

        {showUsers && (
          <section aria-labelledby="users-h" className="mt-5 overflow-hidden rounded-lg border-2 border-text bg-surface">
            <div className="flex flex-wrap items-center justify-between gap-3 p-4">
              <h2 id="users-h" className="text-xs font-bold tracking-[0.12em] text-accent uppercase">
                {view === "reported" ? "Most reported first" : q ? "Search results" : filter === "new" ? "Newest first" : "Most recently active first"}
              </h2>
              <div className="flex flex-wrap gap-2">
                <label className="sr-only" htmlFor="q">Search by name</label>
                <input id="q" value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search name…" maxLength={50}
                  className="min-h-9 w-56 rounded-md border-2 border-line bg-bg px-3 text-sm outline-none focus-visible:border-focus" />
                {view !== "reported" && (["active", "new"] as const).map((f) => (
                  <button key={f} type="button" aria-pressed={filter === f} onClick={() => setFilter(f)}
                    className={`min-h-9 rounded-full border-[1.5px] px-4 text-sm font-semibold ${filter === f ? "border-accent bg-accent text-on-accent" : "border-text"}`}>
                    {f === "active" ? "Active" : "New"}
                  </button>
                ))}
              </div>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full border-collapse text-sm">
                <thead>
                  <tr className="border-b-2 border-line text-left text-[11px] font-bold tracking-[0.12em] text-muted uppercase">
                    {["User", "Last active", "Logins 7d", "Swipes", "Matches", "Msgs", "Calls", "Reports", "Plus"].map((h) => <th key={h} scope="col" className="whitespace-nowrap px-2 py-2">{h}</th>)}
                  </tr>
                </thead>
                <tbody>
                  {rows === null ? (
                    <tr><td colSpan={9} className="px-3 py-6 text-muted">Loading…</td></tr>
                  ) : rows.length === 0 ? (
                    <tr><td colSpan={9} className="px-3 py-6 text-muted">{view === "reported" ? "Nobody has been reported. Good." : "No users here yet."}</td></tr>
                  ) : rows.map((u) => (
                    <tr key={u.id} onClick={() => setSelected(u)} aria-selected={selected?.id === u.id}
                      className={`cursor-pointer border-b border-surface-2 hover:bg-surface-2 ${selected?.id === u.id ? "bg-surface-2 shadow-[inset_3px_0_0_var(--accent)]" : ""}`}>
                      <td className="px-2 py-2">
                        <button type="button" onClick={() => setSelected(u)} className="flex items-center gap-2.5 text-left">
                          <Avatar src={u.photo} />
                          <span>
                            <span className="block font-bold">{u.name} {u.verified && <span className="text-accent" title="Verified">✓</span>}
                              {u.suspended && <span className="ml-1 rounded-sm bg-danger px-1 text-[10px] text-on-accent">SUSPENDED</span>}</span>
                            <span className="block text-xs text-muted">{u.maskedEmail}</span>
                          </span>
                        </button>
                      </td>
                      <td className="whitespace-nowrap px-2">{isLive(u.lastActive) && <span className="text-accent">● </span>}{ago(u.lastActive)}</td>
                      <td className="px-2 tabular">{u.logins7d}</td>
                      <td className="px-2 tabular">{u.swipes}</td>
                      <td className="px-2 tabular">{u.matches}</td>
                      <td className="px-2 tabular">{u.messages}</td>
                      <td className="px-2 tabular">{u.calls}</td>
                      <td className="px-2">{u.reports ? <span className="rounded-full border-[1.5px] border-danger px-2 text-xs text-danger">{u.reports}</span> : 0}</td>
                      <td className="px-2">{u.plusUntil ? <span className="rounded-full bg-accent px-2 text-xs font-semibold text-on-accent">Plus</span> : <span className="text-muted">—</span>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        )}

        {view === "audit" && (
          <section className="mt-5 overflow-hidden rounded-lg border-2 border-text bg-surface">
            <table className="w-full border-collapse text-sm">
              <thead><tr className="border-b-2 border-line text-left text-[11px] font-bold tracking-[0.12em] text-muted uppercase">
                <th className="px-3 py-2">When</th><th className="px-3 py-2">Admin</th><th className="px-3 py-2">Action</th><th className="px-3 py-2">User</th></tr></thead>
              <tbody>
                {(audit ?? []).map((a, i) => (
                  <tr key={i} className="border-b border-surface-2">
                    <td className="whitespace-nowrap px-3 py-2 text-muted">{new Date(a.at).toLocaleString("en-IN", { timeZone: "Asia/Kolkata" })}</td>
                    <td className="px-3">{a.adminName}</td><td className="px-3">{a.action.replace(/_/g, " ")}</td><td className="px-3">{a.targetName ?? "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>
        )}
      </main>

      <aside className="flex flex-col gap-4 border-t-2 border-surface-2 bg-[#100e0d] p-5 lg:border-l-2 lg:border-t-0">
        {selected && (
          <>
            <p className="text-xs font-bold tracking-[0.12em] text-muted uppercase">Selected</p>
            <div className="flex items-center gap-4">
              <span className="block shrink-0 -rotate-3 border-2 border-accent bg-accent-2 p-1 shadow-[3px_3px_0_var(--accent)]"><Avatar src={selected.photo} size={84} /></span>
              <div>
                <h2 className="font-display text-4xl leading-none">{selected.name}{selected.age !== null && `, ${selected.age}`}</h2>
                <p className="mt-1 text-sm text-muted">
                  {selected.city ?? "No city"} · {[selected.google && "Google", selected.password && "password"].filter(Boolean).join(" + ")}<br />
                  Joined {new Date(selected.joined).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" })}
                </p>
              </div>
            </div>
            <dl className="grid grid-cols-2 gap-2 text-sm">
              {[["Logins 7d", selected.logins7d], ["Last active", ago(selected.lastActive)], ["Swipes · likes", `${selected.swipes} · ${selected.likes}`],
                ["Matches", selected.matches], ["Msgs sent", selected.messages], ["Calls · min", `${selected.calls} · ${selected.callMinutes}`],
                ["Reports", selected.reports], ["Plus until", selected.plusUntil ? new Date(selected.plusUntil).toLocaleDateString("en-IN") : "—"]].map(([k, v]) => (
                <div key={k as string} className="rounded-lg border-2 border-text bg-surface p-2.5"><dt className="text-muted">{k}</dt><dd className="text-lg font-bold">{v}</dd></div>
              ))}
            </dl>
            <p className="rounded-md border-2 border-dashed border-line p-3 text-xs leading-relaxed text-muted">
              Not shown here, by design: message text, call content, exact location, date of birth, ID documents.
            </p>
            {!selected.admin && (
              <button type="button" onClick={() => void suspend(selected)}
                className={`min-h-11 rounded-md border-2 text-xs font-bold tracking-[0.08em] uppercase ${selected.suspended ? "border-text" : "border-danger text-danger"}`}>
                {selected.suspended ? "Unsuspend" : "Suspend"}
              </button>
            )}
          </>
        )}
        <section aria-labelledby="feed-h">
          <h2 id="feed-h" className="text-xs font-bold tracking-[0.12em] text-accent uppercase">Live activity</h2>
          <ul className="mt-2">
            {feed.length === 0 && <li className="py-2 text-sm text-muted">Nothing yet.</li>}
            {feed.slice(0, 12).map((f, i) => (
              <li key={i} className="flex items-center justify-between gap-2 border-b border-surface-2 py-2 text-sm">
                <span className="flex min-w-0 items-center gap-2"><Avatar src={f.photo} size={24} />
                  <span className="truncate"><b>{f.name}</b> <span className="text-muted">{FEED[f.kind] ?? f.kind}</span></span></span>
                <span className="shrink-0 text-xs text-muted">{ago(f.at)}</span>
              </li>
            ))}
          </ul>
        </section>
      </aside>
    </div>
  );
}
