"use client";

import { useId, type InputHTMLAttributes, type ReactNode } from "react";

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string | null;
  hint?: ReactNode;
  big?: boolean;
}

/** Label above, 56px input, error below with role=alert (design skill §5). */
export function Field({ label, error, hint, big, className = "", ...input }: Props) {
  const id = useId();
  const describedBy = [error ? `${id}-err` : null, hint ? `${id}-hint` : null].filter(Boolean).join(" ") || undefined;
  return (
    <div className={className}>
      <label htmlFor={id} className="mb-2 block text-xs font-bold tracking-[0.08em] text-text uppercase">
        {label}
      </label>
      <input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={`w-full rounded-md border-2 bg-bg px-4 text-text placeholder:text-muted outline-none transition-colors
          focus-visible:border-focus ${big ? "min-h-16 text-xl" : "min-h-14 text-base"}
          ${error ? "border-danger" : "border-text/70"}`}
        {...input}
      />
      {hint && !error && (
        <p id={`${id}-hint`} className="mt-2 text-sm text-muted">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${id}-err`} role="alert" className="mt-2 flex items-start gap-2 text-sm text-danger">
          <svg aria-hidden width="16" height="16" viewBox="0 0 16 16" className="mt-0.5 shrink-0">
            <circle cx="8" cy="8" r="7" fill="none" stroke="currentColor" strokeWidth="1.5" />
            <path d="M8 4.5v4M8 11v.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
          </svg>
          {error}
        </p>
      )}
    </div>
  );
}
