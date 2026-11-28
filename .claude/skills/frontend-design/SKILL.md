---
name: frontend-design
description: SoftLaunch web design system (web/). Use for ANY UI work in the Next.js app — new screens, components, motion, 3D scenes, colors, spacing, typography, copy tone. Defines tokens, patterns, motion rules and what to avoid.
---

# SoftLaunch frontend design

SoftLaunch is a **calm** dating app: "Dating that doesn't waste your evening." Every screen should feel unhurried,
warm and confident. When unsure, choose less: fewer colors, fewer animations, more space.

Tokens live in `web/app/globals.css` (CSS variables, mapped into Tailwind with `@theme`). **Never hard-code a hex,
px size or duration in a component** — use the tokens below. If a token is missing, add it here and in globals.css first.

---

## 1. Spacing: 8px grid

| Token | px | Use |
|---|---|---|
| `--space-0-5` | 4 | only hairline nudges (icon ↔ label, stamp borders) |
| `--space-1` | 8 | tight gaps inside a component |
| `--space-2` | 16 | default gap, input padding |
| `--space-3` | 24 | card padding, gaps between related blocks |
| `--space-4` | 32 | between groups |
| `--space-5` | 40 | |
| `--space-6` | 48 | section padding on mobile |
| `--space-8` | 64 | |
| `--space-10` | 80 | |
| `--space-12` | 96 | section padding on desktop |
| `--space-16` | 128 | hero breathing room |

- Tailwind spacing is 4px-based: use **even** steps only (`p-2`=8, `p-4`=16, `p-6`=24, `gap-8`=32 …). Odd steps (`p-3`, `mt-5`) are not allowed except the 4px nudge `*-1`.
- Page side gutter: 16px on phones, 24px ≥ 640px, max content width 1120px. No horizontal page scroll, ever.
- Tap targets ≥ 48×48px (onboarding choices and the swipe buttons: 56–64px).

## 2. Type scale (design v2, Pure-inspired)

Three families, loaded with `next/font` (self-hosted, `display: swap`):
- **Display:** Bricolage Grotesque, **800, condensed (wdth 75), ALL CAPS**, line-height ~0.88. Headlines, names, big numbers.
  The `.font-display` class sets weight/width/caps itself: never add `font-normal`/`font-medium` to it.
- **UI:** Instrument Sans — everything you read. Buttons and labels: bold, uppercase, `tracking-[0.08em]`.
- **Hand:** Caveat 600 (`<Note>` in `components/ui/Doodles`) — short handwritten asides only ("no endless swiping, promise"). Max one per screen, never for information someone needs.

| Token | size / line-height | Family | Use |
|---|---|---|---|
| `text-xs` | 12 / 16 | UI | meta, eyebrows (`font-bold tracking-[0.14em] uppercase`) |
| `text-sm` | 14 / 20 | UI | secondary text, button labels |
| `text-base` | 16 / 24 | UI | body (minimum for anything you must read) |
| `text-lg` | 20 / 28 | UI | lead paragraphs |
| `text-3xl`… | 40+ | Display | screen titles: go BIG (`text-[4rem]` titles, `clamp(4rem,11vw,10.5rem)` hero) |

- Numbers that tick (countdowns, %): `font-variant-numeric: tabular-nums`.

## 3. Color tokens (palette v2: dark only)

Jet Black, Warm Peach Beige, Rich Amber Orange. There is no light theme: the brand is the night.

| Token | Value | Use |
|---|---|---|
| `--bg` | #0B0B0C (Jet Black) | page |
| `--surface` / `--surface-2` | #171513 / #231F1C | cards, inputs |
| `--line` | #342E29 | dividers |
| `--text` | #F3D9C6 (Warm Peach Beige) | primary text (15:1 on bg) |
| `--muted` | #B9A595 | secondary text (8:1) |
| `--accent` | #F28C28 (Rich Amber Orange) | main action, active tab, highlights (8:1 on bg) |
| `--accent-2` | #F3D9C6 | sticker shadows, peach stickers |
| `--on-accent` | #0B0B0C | text on amber/peach (8:1 / 15:1) |
| `--danger` | #FF8A7A | errors, destructive |
| `--focus` | #F6B26B | focus ring |

- Text on amber or peach is always Jet Black. All pairs are WCAG AA+.
- Status is never color-only: stamps have words, errors have text + icon.

## 4. Shape, depth, patterns

- Radii: `--radius-sm` 4, `--radius-md` 6 (buttons, inputs), `--radius-lg` 8 (cards). Pills (`rounded-full`) only for chips, avatars, switches.
- Depth = **solid offset "sticker" shadows**, not blur: `shadow-sticker` (4px peach), `shadow-sticker-accent` (4px amber), cards `--shadow-card` (6px amber).
- Borders are bold: `border-2`, peach (`border-text`) on dark.
- **Patterns** (CSS only, no images): `bg-burst` ("Afterglow" amber/peach sunburst: hero, log-in header, match, Plus) and
  `bg-ripple` (amber rings: closing CTA, card theme). At most one pattern block per screen; text on it is Jet Black.
- `<Torn>` (`components/ui/Torn`): torn-paper edge at the bottom of a pattern block, filled with the colour below.
- Doodles (`Star`, `Arrow`): decorative, `aria-hidden`, slightly rotated.

## 5. Component patterns

**Card** (`components/ui/Card`): `bg-surface rounded-lg p-6 shadow-card`. Profile cards: full-bleed photo, 24px radius,
bottom scrim (`linear-gradient(to top, rgb(0 0 0 / .72), transparent 55%)`) so white text is always AA, name in Display `text-xl`.

**Button** (`components/ui/Button`), min-height 48px, `rounded-md`, `text-base font-medium`, 16px/24px padding:
- `primary`: amber fill, Jet Black label, peach `shadow-sticker`; pressing slides it into its shadow. One per screen.
- `secondary`: transparent with a 2px peach border.
- **Google**: always Google's own rendered button (`components/ui/GoogleButton`), never a homemade one.
- `ghost`: transparent, `--text`, underline-on-hover only for inline links.
- `danger`: `--danger` text on `--surface-2` (fill only in confirm dialogs).
- Pressed: scale 0.97 with the `snappy` spring. Disabled: 45% opacity, no pointer, `aria-disabled`.
- Icon-only buttons need `aria-label` and a 48px box.

**Input** (`components/ui/Field`): label above (always visible, never placeholder-as-label), 56px tall, `--surface-2` fill,
1px `--line` border, `rounded-md`. Error: `--danger` border + message below with `role="alert"` and `aria-describedby`.
Helper text in `--muted`. Validate on blur and on submit, not on every keystroke (except the 18+ birthday check, which is friendly and live).

**Focus**: every interactive element shows `outline: 2px solid var(--focus); outline-offset: 2px` on `:focus-visible`. Never `outline: none` without a replacement.

**Skeletons**: `--surface-2` blocks with the same radius as the content, a slow shimmer (1.6s, linear) — static under reduced motion.

**Empty states**: small 3D or illustrated object (≤ 160px), one sentence in Display `text-xl`, one helpful action.
Calm, never guilt ("No one new nearby yet — we'll ping you." not "Oops! Nothing here!").

## 6. Motion

Library: Framer Motion (`motion/*` presets in `web/lib/motion.ts`). 3D: React Three Fiber.

- **Springs for anything that moves**; tweens only for opacity/color.
  - `snappy` `{ type: "spring", stiffness: 520, damping: 34 }` — presses, toggles, stamps
  - `gentle` `{ type: "spring", stiffness: 300, damping: 30 }` — cards, sheets, step transitions
  - `soft` `{ type: "spring", stiffness: 170, damping: 26 }` — big layout moves, reveals
  - drag release: physics from the gesture velocity (`dragTransition` / `animate` with `velocity`)
- **Durations 150–400ms** for UI. Exceptions: ambient 3D loops and the match moment (still skippable after 1.2s).
- Stagger reveals by 60–80ms, max 6 items; reveal once (`viewport={{ once: true, margin: "-10% 0px" }}`).
- Enter: fade + 16px rise. Exit: faster than enter (≈ 60%).
- Shared layout (`layoutId`) for elements that continue between steps (progress arc, preview card).
- **`prefers-reduced-motion`**: use `useReducedMotion()` everywhere. Replace movement with opacity fades (≤ 200ms),
  no parallax, no shake, no particle bursts, 3D scenes → static image. Drag still works (user-driven), but release snaps without bounce.
- Never animate layout properties (width/height/top) — use transforms. Keep 60fps on a mid-range phone.
- 3D scenes: lazy-load after first paint, `dpr={[1, 1.5]}`, pause when off-screen or tab hidden, static fallback when WebGL is missing,
  `saveData` is on, `hardwareConcurrency ≤ 4` or `deviceMemory ≤ 4`.

## 7. Voice

Short, warm, specific. Second person. No exclamation-mark spam, no pressure ("Only 2 left!"), no FOMO.
Limits are framed as rest: "You're out of energy for today. Back at midnight."

## 8. Avoid: generic dating-app and AI aesthetics

Do **not**:
- Flame/fire icons, hot-pink-on-white, red heart confetti, "🔥 HOT" badges, slot-machine "It's a match!" banners.
- Purple-to-blue "AI" gradients on everything, glassmorphism on every surface, neon glow borders, aurora blobs as background filler.
- Default Tailwind look: `rounded-xl shadow-lg` cards in a 3-column grid with emoji icons; `bg-gradient-to-r from-pink-500 to-purple-500` buttons.
- Inter/Roboto-only typography, centered everything, giant stock-photo smiling couples, fake testimonials or fake user counts.
- Sparkle ✨ / robot / "magic" wording for AI features — describe what it does ("suggests a first message").
- Lorem ipsum or placeholder photos of real-looking people; use our generated portrait textures or real user photos.
- Dark patterns: hidden unmatch/delete, guilt copy, fake scarcity, paywall teasers.

Do:
- Asymmetric, editorial layouts with generous space; one strong headline per screen.
- Warm near-black and paper-white surfaces; the gradient as a rare signal.
- Real product moments (a card, a match) instead of decoration.

## 9. "After dark" mood (inside the app only)

A night variant, in the spirit of apps like Pure: moody, warm, flirty. **Suggestive, never explicit.**
Set with `data-mood="after-dark"` on `<html>` by `MoodProvider` (`web/lib/mood.tsx`). It turns on by default when the user's
intents include HOOKUP, FRIENDS_WITH_BENEFITS, ONE_NIGHT_STAND or CASUAL_DATING; the header toggle switches it, and the choice is remembered.

Same palette, lit lower and warmer: slightly browner surfaces, a stronger amber glow on cards and a faint amber vignette.

Rules:
- **Never on the landing page, the web pages for privacy/deletion, or Play Store screenshots**: Google Play rejects sexually suggestive store listings. The public brand stays calm.
- Dark only (ignores the system light setting).
- Copy: use `t(calm, afterDark)`. Flirty, confident, consent-forward ("Make the first move"), never crude, never body-focused, never pressure.
- Imagery: mood comes from color, light and blur, not from bodies. No nudity, no lingerie stock, no suggestive emoji (🍑🍆💦).
- 3D: read colors from `palettes[mood]` (WebGL can't read CSS variables).
- Same spacing, type, motion and accessibility rules as the calm mood.
- **Veiled photos** (mask motif): blur is decided by the SERVER (the browser only gets a 120 px blurred copy). In the UI:
  `scale-110 blur-xl` on the image, the mask icon and "Photos unveil when you match". The match moment is the unveil.
- **Private Mode / Settings switches**: `role="switch"`, label + description, optimistic with rollback.
