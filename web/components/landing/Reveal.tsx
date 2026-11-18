"use client";

import { motion, useReducedMotion } from "framer-motion";
import type { ReactNode } from "react";
import { reveal } from "@/lib/motion";

/** Staggered scroll reveal, once. Children should be <RevealItem>. */
export function Reveal({ children, className = "", as = "div" }: { children: ReactNode; className?: string; as?: "div" | "ul" | "ol" }) {
  const Tag = as === "ul" ? motion.ul : as === "ol" ? motion.ol : motion.div;
  return (
    <Tag className={className} variants={reveal.container} initial="hidden" whileInView="show" viewport={{ once: true, margin: "-10% 0px" }}>
      {children}
    </Tag>
  );
}

export function RevealItem({ children, className = "", as = "div" }: { children: ReactNode; className?: string; as?: "div" | "li" }) {
  const reduce = useReducedMotion();
  const Tag = as === "li" ? motion.li : motion.div;
  return (
    <Tag className={className} variants={reduce ? reveal.itemReduced : reveal.item}>
      {children}
    </Tag>
  );
}
