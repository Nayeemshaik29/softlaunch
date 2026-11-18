# Play Console: "Data safety" form answers

Copy these into **Play Console → App content → Data safety**. Built from [DATA_INVENTORY.md](DATA_INVENTORY.md).
Google's terms: **collected** = sent off the phone to us. **Shared** = given to a *third party*. Service providers that work for us (hosting, storage, email) and transfers the user asks for do **not** count as sharing.

> ⚠️ Re-check before submitting if Phase 12 (Flutter app) adds **push notifications** (FCM device token → "Device or other IDs"),
> **crash reporting** (→ "Crash logs", "Diagnostics") or any analytics/ads SDK. Each one changes these answers *and* the privacy policy.

## Overview questions
| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all of the user data collected by your app encrypted in transit? | **Yes** (HTTPS only; see DEPLOY.md) |
| Do you provide a way for users to request that their data is deleted? | **Yes**: in the app, and `https://[FILL IN: api domain]/delete-account` |
| Privacy policy URL | `https://[FILL IN: api domain]/privacy` |
| Is any data shared with third parties? | **No** |

## Data types
Purposes: **AF** App functionality · **AM** Account management · **FP** Fraud prevention, security and compliance · **P** Personalization. We never use **Advertising or marketing** or **Analytics**.

| Category → type | Collected | Shared | Ephemeral? | Required? | Purposes |
|---|---|---|---|---|---|
| Location → **Approximate location** (city) | Yes | No | No | Required | AF, P |
| Location → **Precise location** (we store exact coordinates) | Yes | No | No | Required | AF, P |
| Personal info → **Name** | Yes | No | No | Required | AF, AM |
| Personal info → **Email address** | Yes | No | No | Required | AM |
| Personal info → **User IDs** (account id) | Yes | No | No | Required | AF, AM, FP |
| Personal info → **Sexual orientation** (gender + "interested in") | Yes | No | No | Required for matching | AF, P |
| Personal info → **Other info** (date of birth, gender, profile fields, ID-verification result) | Yes | No | No | DOB required; rest optional | AF, FP, P |
| Messages → **Other in-app messages** (chats) | Yes | No | No | Optional (only if you chat) | AF, FP |
| Photos and videos → **Photos** | Yes | No | No | Optional | AF |
| App activity → **App interactions** (likes, passes, matches, blocks, screenshot events) | Yes | No | No | Required | AF, P, FP |
| App activity → **Other user-generated content** (bio, prompts, reports) | Yes | No | No | Optional | AF, FP |
| Personal info → Phone number, address, race/ethnicity, political or religious beliefs | No | | | | |
| Financial info, Health and fitness, Audio, Files and docs, Calendar, Contacts, Web browsing, App info and performance, Device or other IDs | No | | | | |

Notes:
- **Precise location:** if we start storing location rounded to ~1 km (idea in DATA_INVENTORY.md), this becomes "Approximate location" only. That's a nicer answer for users.
- **Habits** (drinking, smoking, workout) are lifestyle profile fields, so we list them under "Other info" and not "Health and fitness". Ask a reviewer if unsure.
- **ID verification** happens through DigiLocker at the user's request. We receive the result; we don't send their data to anyone, so it isn't sharing.

## Other Play Console declarations to check
- **Target audience:** 18+ only.
- **Dating app policy:** in-app reporting and blocking exist (safety-service); see Play's "Dating apps" requirements.
- **Account deletion:** both the in-app path and the web URL above.
