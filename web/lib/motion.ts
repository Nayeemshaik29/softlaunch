import type { Transition } from "framer-motion";

// Spring presets from the design skill (section 6). Use these, not ad-hoc numbers.
export const spring = {
  snappy: { type: "spring", stiffness: 520, damping: 34 } satisfies Transition,
  gentle: { type: "spring", stiffness: 300, damping: 30 } satisfies Transition,
  soft: { type: "spring", stiffness: 170, damping: 26 } satisfies Transition,
};

export const fade = { duration: 0.2, ease: "easeOut" } satisfies Transition;

/** Reveal-on-scroll: fade + 16px rise, children staggered 70ms. */
export const reveal = {
  container: { hidden: {}, show: { transition: { staggerChildren: 0.07 } } },
  item: {
    hidden: { opacity: 0, y: 16 },
    show: { opacity: 1, y: 0, transition: spring.soft },
  },
  itemReduced: {
    hidden: { opacity: 0 },
    show: { opacity: 1, transition: fade },
  },
};
