# Going live

How to run SoftLaunch for real users. Start with **Option A**, and move pieces to managed services (Option B) when the app grows.

Measured on the full stack (11 services + infrastructure): **about 6.5 GB of RAM** when idle.

---

## Option A: one server with Docker Compose (recommended start)

The same `docker-compose.yml` you use locally, on one Linux server.

| | |
|---|---|
| **Server** | 4 vCPU, **16 GB RAM**, 100 GB SSD, Ubuntu 24.04 LTS. Pick a region close to your users (Mumbai for India). 8 GB works, but with no headroom. |
| **Cost** | roughly ₹1,500–7,000 / month depending on the provider (Hetzner, DigitalOcean, AWS Lightsail…). Check current prices. |
| **Good for** | launch and the first few thousand users |
| **Weak spot** | one machine: if it dies, the app is down until you restore. Backups are what make this safe. |

### 1. Server setup
1. Create the server with **SSH key login only** (turn off password login).
2. Firewall: allow **22** (only from your own IP), **80** and **443**. Nothing else. Postgres, Redis, Kafka and Grafana must never be reachable from the internet.
3. Install Docker Engine and the Compose plugin from Docker's official guide.
4. Turn on automatic security updates (`unattended-upgrades`).

### 2. Close the database ports
Locally, `docker-compose.yml` publishes database ports (`5433:5432`, `6379:6379`…) so you can debug from your Mac. On the server, add a `docker-compose.prod.yml` that removes them (or binds them to `127.0.0.1`), and publish **only** the gateway, behind HTTPS:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile all up -d
```

Docker bypasses the Ubuntu firewall (`ufw`) for published ports. That's why the ports must go, not just be blocked.

### 3. HTTPS
Put **Caddy** in front of the gateway. It gets and renews Let's Encrypt certificates on its own:

```
api.yourdomain.com {
    reverse_proxy localhost:8080
}
```

- Set the gateway's `trusted-proxies` to Caddy's address, so rate limits use the phone's IP and not Caddy's.
- The app must only talk HTTPS (Android blocks plain HTTP by default anyway).

### 4. Secrets
- Create a fresh `.env` **on the server** with new values (`openssl rand -base64 32`). Never reuse your laptop's values, never commit it, `chmod 600 .env`.
- DigiLocker: production client id/secret and the production redirect URL.
- **Email** (account-deletion links): Mailpit is for testing only, it sends nothing out. Use a real provider (Amazon SES in Mumbai, Brevo, …):
  - set `spring.mail.host`/`port` in `config-repo/user-service.yaml` and `MAIL_USERNAME` / `MAIL_PASSWORD` in `.env`
  - set `softlaunch.account-deletion.mail-from` to an address on your domain, and `page-url` to `https://api.yourdomain.com/delete-account`
  - add the provider's **SPF and DKIM** DNS records, or the emails land in spam
  - remove the `mailpit` service from the server's compose file

### 5. Photos
Use real **S3** (or any S3-compatible store), not RustFS on the same disk:

```yaml
softlaunch.photos.endpoint: https://s3.ap-south-1.amazonaws.com
softlaunch.photos.public-endpoint: https://s3.ap-south-1.amazonaws.com
softlaunch.photos.region: ap-south-1
```

- The bucket stays **private**. The app already uses expiring signed links.
- Use an IAM user that can only access that one bucket.

### 6. AI (Ollama)
- On a CPU-only server, `llama3.2:3b` is slow (several seconds per answer). The app still works: chat openers fall back to templates, and scam checks fall back to rules.
- Options:
  - Accept the fallbacks at launch.
  - Rent a small GPU server for Ollama only, and point `spring.ai.ollama.base-url` at it over a private network.
- Embeddings (`nomic-embed-text`) are fast enough on CPU.

### 7. Images: build once, run anywhere
1. Turn on publishing in CI: in the GitHub repo, go to **Settings → Secrets and variables → Actions → Variables** and add `PUBLISH_IMAGES` = `true`.
2. Every push to `main` that passes all tests then publishes `ghcr.io/nayeemshaik29/softlaunch-<service>:<commit>`.
3. On the server, point each service's `image:` at those tags, then run:

```bash
docker compose pull && docker compose --profile all up -d
```

Deploy a **commit id**, not `latest`, so you always know what's running and can roll back to the previous id.

### 8. Backups (do this before launch)
| What | How | How often |
|---|---|---|
| 5 Postgres DBs + ai-db | `pg_dump` from each container, gzip, upload to S3 (another region) | nightly |
| Mongo (chat, safety) | `mongodump --archive --gzip`, upload to S3 | nightly |
| Photos | S3 versioning + lifecycle rule | built in |
| `.env` | your password manager | on every change |

**Test a restore** on a spare machine once. A backup you have never restored is a guess.

### 9. Monitoring
- Grafana, Prometheus and Zipkin stay private. Reach them through an SSH tunnel: `ssh -L 3000:localhost:3000 server`.
- Point `alert-webhook` at something that wakes you up (email, Telegram, Slack).

---

## Option B: AWS managed services (when you grow)

Move the stateful parts first: they're the hardest to run yourself.

| Piece | Managed option | Notes |
|---|---|---|
| Postgres ×5 + pgvector | **RDS PostgreSQL** (one instance, 5 databases) | pgvector is supported on RDS; automatic backups and failover |
| Redis | **ElastiCache** | |
| Kafka | **MSK** (or MSK Serverless) | expensive at small scale; keep Kafka on a VM until you need it |
| Mongo | **MongoDB Atlas** (AWS Mumbai region) | DocumentDB is only partly Mongo-compatible |
| Photos | **S3** | already done in Option A |
| Services | **ECS Fargate** (one task per service) | Eureka/Config Server keep working; or move to ECS service discovery later |

- **Cost:** several times Option A. Managed databases are where the money goes.
- **What you get:** automatic backups, failover, and no database upgrades to do yourself.

---

## Play Store launch checklist

- [x] **Account deletion** endpoint: `DELETE /api/users/me` (see [DATA_DELETION.md](DATA_DELETION.md)).
- [x] **Web link** for deletion without the app: `https://api.yourdomain.com/delete-account`. Put this URL in the Play Console.
- [ ] **Email provider** for the deletion links (see section 4).
- [ ] Privacy policy URL (public web page).
- [ ] Data safety form: answers in [`AI_DATA.md`](AI_DATA.md) and [`DATA_DELETION.md`](DATA_DELETION.md), plus photos, location, DOB and KYC.
- [ ] India's DPDP Act: consent text, purpose of each data item, and a grievance contact.
- [ ] HTTPS everywhere; no database ports open (check with `nmap` from outside).
- [ ] Fresh production secrets; nothing from `.env.example` or your laptop.
- [ ] Backups running **and** one restore tested.
- [ ] Alerts reach your phone.
- [ ] Age gate 18+ (signup already enforces `@Adult`).
- [ ] Phone app converts HEIC photos to JPEG before upload (the server accepts JPEG/PNG only).
- [ ] CI green on the commit you deploy.
