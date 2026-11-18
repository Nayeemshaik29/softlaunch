"use client";

import { motion, useReducedMotion } from "framer-motion";
import { useEffect, useRef, useState } from "react";
import { api } from "@/lib/api";
import { spring } from "@/lib/motion";
import type { PhotoRef, UploadTicket } from "@/lib/types";

const MAX = 6;

/** Shrink to ≤ 1600px and re-encode as JPEG in the browser: faster uploads, and turns WebP/large PNGs into something the server accepts. */
async function toJpeg(file: File): Promise<Blob> {
  const bitmap = await createImageBitmap(file, { imageOrientation: "from-image" });
  const scale = Math.min(1, 1600 / Math.max(bitmap.width, bitmap.height));
  const canvas = document.createElement("canvas");
  canvas.width = Math.round(bitmap.width * scale);
  canvas.height = Math.round(bitmap.height * scale);
  canvas.getContext("2d")!.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
  bitmap.close();
  return new Promise((ok, fail) => canvas.toBlob((b) => (b ? ok(b) : fail(new Error("encode"))), "image/jpeg", 0.88));
}

/** upload-url → PUT straight to storage (signed link) → confirm (server checks + cleans the image). */
export async function uploadPhoto(file: File): Promise<PhotoRef> {
  const blob = await toJpeg(file);
  const ticket = await api<UploadTicket>("/api/users/me/photos/upload-url", { method: "POST", body: { contentType: "image/jpeg", sizeBytes: blob.size } });
  const put = await fetch(ticket.uploadUrl, { method: "PUT", headers: ticket.headers, body: blob });
  if (!put.ok) throw new Error("Upload failed. Try again.");
  const view = await api<{ photoId: string; url: string }>(`/api/users/me/photos/${ticket.photoId}/confirm`, { method: "POST" });
  return { photoId: view.photoId, url: view.url };
}

interface Props {
  photos: PhotoRef[];
  setPhotos: (p: PhotoRef[]) => void;
}

export function PhotoGrid({ photos, setPhotos }: Props) {
  const reduce = useReducedMotion();
  const [busy, setBusy] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [draggingId, setDraggingId] = useState<string | null>(null);
  const slots = useRef<(HTMLDivElement | null)[]>([]);
  const input = useRef<HTMLInputElement>(null);
  const order = useRef(photos);
  useEffect(() => { order.current = photos; }, [photos]);

  const saveOrder = async (list: PhotoRef[], before: PhotoRef[]) => {
    try {
      await api("/api/users/me/photos/order", { method: "PUT", body: { photoIds: list.map((p) => p.photoId) } });
    } catch {
      setPhotos(before);
      setError("Couldn't save the new order. Try again.");
    }
  };

  const move = (from: number, to: number) => {
    if (to < 0 || to >= order.current.length || from === to) return order.current;
    const next = [...order.current];
    const [p] = next.splice(from, 1);
    next.splice(to, 0, p);
    order.current = next;
    setPhotos(next);
    return next;
  };

  const add = async (files: FileList | null) => {
    if (!files?.length) return;
    setError(null);
    const room = MAX - photos.length;
    const list = [...files].slice(0, room);
    setBusy((b) => b + list.length);
    for (const f of list) {
      try {
        const ref = await uploadPhoto(f);
        order.current = [...order.current, ref];
        setPhotos(order.current);
      } catch (e) {
        setError((e as Error).message || "That photo didn't upload. JPEG or PNG, up to 10 MB.");
      } finally {
        setBusy((b) => b - 1);
      }
    }
  };

  const remove = async (p: PhotoRef) => {
    const before = order.current;
    setPhotos(before.filter((x) => x.photoId !== p.photoId));
    try {
      await api(`/api/users/me/photos/${p.photoId}`, { method: "DELETE" });
    } catch {
      setPhotos(before);
      setError("Couldn't remove that photo. Try again.");
    }
  };

  /** which slot is the pointer over? */
  const slotAt = (x: number, y: number) => {
    let best = -1, bestD = Infinity;
    slots.current.forEach((el, i) => {
      if (!el || i >= order.current.length) return;
      const r = el.getBoundingClientRect();
      const d = Math.hypot(r.left + r.width / 2 - x, r.top + r.height / 2 - y);
      if (d < bestD) { bestD = d; best = i; }
    });
    return best;
  };

  return (
    <div>
      <ul className="grid grid-cols-3 gap-2" aria-label="Your photos. Drag to reorder; the first is your main photo.">
        {Array.from({ length: MAX }, (_, i) => {
          const p = photos[i];
          return (
            <li key={p?.photoId ?? `empty-${i}`} ref={(el) => { slots.current[i] = el as HTMLDivElement | null; }} className="relative aspect-[3/4]">
              {p ? (
                <motion.div
                  layout={!reduce}
                  transition={spring.gentle}
                  drag
                  dragSnapToOrigin
                  dragElastic={0.2}
                  whileDrag={{ scale: 1.06, zIndex: 10, boxShadow: "var(--shadow-card)" }}
                  onDragStart={() => setDraggingId(p.photoId)}
                  onDrag={(_, info) => {
                    const from = order.current.findIndex((x) => x.photoId === p.photoId);
                    const to = slotAt(info.point.x - window.scrollX, info.point.y - window.scrollY);
                    if (to >= 0 && to !== from) move(from, to);
                  }}
                  onDragEnd={() => {
                    setDraggingId(null);
                    void saveOrder(order.current, photos);
                  }}
                  tabIndex={0}
                  aria-describedby="photo-keys"
                  onKeyDown={(e) => {
                    // keyboard alternative to dragging
                    if (e.key === "ArrowLeft" || e.key === "ArrowUp") { e.preventDefault(); void saveOrder(move(i, i - 1), photos); }
                    if (e.key === "ArrowRight" || e.key === "ArrowDown") { e.preventDefault(); void saveOrder(move(i, i + 1), photos); }
                    if (e.key === "Delete" || e.key === "Backspace") { e.preventDefault(); void remove(p); }
                  }}
                  className={`group absolute inset-0 cursor-grab touch-none overflow-hidden rounded-md bg-surface-2 active:cursor-grabbing ${draggingId === p.photoId ? "z-10" : ""}`}
                >
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img src={p.url} alt={`Photo ${i + 1}${i === 0 ? " (main)" : ""}`} className="h-full w-full object-cover" draggable={false} />
                  {i === 0 && <span className="absolute left-2 top-2 rounded-full bg-black/55 px-2 py-0.5 text-xs text-white">Main</span>}
                  <IconBtn label={`Remove photo ${i + 1}`} onClick={() => void remove(p)}>
                    <path d="M4 4l8 8M12 4l-8 8" />
                  </IconBtn>
                </motion.div>
              ) : i === photos.length ? (
                <button
                  type="button"
                  onClick={() => input.current?.click()}
                  className="absolute inset-0 grid place-items-center rounded-md border-2 border-dashed border-line text-muted hover:border-accent hover:text-text"
                  aria-label="Add a photo"
                >
                  {busy > 0 ? <span className="skeleton absolute inset-0 rounded-md" aria-label="Uploading" /> : (
                    <svg aria-hidden width="28" height="28" viewBox="0 0 28 28"><path d="M14 7v14M7 14h14" stroke="currentColor" strokeWidth="2" strokeLinecap="round" /></svg>
                  )}
                </button>
              ) : (
                <div aria-hidden className="absolute inset-0 rounded-md bg-surface-2/60" />
              )}
            </li>
          );
        })}
      </ul>
      <input ref={input} type="file" accept="image/jpeg,image/png,image/webp" multiple className="sr-only" tabIndex={-1} onChange={(e) => { void add(e.target.files); e.target.value = ""; }} />
      {error && <p role="alert" className="mt-2 text-sm text-danger">{error}</p>}
      <p id="photo-keys" className="mt-2 text-xs text-muted">Up to 6 photos. Drag to reorder (or focus a photo and use the arrow keys): the first one is what people see first.</p>
    </div>
  );
}

function IconBtn({ label, onClick, disabled, children }: { label: string; onClick: () => void; disabled?: boolean; children: React.ReactNode }) {
  return (
    <button type="button" aria-label={label} onClick={onClick} disabled={disabled} onPointerDown={(e) => e.stopPropagation()}
      className="absolute right-0 top-0 grid h-12 w-12 place-items-center rounded-bl-md bg-black/45 text-white hover:bg-black/65 disabled:opacity-30">
      <svg aria-hidden width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">{children}</svg>
    </button>
  );
}
