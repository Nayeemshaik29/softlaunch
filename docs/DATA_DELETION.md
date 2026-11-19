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

## Without the app: the web page

`https://<your api domain>/delete-account`, served by the gateway. Proof of ownership is the email inbox, not the password (people who uninstalled often forgot it).

```
1. enter email → POST /api/users/auth/deletion-requests → always 202 "if an account uses this email…"
     background: account exists? → max 1 email per 10 min → random token, Redis stores only its SHA-256 (30 min)
     email: https://…/delete-account#token=…
2. open the link → the page reads the token, removes it from the address bar, shows what is deleted/kept
3. tick "I understand" + press Delete → POST …/confirm {token} → GETDEL (works once) → same steps 2-4 as the app
```

| Protection | Why |
|---|---|
| Same answer, same speed for unknown emails (work runs in the background) | the page can't be used to find out who has a dating account |
| Token only hashed in Redis | a Redis leak can't be turned into deletion links |
| Token after `#` in the link | browsers never send it to servers: not in access logs, proxies or Referer headers |
| Delete needs a button press (POST), the link alone does nothing | email scanners open every link automatically |
| One email per account per 10 min, own gateway rate limit (`deletion:<ip>`) | nobody can flood a person's inbox; doesn't use up login attempts |
| Content-Security-Policy `default-src 'none'`, own files only | injected scripts can't run on the page; it can't be framed |

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
| user-service (Plus) | membership, Plus status | **payment records** (plan, amount, Razorpay ids, dates): kept for tax and refunds, no card/UPI data |
| Backups | – | expire with the backup rotation (say how long in the privacy policy) |

## Play Store "Data deletion" answers
- **Users can request deletion:** yes, in the app (Settings → Delete account), and on the web at `/delete-account` for people who uninstalled (this is the URL the Play Console asks for).
- **Deleted:** all account, profile, photo, match, chat, location and verification data.
- **Kept:** safety reports (up to 365 days, for user safety and legal requests), with the deleted user's identity removed from reports they filed.

## Monitoring
- `softlaunch_accounts_deleted_total{result="deleted|wrong_password", via="app|web"}`
- `softlaunch_deletion_links_total{result="sent|no_account|cooldown|failed|dropped"}`: many `failed` = the email provider is down
- A message in `user.deleted.DLT` means one service could not clean up. Replay it after fixing the cause (cleanup is idempotent).
