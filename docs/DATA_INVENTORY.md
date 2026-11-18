# Personal data inventory

Every piece of personal data SoftLaunch stores, where, why, and for how long. Built from the code (Nov 2026).
**The privacy policy (`/privacy`) and the Play "Data safety" form ([DATA_SAFETY.md](DATA_SAFETY.md)) are written from this file.**

> Rule: a pull request that adds, removes or keeps a field longer must update this file, the policy page
> (`api-gateway/src/main/resources/static/privacy/index.html`) and its version/date.

## What we store

| Data | Service / store | Why | Kept for |
|---|---|---|---|
| Email, display name | user-service · user-db | account, login, deletion emails | until account deletion |
| Password | user-service · user-db | login. Stored only as a **BCrypt hash** | until account deletion |
| Date of birth | user-service · user-db | 18+ check, age shown to others, age filters | until account deletion |
| Profile: bio, pronouns, height, job, company, education, languages, habits, interests, prompts, hangout places, relationship intent | user-service · user-db | shown to other members, matching | until account deletion |
| Gender, "interested in" | user-service · user-db | matching. **Sensitive**: can reveal sexual orientation | until account deletion |
| City + **exact latitude/longitude** | user-db (profile), Redis `geo:users` | nearby feed. Others only see a distance, rounded up | until account deletion or location change |
| Photos | user-service · S3 bucket (private) | shown to members via expiring links. Re-encoded on upload: **camera metadata/GPS removed** | until removed or account deletion. Unconfirmed uploads: 1 day |
| Blurred photo copies (120 px) | user-service · S3, next to each photo | Veiled photos: what others see before a match | same as the photo |
| Privacy settings (Private Mode, Veiled photos) | user-db (profile) + Redis sets `privacy:private_mode`, `privacy:veiled` | hide you / blur your photos everywhere at once | until changed or account deletion |
| Swipes, matches | match-service · match-db | matching, "who liked whom" | until unmatch / account deletion |
| Chat messages | chat-service · Mongo | messaging | **24 hours** (auto-delete), or earlier on unmatch / deletion |
| Message text for scam screening | Kafka `message.screening` → ai-service | scam/fraud detection by **our own AI on our servers** | 24 hours in Kafka. The verdict stores **no text** |
| Online status, typing | chat-service · Redis | presence shown to matches | 60 seconds (expires automatically) |
| Last seen time | chat-service · Redis | "last seen" shown to matches | overwritten on each visit; removed on account deletion |
| Screenshot / screen-recording events in a chat | chat-service → safety-service · Mongo | warn the other person; detect patterns | 90 days |
| Notifications | notification-service · notification-db | in-app notifications | until account deletion |
| ID verification (DigiLocker): verified name, DOB, a keyed hash of the identity (no ID number), result | verification-service · verification-db | "Verified" badge, 18+, one identity per account | until account deletion |
| Reports **you file**: reason, details, a copy of recent chat messages as evidence | safety-service · Mongo | safety review by moderators | 365 days. After your deletion: kept, with your identity replaced by a pseudonym |
| Reports **about you** | safety-service · Mongo | safety review | 365 days, also after your deletion |
| Blocks | safety-service, match-service, Redis | hide people from each other | until unblocked / account deletion |
| Profile "vibe" embedding (numbers computed from interests, bio, prompts, places) | ai-service · ai-db | similar-vibe ranking | until profile change / account deletion |
| AI chat-opener suggestions | ai-service · Redis | suggestions in a new chat | 30 minutes |
| IP address | api-gateway · Redis rate-limit keys | stop abuse (login guessing, spam) | minutes |
| Session (refresh token) | user-service · user-db `refresh_tokens` (SHA-256 only) + an HttpOnly cookie in the browser | stay logged in | 30 days, or until logout / theft detection / account deletion; rows purged 7 days after expiry |
| Login failure counter | user-service · Redis (SHA-256 of email) | account lockout | 15 minutes |
| Deletion-link token | user-service · Redis (SHA-256 only) | web account deletion | 30 minutes, single use |
| Service logs | Loki | security, debugging. **ids only**, never emails, passwords, tokens, chat or profile text | 30 days (`observability/loki/loki.yaml`) |
| Metrics, traces | Prometheus, Zipkin | performance. Counts and timings, no personal data | Prometheus 15 days; Zipkin in memory |
| Events between services | Kafka | keep services in sync. ids only (except `message.screening`) | 7 days (Kafka default) |
| Backups | [FILL IN: backup store] | disaster recovery | [FILL IN: e.g. 30 days rotation] |

## What we do NOT collect
- Aadhaar / ID numbers (only a keyed hash from DigiLocker)
- Contacts, call logs, SMS, browsing history
- Advertising IDs. No ads, no third-party analytics or tracking SDKs (**keep it that way in the Flutter app**, or update the policy)
- Payment data (no payments yet)

## Who processes data for us (processors, not "sharing")
| Who | What | Status |
|---|---|---|
| Hosting provider | all of the above (servers, databases) | [FILL IN: e.g. AWS, Mumbai ap-south-1] |
| Object storage | photos | [FILL IN: e.g. Amazon S3, Mumbai] |
| Email provider | email address + deletion emails | [FILL IN: e.g. Amazon SES / Brevo] |
| DigiLocker (Govt. of India) | identity check, only when **you** start verification | integrated |
| AI | runs on our own servers (Ollama). Nothing sent to outside AI companies | integrated |

## Ideas to collect less (not done yet)
- Store location rounded to ~1 km instead of exact coordinates (the feed only needs distance).
- Delete notifications older than 90 days.
