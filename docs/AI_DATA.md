# What the AI sees and keeps

All AI runs on **our own servers** (Ollama, local models). No user data is sent to OpenAI, Google or any other AI company.

## Feature by feature

| Feature | What the AI reads | What we store | How long | Where |
|---|---|---|---|---|
| **Chat openers** | Both users' first name, bio, city, interests | The 3 suggested openers | 30 min (cache), and removed early when the match or account is deleted | Redis `ai:openers:<match>:<user>` |
| **Scam check** | The text of each chat message, **only for messages the rules flag** (most messages never reach the LLM) | Nothing from the message. Only a verdict (category, confidence) goes to safety-service | Screening topic: **24 h** (same as the chat) | Kafka `message.screening` |
| **Scam report** | – | A report with a copy of the recent chat as evidence, for a human moderator | Report retention (365 days) | safety-service (Mongo) |
| **Vibe matching** | Interests, bio, favourite places, prompt answers | 768 numbers per profile (an "embedding"), never shown to anyone. Only similarity scores leave ai-service | Until the profile changes or the **account is deleted** | ai-db `profile_embeddings` |

## Never used by the AI
Age, date of birth, gender, "looking for", photos, exact location, phone/email, DigiLocker data.

## Never logged
Message text, bios, prompts, openers and embeddings never go into logs (checked in Loki). Logs contain ids, scores and verdicts only.

## Decisions are human
The AI never bans, blocks or hides anyone. Scam suspicions become reports; a moderator decides.

## Play Store "Data safety" form: answers this supports
- **Data shared with third parties:** No (AI is self-hosted).
- **Messages:** collected; used for app functionality and safety (fraud prevention); deleted after 24 h (except reported conversations, kept as evidence).
- **App activity / profile info:** used for personalisation (matching).
- **Users can request deletion:** yes. `user.deleted` removes embeddings and cached openers (account deletion endpoint: to do before launch).
