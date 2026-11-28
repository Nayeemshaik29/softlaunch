"use client";

import Link from "next/link";
import { motion, useReducedMotion } from "framer-motion";
import type { ComponentProps, ReactNode } from "react";
import { spring } from "@/lib/motion";

type Variant = "primary" | "secondary" | "ghost" | "danger";
type Size = "md" | "lg";

// Design v2: square-ish, all-caps label, solid offset "sticker" shadow on the main action.
const base =
  "inline-flex shrink-0 items-center justify-center gap-2 whitespace-nowrap rounded-md border-2 font-bold uppercase tracking-[0.08em] select-none transition-[color,background-color,box-shadow,translate] " +
  "disabled:opacity-45 disabled:pointer-events-none aria-disabled:opacity-45 aria-disabled:pointer-events-none";
const variants: Record<Variant, string> = {
  primary: "border-bg bg-accent text-on-accent shadow-sticker active:translate-x-[2px] active:translate-y-[2px] active:shadow-none",
  secondary: "border-text bg-transparent text-text hover:bg-surface-2",
  ghost: "border-transparent bg-transparent text-text hover:bg-surface-2",
  danger: "border-danger bg-transparent text-danger hover:bg-surface-2",
};
const sizes: Record<Size, string> = {
  md: "min-h-12 px-6 text-sm",
  lg: "min-h-14 px-8 text-base",
};

export function buttonClass(variant: Variant = "primary", size: Size = "md", extra = "") {
  return `${base} ${variants[variant]} ${sizes[size]} ${extra}`;
}

interface Common {
  variant?: Variant;
  size?: Size;
  className?: string;
  children: ReactNode;
}

export function Button({
  variant = "primary",
  size = "md",
  className = "",
  children,
  ...rest
}: Common & Omit<ComponentProps<typeof motion.button>, "children">) {
  const reduce = useReducedMotion();
  return (
    <motion.button
      whileTap={reduce ? undefined : { scale: 0.97 }}
      transition={spring.snappy}
      className={buttonClass(variant, size, className)}
      {...rest}
    >
      {children}
    </motion.button>
  );
}

export function ButtonLink({
  href,
  variant = "primary",
  size = "md",
  className = "",
  children,
}: Common & { href: string }) {
  return (
    <Link href={href} className={buttonClass(variant, size, className)}>
      {children}
    </Link>
  );
}
