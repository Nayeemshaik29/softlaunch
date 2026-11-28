import { useId } from "react";

/**
 * The SoftLaunch peach.
 *  - "full" (with the little bow): INSIDE the app only (header, splash, match moment).
 *  - "clean": everywhere public: app icon, landing page, store screenshots, emails. Apple 2.3.8 / Play listing rules.
 */
export function Peach({ variant = "clean", size = 40, className = "" }: { variant?: "full" | "clean"; size?: number; className?: string }) {
  const id = useId().replace(/:/g, "");
  return (
    <svg aria-hidden width={size} height={size} viewBox="0 0 200 200" className={className}>
      <defs>
        <linearGradient id={`${id}g`} x1="0.3" y1="0" x2="0.6" y2="1">
          <stop offset="0" stopColor="#F8E6D8" />
          <stop offset="0.55" stopColor="#F3C9A6" />
          <stop offset="1" stopColor="#EE9A4E" />
        </linearGradient>
      </defs>
      <path d="M100 62 C72 34 22 50 26 106 C30 150 74 174 100 184 C126 174 170 150 174 106 C178 50 128 34 100 62 Z" fill="#0B0B0C" stroke="#0B0B0C" strokeWidth="22" strokeLinejoin="round" />
      <path d="M100 62 C72 34 22 50 26 106 C30 150 74 174 100 184 C126 174 170 150 174 106 C178 50 128 34 100 62 Z" fill={`url(#${id}g)`} />
      <path d="M100 64 C86 96 90 140 110 172" fill="none" stroke="#0B0B0C" strokeWidth="7" strokeLinecap="round" />
      <path d="M44 86 C50 66 66 58 82 58" fill="none" stroke="#fff" strokeOpacity=".75" strokeWidth="6" strokeLinecap="round" />
      {variant === "full" && (
        <>
          <path d="M33 112 C60 100 84 106 100 121 C116 106 140 100 167 112" fill="none" stroke="#0B0B0C" strokeWidth="7" strokeLinecap="round" />
          <path d="M38 122 C62 112 84 117 98 130 M102 130 C116 117 138 112 162 122" fill="none" stroke="#0B0B0C" strokeWidth="3.5" strokeLinecap="round" strokeDasharray="0.1 7" />
          <path d="M91 122 L110 122 L101 150 Z" fill="#0B0B0C" />
          <ellipse cx="93" cy="121" rx="7" ry="5" fill="#F28C28" stroke="#0B0B0C" strokeWidth="2.5" />
          <ellipse cx="108" cy="121" rx="7" ry="5" fill="#F28C28" stroke="#0B0B0C" strokeWidth="2.5" />
          <circle cx="100.5" cy="121" r="3.2" fill="#fff" stroke="#0B0B0C" strokeWidth="2" />
        </>
      )}
      <path d="M97 56 C80 30 56 32 47 44 C60 58 82 62 97 56 Z" fill="#F3D9C6" stroke="#0B0B0C" strokeWidth="6" strokeLinejoin="round" />
      <path d="M101 60 C104 44 112 34 126 26" fill="none" stroke="#0B0B0C" strokeWidth="10" strokeLinecap="round" />
      <path d="M102 58 C105 45 112 37 123 30" fill="none" stroke="#F3D9C6" strokeWidth="3.5" strokeLinecap="round" />
    </svg>
  );
}
