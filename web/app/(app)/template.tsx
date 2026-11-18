"use client";

import { motion, useReducedMotion } from "framer-motion";
import { spring } from "@/lib/motion";

/** Page transition between app screens: fade + small rise (fade only under reduced motion). */
export default function Template({ children }: { children: React.ReactNode }) {
  const reduce = useReducedMotion();
  return (
    <motion.div
      initial={reduce ? { opacity: 0 } : { opacity: 0, y: 12 }}
      animate={reduce ? { opacity: 1 } : { opacity: 1, y: 0 }}
      transition={reduce ? { duration: 0.15 } : spring.gentle}
    >
      {children}
    </motion.div>
  );
}
