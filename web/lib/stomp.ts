"use client";

/**
 * A tiny STOMP 1.2 client over a plain WebSocket (Spring's /ws/chat endpoint), so we don't ship a library for it.
 * Connects with the current access token, re-subscribes after reconnects, and backs off when the network is gone.
 */
type Handler = (body: unknown) => void;

const EOL = "\n";
const NUL = "\u0000";

function frame(command: string, headers: Record<string, string>, body = "") {
  const h = Object.entries(headers).map(([k, v]) => `${k}:${v.replace(/\\/g, "\\\\").replace(/\n/g, "\\n").replace(/:/g, "\\c")}`);
  return [command, ...h, "", body].join(EOL) + NUL;
}

function parse(raw: string): { command: string; headers: Record<string, string>; body: string } | null {
  const end = raw.indexOf(EOL + EOL);
  if (end < 0) return null;
  const [command, ...lines] = raw.slice(0, end).split(EOL);
  const headers: Record<string, string> = {};
  for (const l of lines) {
    const i = l.indexOf(":");
    if (i > 0 && !(l.slice(0, i) in headers)) headers[l.slice(0, i)] = l.slice(i + 1).replace(/\\c/g, ":").replace(/\\n/g, "\n").replace(/\\\\/g, "\\");
  }
  return { command: command.trim(), headers, body: raw.slice(end + 2) };
}

export class StompClient {
  private ws: WebSocket | null = null;
  private connected = false;
  private stopped = false;
  private retry = 0;
  private nextId = 0;
  private subs = new Map<string, { dest: string; handler: Handler }>();
  private outbox: string[] = [];
  private beat: number | undefined;
  private listeners = new Set<(up: boolean) => void>();

  constructor(private url: string, private token: () => string | null) {}

  start() {
    this.stopped = false;
    this.open();
  }

  stop() {
    this.stopped = true;
    window.clearInterval(this.beat);
    try {
      if (this.connected) this.ws?.send(frame("DISCONNECT", {}));
    } catch {
      /* already closed */
    }
    this.ws?.close();
    this.ws = null;
    this.connected = false;
  }

  onStatus(l: (up: boolean) => void) {
    this.listeners.add(l);
    return () => this.listeners.delete(l);
  }

  subscribe(dest: string, handler: Handler) {
    const id = `s${this.nextId++}`;
    this.subs.set(id, { dest, handler });
    if (this.connected) this.ws?.send(frame("SUBSCRIBE", { id, destination: dest }));
    return () => {
      this.subs.delete(id);
      if (this.connected) this.ws?.send(frame("UNSUBSCRIBE", { id }));
    };
  }

  send(dest: string, body: unknown = {}, headers: Record<string, string> = {}) {
    const f = frame("SEND", { destination: dest, "content-type": "application/json", ...headers }, JSON.stringify(body));
    if (this.connected && this.ws?.readyState === WebSocket.OPEN) this.ws.send(f);
    else this.outbox.push(f); // sent right after (re)connecting
  }

  private open() {
    const token = this.token();
    if (!token || this.stopped) return;
    const ws = new WebSocket(this.url);
    this.ws = ws;
    let buffer = "";
    ws.onopen = () => ws.send(frame("CONNECT", { "accept-version": "1.2", "heart-beat": "0,0", Authorization: `Bearer ${token}` }));
    ws.onmessage = (e) => {
      buffer += typeof e.data === "string" ? e.data : "";
      let i: number;
      while ((i = buffer.indexOf(NUL)) >= 0) {
        const raw = buffer.slice(0, i).replace(/^\n+/, "");
        buffer = buffer.slice(i + 1);
        const f = parse(raw);
        if (!f) continue;
        if (f.command === "CONNECTED") {
          this.connected = true;
          this.retry = 0;
          this.subs.forEach((s, id) => ws.send(frame("SUBSCRIBE", { id, destination: s.dest })));
          this.outbox.splice(0).forEach((m) => ws.send(m));
          window.clearInterval(this.beat);
          this.beat = window.setInterval(() => ws.readyState === WebSocket.OPEN && ws.send(EOL), 20_000); // keeps proxies from closing an idle socket
          this.listeners.forEach((l) => l(true));
        } else if (f.command === "MESSAGE") {
          const sub = this.subs.get(f.headers.subscription);
          if (!sub) continue;
          try {
            sub.handler(f.body ? JSON.parse(f.body) : null);
          } catch {
            /* a malformed message is ignored */
          }
        }
      }
    };
    ws.onclose = () => {
      const was = this.connected;
      this.connected = false;
      window.clearInterval(this.beat);
      if (was) this.listeners.forEach((l) => l(false));
      if (this.stopped) return;
      // 1s, 2s, 4s ... up to 30s; a fresh token is read on every attempt
      const wait = Math.min(30_000, 1000 * 2 ** this.retry++);
      window.setTimeout(() => this.open(), wait);
    };
    ws.onerror = () => ws.close();
  }
}
