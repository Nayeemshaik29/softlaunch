# Account deletion

`DELETE /api/users/me` with body `{"password": "..."}` → **204**. The password is asked again, so someone holding an unlocked phone can't delete the account. Wrong guesses count toward the same lockout as login.

## What happens

```
user-service, before anything is deleted:
  1. check the password (lockout after 5 failures)
  2. Redis: revoked:user:<id>       → the gateway and chat WebSocket refuse every token of this user from now on
  3. ONE database transaction:      profile + photo uploads + account deleted, user.deleted written to the outbox
outbox publisher → Kafka user.deleted (retries until Kafka accepts it)
  → every service removes its own data (safe to receive twice; failures go to user.deleted.DLT)
```

If step 2 fails (Redis down), nothing is deleted and the app gets **503 "try again"**. A half-deleted account is never possible.

## Deleted vs. kept

| Service | Deleted | Kept, and why |
|---|---|---|
| user-service | account, profile, photo records, **photos in storage** | – |
| match-service | all matches (each sends `match.removed`, which cleans chats, notifications and cached openers), swipes, blocks both ways | – |
| chat-service | conversations (via `match.removed`), online/last-seen status | – |
| notification-service | all their notifications; the other person's notifications about the match | – |
| discovery-service | location (off the map), block lists, cached feed | other people's cached feeds may show the card for **up to 2 min**; a swipe on it is refused |
| verification-service | **all** KYC data: name, date of birth, identity hash. The same identity can verify a new account later. | – |
| ai-service | profile embedding, cached openers | – |
| safety-service | blocks, review status, records of screenshots they took | **Reports about them:** kept as evidence for moderators, deleted after **365 days**. **Reports they filed:** kept (the reported person may be dangerous) with their id replaced by an anonymous pseudonym. **Screenshots taken of them:** kept 90 days for the other person's pattern check, pseudonymised. |
| Kafka | – | events contain ids only (no names or text, except `message.screening`, which expires after 24 h) |
| Backups | – | expire with the backup rotation (say how long in the privacy policy) |

## Play Store "Data deletion" answers
- **Users can request deletion:** yes, in the app (Settings → Delete account), plus a web form for people who uninstalled. The web form still needs to be built: a page on the website that verifies the email and calls the same flow.
- **Deleted:** all account, profile, photo, match, chat, location and verification data.
- **Kept:** safety reports (up to 365 days, for user safety and legal requests), with the deleted user's identity removed from reports they filed.

## Monitoring
- `softlaunch_accounts_deleted_total{result="deleted|wrong_password"}`
- A message in `user.deleted.DLT` means one service could not clean up. Replay it after fixing the cause (cleanup is idempotent).
