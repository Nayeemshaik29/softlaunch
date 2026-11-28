"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { api, API_URL } from "./api";
import { profileOf } from "./me";
import { haptic } from "./native";
import { getSession } from "./session";
import { StompClient } from "./stomp";

/**
 * Audio/video calls (WebRTC). The media goes straight between the two browsers, encrypted (DTLS-SRTP).
 * chat-service only rings and passes connection details, and only when BOTH people agreed to calls for that match.
 * Nothing is recorded, anywhere.
 */

export type CallMode = "OFF" | "AUDIO" | "VIDEO";
export interface Consent {
  mine: CallMode;
  theirs: CallMode;
  allowed: CallMode;
}

type Phase = "idle" | "outgoing" | "incoming" | "connecting" | "active" | "ended";
export interface Peer {
  userId: string;
  name: string;
  photo: string | null;
}
export interface CallState {
  phase: Phase;
  matchId: string | null;
  callId: string | null;
  peer: Peer | null;
  video: boolean; // the call allows video (both said yes)
  local: MediaStream | null;
  remote: MediaStream | null;
  muted: boolean;
  cameraOn: boolean;
  startedAt: number | null;
  videoAsk: "none" | "asking" | "asked"; // asking = I asked them; asked = they asked me
  endedReason: string | null;
}

interface CallEvent {
  type: string;
  matchId: string;
  callId: string | null;
  from: string | null;
  video: boolean | null;
  kind: string | null;
  payload: Record<string, unknown> | null;
  consent: Consent | null;
  reason: string | null;
}

interface Ctx {
  state: CallState;
  start(matchId: string, peer: Peer, video: boolean): Promise<void>;
  accept(withVideo: boolean): Promise<void>;
  decline(): void;
  end(): void;
  toggleMute(): void;
  toggleCamera(): Promise<void>;
  flip(): Promise<void>;
  askForVideo(): void;
  answerVideo(yes: boolean): void;
  dismiss(): void;
  /** live consent changes per match, pushed by the server */
  onConsent(matchId: string, l: (c: Consent) => void): () => void;
}

const idle: CallState = {
  phase: "idle", matchId: null, callId: null, peer: null, video: false, local: null, remote: null,
  muted: false, cameraOn: false, startedAt: null, videoAsk: "none", endedReason: null,
};

const CallCtx = createContext<Ctx | null>(null);
export const useCalls = () => {
  const c = useContext(CallCtx);
  if (!c) throw new Error("useCalls outside CallProvider");
  return c;
};

export const getConsent = (matchId: string) => api<Consent>(`/api/chats/${matchId}/calls/consent`);
export const setConsent = (matchId: string, mode: CallMode) =>
  api<Consent>(`/api/chats/${matchId}/calls/consent`, { method: "PUT", body: { mode } });

const ENDED_TEXT: Record<string, string> = {
  declined: "They can't talk right now.",
  busy: "They're on another call.",
  no_answer: "No answer.",
  consent_withdrawn: "Calls were turned off for this chat.",
  match_removed: "This match has ended.",
  not_allowed: "Calls unlock when you both say yes.",
  video_not_allowed: "Video needs both of you to allow it.",
  slow_down: "One moment before calling again.",
  you_are_busy: "You're already in a call.",
  media: "We couldn't use your microphone or camera. Check the permission and try again.",
  pass_needed: "Your free calling day is over. Get today's pass to call.",
  their_pass_needed: "They need today's pass too. Calls start when you both have one.",
  video_locked: "Video unlocks after you've called on 3 different days.",
  failed: "The call dropped. Try again.",
};
export const endedText = (r: string | null) => (r && ENDED_TEXT[r]) || "Call ended.";

export function CallProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<CallState>(idle);
  const ref = useRef(state);
  useEffect(() => {
    ref.current = state;
  }, [state]);
  const stomp = useRef<StompClient | null>(null);
  const pc = useRef<RTCPeerConnection | null>(null);
  const polite = useRef(false);
  const makingOffer = useRef(false);
  const ignoreOffer = useRef(false);
  const pendingIce = useRef<RTCIceCandidateInit[]>([]);
  const tracksAdded = useRef(false);
  const ringTimer = useRef<number | undefined>(undefined);
  const consentListeners = useRef(new Map<string, Set<(c: Consent) => void>>());

  const patch = useCallback((p: Partial<CallState>) => setState((s) => ({ ...s, ...p })), []);

  const send = useCallback((action: string, body: unknown = {}) => {
    const s = ref.current;
    if (!s.matchId) return;
    stomp.current?.send(`/app/calls/${s.matchId}/${action}`, body, s.callId ? { "call-id": s.callId } : {});
  }, []);

  /** Stop camera + mic, close the connection. Always safe to call. */
  const teardown = useCallback(() => {
    window.clearTimeout(ringTimer.current);
    pc.current?.close();
    pc.current = null;
    pendingIce.current = [];
    tracksAdded.current = false;
    makingOffer.current = false;
    ignoreOffer.current = false;
    ref.current.local?.getTracks().forEach((t) => t.stop());
  }, []);

  const finish = useCallback((reason: string | null) => {
    teardown();
    setState((s) => ({ ...idle, phase: "ended", peer: s.peer, endedReason: reason }));
  }, [teardown]);

  const media = useCallback(async (video: boolean) => {
    return navigator.mediaDevices.getUserMedia({
      audio: { echoCancellation: true, noiseSuppression: true },
      video: video ? { facingMode: "user", width: { ideal: 1280 }, height: { ideal: 720 } } : false,
    });
  }, []);

  const signal = useCallback((kind: string, payload: Record<string, unknown> = {}) => send("signal", { kind, payload }), [send]);

  /** One RTCPeerConnection per call. "Perfect negotiation": either side may (re)negotiate, collisions resolve themselves. */
  const makePeer = useCallback(async (isPolite: boolean) => {
    const { iceServers } = await api<{ iceServers: RTCIceServer[] }>("/api/chats/calls/ice-servers").catch(() => ({ iceServers: [] as RTCIceServer[] }));
    const conn = new RTCPeerConnection({ iceServers });
    polite.current = isPolite;
    const remote = new MediaStream();
    patch({ remote });
    conn.ontrack = (e) => {
      remote.addTrack(e.track);
      e.track.onended = () => remote.removeTrack(e.track);
      patch({ remote: new MediaStream(remote.getTracks()) });
    };
    conn.onicecandidate = (e) => e.candidate && signal("ice", { candidate: e.candidate.toJSON() });
    conn.onnegotiationneeded = async () => {
      try {
        makingOffer.current = true;
        await conn.setLocalDescription();
        signal("offer", { description: conn.localDescription?.toJSON() });
      } finally {
        makingOffer.current = false;
      }
    };
    conn.onconnectionstatechange = () => {
      if (conn.connectionState === "connected" && ref.current.phase !== "active") {
        patch({ phase: "active", startedAt: Date.now() });
        void haptic("match");
      }
      if (conn.connectionState === "failed") {
        send("end");
        finish("failed");
      }
    };
    pc.current = conn;
    return conn;
  }, [patch, signal, send, finish]);

  const addLocalTracks = useCallback(() => {
    const conn = pc.current;
    const local = ref.current.local;
    if (!conn || !local || tracksAdded.current) return;
    local.getTracks().forEach((t) => conn.addTrack(t, local));
    tracksAdded.current = true;
  }, []);

  const onSignal = useCallback(async (kind: string, payload: Record<string, unknown> | null) => {
    if (kind === "video-request") return patch({ videoAsk: "asked" });
    if (kind === "video-decline") return patch({ videoAsk: "none" });
    if (kind === "video-accept") {
      // they said yes: switch my camera on (it renegotiates by itself)
      patch({ videoAsk: "none", video: true });
      try {
        const cam = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "user" } });
        const local = ref.current.local ?? new MediaStream();
        cam.getVideoTracks().forEach((t) => {
          local.addTrack(t);
          pc.current?.addTrack(t, local);
        });
        patch({ local: new MediaStream(local.getTracks()), cameraOn: true });
      } catch {
        patch({ endedReason: "media" });
      }
      return;
    }
    const conn = pc.current;
    if (!conn || !payload) return;
    try {
      if (kind === "offer" || kind === "answer") {
        const description = payload.description as RTCSessionDescriptionInit;
        const collision = description.type === "offer" && (makingOffer.current || conn.signalingState !== "stable");
        ignoreOffer.current = !polite.current && collision;
        if (ignoreOffer.current) return;
        await conn.setRemoteDescription(description);
        if (description.type === "offer") {
          addLocalTracks(); // callee: our tracks ride on the caller's first offer
          await conn.setLocalDescription();
          signal("answer", { description: conn.localDescription?.toJSON() });
        }
        for (const c of pendingIce.current.splice(0)) await conn.addIceCandidate(c).catch(() => {});
      } else if (kind === "ice") {
        const candidate = payload.candidate as RTCIceCandidateInit;
        if (!conn.remoteDescription) pendingIce.current.push(candidate);
        else await conn.addIceCandidate(candidate).catch((e) => { if (!ignoreOffer.current) throw e; });
      }
    } catch {
      /* a bad signal is dropped; the connection state handler ends calls that truly fail */
    }
  }, [patch, signal, addLocalTracks]);

  const whoIs = useCallback(async (userId: string): Promise<Peer> => {
    const p = await profileOf(userId).catch(() => null);
    return { userId, name: p?.displayName ?? "Your match", photo: p?.photoUrls?.[0] ?? null };
  }, []);

  const onEvent = useCallback(async (e: CallEvent) => {
    const s = ref.current;
    switch (e.type) {
      case "consent":
        if (e.consent) consentListeners.current.get(e.matchId)?.forEach((l) => l(e.consent!));
        return;
      case "ring": {
        if (s.phase !== "idle" && s.phase !== "ended") return; // server already refuses double calls
        const peer = await whoIs(e.from!);
        setState({ ...idle, phase: "incoming", matchId: e.matchId, callId: e.callId, peer, video: !!e.video });
        void haptic("match");
        return;
      }
      case "ringing":
        if (s.matchId === e.matchId) patch({ callId: e.callId });
        return;
      case "accepted":
        if (s.callId !== e.callId) return;
        window.clearTimeout(ringTimer.current);
        patch({ phase: "connecting" });
        await makePeer(false); // caller = impolite
        addLocalTracks(); // → negotiationneeded → offer
        return;
      case "signal":
        if (s.callId === e.callId) await onSignal(e.kind!, e.payload);
        return;
      case "busy":
        if (s.matchId === e.matchId) finish("busy");
        return;
      case "declined":
      case "ended":
        if (s.callId === e.callId || (s.phase === "outgoing" && s.matchId === e.matchId)) finish(e.reason ?? e.type);
        return;
      case "error":
        if (s.matchId === e.matchId || s.phase === "outgoing") finish(e.reason);
        return;
    }
  }, [whoIs, patch, makePeer, addLocalTracks, onSignal, finish]);

  // one socket for the whole signed-in app, so calls ring on any screen
  useEffect(() => {
    const url = API_URL.replace(/^http/, "ws") + "/ws/chat";
    const client = new StompClient(url, () => getSession()?.token ?? null);
    stomp.current = client;
    const off = client.subscribe("/user/queue/calls", (b) => void onEvent(b as CallEvent));
    client.start();
    return () => {
      off();
      client.stop();
      stomp.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // closing the tab mid-call hangs up properly
  useEffect(() => {
    const bye = () => ref.current.callId && send("end");
    window.addEventListener("pagehide", bye);
    return () => window.removeEventListener("pagehide", bye);
  }, [send]);

  const start = useCallback(async (matchId: string, peer: Peer, video: boolean) => {
    if (ref.current.phase !== "idle" && ref.current.phase !== "ended") return;
    let local: MediaStream;
    try {
      local = await media(video);
    } catch {
      setState({ ...idle, phase: "ended", peer, endedReason: "media" });
      return;
    }
    setState({ ...idle, phase: "outgoing", matchId, peer, video, local, cameraOn: video });
    ref.current = { ...ref.current, matchId, callId: null };
    stomp.current?.send(`/app/calls/${matchId}/ring`, { video });
    ringTimer.current = window.setTimeout(() => {
      if (ref.current.phase === "outgoing") {
        send("end");
        finish("no_answer");
      }
    }, 45_000);
  }, [media, send, finish]);

  const accept = useCallback(async (withVideo: boolean) => {
    const s = ref.current;
    if (s.phase !== "incoming") return;
    let local: MediaStream;
    try {
      local = await media(withVideo && s.video); // camera stays off unless they tap Video
    } catch {
      send("decline");
      finish("media");
      return;
    }
    patch({ phase: "connecting", local, cameraOn: withVideo && s.video });
    ref.current = { ...ref.current, local };
    await makePeer(true); // callee = polite; tracks are added when the caller's offer arrives
    send("accept");
  }, [media, patch, makePeer, send, finish]);

  const decline = useCallback(() => {
    send("decline");
    teardown();
    setState(idle);
  }, [send, teardown]);

  const end = useCallback(() => {
    send("end");
    finish("ended_by_me");
  }, [send, finish]);

  const toggleMute = useCallback(() => {
    const local = ref.current.local;
    const muted = !ref.current.muted;
    local?.getAudioTracks().forEach((t) => (t.enabled = !muted));
    patch({ muted });
  }, [patch]);

  /** Camera off = the track is STOPPED (the camera light goes off), not just hidden. */
  const toggleCamera = useCallback(async () => {
    const s = ref.current;
    if (!s.video || !pc.current) return;
    const sender = pc.current.getTransceivers().find((t) => t.receiver.track.kind === "video")?.sender;
    if (s.cameraOn) {
      s.local?.getVideoTracks().forEach((t) => {
        t.stop();
        s.local?.removeTrack(t);
      });
      await sender?.replaceTrack(null);
      patch({ cameraOn: false, local: s.local ? new MediaStream(s.local.getTracks()) : null });
    } else {
      const cam = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "user" } }).catch(() => null);
      const track = cam?.getVideoTracks()[0];
      if (!track) return patch({ endedReason: "media" });
      const local = s.local ?? new MediaStream();
      local.addTrack(track);
      if (sender) await sender.replaceTrack(track);
      else pc.current.addTrack(track, local);
      patch({ cameraOn: true, local: new MediaStream(local.getTracks()) });
    }
  }, [patch]);

  const flip = useCallback(async () => {
    const s = ref.current;
    const current = s.local?.getVideoTracks()[0];
    if (!current || !pc.current) return;
    const facing = current.getSettings().facingMode === "environment" ? "user" : "environment";
    const cam = await navigator.mediaDevices.getUserMedia({ video: { facingMode: facing } }).catch(() => null);
    const track = cam?.getVideoTracks()[0];
    if (!track) return;
    await pc.current.getSenders().find((x) => x.track === current)?.replaceTrack(track);
    current.stop();
    s.local!.removeTrack(current);
    s.local!.addTrack(track);
    patch({ local: new MediaStream(s.local!.getTracks()) });
  }, [patch]);

  const askForVideo = useCallback(() => {
    signal("video-request");
    patch({ videoAsk: "asking" });
  }, [signal, patch]);

  const answerVideo = useCallback((yes: boolean) => {
    signal(yes ? "video-accept" : "video-decline");
    patch({ videoAsk: "none", video: yes || ref.current.video });
  }, [signal, patch]);

  const dismiss = useCallback(() => setState(idle), []);

  const onConsent = useCallback((matchId: string, l: (c: Consent) => void) => {
    const set = consentListeners.current.get(matchId) ?? new Set();
    set.add(l);
    consentListeners.current.set(matchId, set);
    return () => set.delete(l);
  }, []);

  const value = useMemo<Ctx>(() => ({ state, start, accept, decline, end, toggleMute, toggleCamera, flip, askForVideo, answerVideo, dismiss, onConsent }),
    [state, start, accept, decline, end, toggleMute, toggleCamera, flip, askForVideo, answerVideo, dismiss, onConsent]);
  return <CallCtx.Provider value={value}>{children}</CallCtx.Provider>;
}
