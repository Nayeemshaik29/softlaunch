# SoftLaunch web app

Next.js 16 (App Router) + Tailwind 4 + Framer Motion + React Three Fiber. Talks to the gateway at `NEXT_PUBLIC_API_URL`.
Design rules: [`.claude/skills/frontend-design/SKILL.md`](../.claude/skills/frontend-design/SKILL.md).

```bash
cp .env.example .env.local     # API + photo storage addresses
npm install
npm run dev -- --port 3030     # 3000 is Grafana; the gateway's CORS allows 3030 (WEB_ORIGINS in ../.env)
```

Demo data (local only): `python3 scripts/seed_demo.py` creates a demo login + 8 people nearby; the login is written to
`web/.demo-login` (git-ignored). Three of them already like the demo user, so liking them back shows the match moment.

| Screen | Path |
|---|---|
| Landing (3D hero, loads on first interaction) | `/` |
| Signup + 5-step onboarding | `/signup` |
| Swipe deck + energy meter / rest state | `/swipe` |
| Matches, chat | `/matches`, `/matches/[id]` |
| Activity (swipe to dismiss) | `/notifications` |

Checks: `npm run lint`, `npm run typecheck`, `npm run build`.
