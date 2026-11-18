#!/usr/bin/env python3
"""
End-to-end smoke test: talks to the WHOLE system through the gateway, like the phone app would.
Creates its own users every run (random emails), so it works on an empty database:
CI runs it after `docker compose --profile all up`. You can run it on your Mac too (wait 2 min between runs: the login rate limiter counts them):

    python3 ci/smoke_test.py

Standard library only (no pip install). Exits 1 if any check fails.
No Ollama in CI: AI features must answer with their fallbacks (that's what we check).
"""
import json
import re
import os
import struct
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
import zlib

GW = os.environ.get("GATEWAY_URL", "http://localhost:8080")
PROM = os.environ.get("PROMETHEUS_URL", "http://localhost:9090")
MAILPIT = os.environ.get("MAILPIT_URL", "http://localhost:8025")   # fake inbox: catches every email
PASSWORD = "ci-" + uuid.uuid4().hex   # throwaway test password, new every run
RUN = uuid.uuid4().hex[:8]
failures = []


def call(method, url, body=None, tok=None, raw=None, headers=None, timeout=30):
    """503 = a service just started and the others don't know its address yet (Eureka refreshes every
    30 s). The request never reached the service, so retrying is safe. Give it up to a minute."""
    for _ in range(12):
        status, data = _call(method, url, body, tok, raw, headers, timeout)
        if status != 503 or not url.startswith(GW):
            break
        time.sleep(5)
    return status, data


def _call(method, url, body, tok, raw, headers, timeout):
    h = dict(headers or {})
    if body is not None:
        h["Content-Type"] = "application/json"
    if tok:
        h["Authorization"] = "Bearer " + tok
    data = json.dumps(body).encode() if body is not None else raw
    req = urllib.request.Request(url, method=method, data=data, headers=h)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()
    except (urllib.error.URLError, TimeoutError, ConnectionError) as e:
        return 0, str(e).encode()


def js(b):
    try:
        return json.loads(b)
    except (ValueError, TypeError):
        return {}


def check(name, cond, detail=""):
    print(("✅" if cond else "❌"), name, ("— " + str(detail) if detail else ""), flush=True)
    if not cond:
        failures.append(name)
    return cond


def eventually(fn, seconds=90, every=5):
    """Kafka and Prometheus are asynchronous: retry until fn() returns something truthy."""
    deadline = time.time() + seconds
    while True:
        result = fn()
        if result or time.time() > deadline:
            return result
        time.sleep(every)


def prom(query):
    q = urllib.parse.urlencode({"query": query})
    s, b = call("GET", f"{PROM}/api/v1/query?{q}")
    r = js(b).get("data", {}).get("result", []) if s == 200 else []
    return sum(float(x["value"][1]) for x in r)


def tiny_png(w=40, h=30):
    """A real PNG made with zlib only (no Pillow needed)."""
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
    rows = b"".join(b"\x00" + bytes(v for x in range(w) for v in (x * 6 % 256, y * 8 % 256, 150)) for y in range(h))
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(rows)) + chunk(b"IEND", b""))


def new_user(name, dob, gender, likes, interests):
    email = f"{name.lower()}-{RUN}@ci.test"
    s, b = call("POST", f"{GW}/api/users/auth/signup",
                {"email": email, "password": PASSWORD, "displayName": name, "dateOfBirth": dob})
    check(f"signup {name}", s in (200, 201), f"HTTP {s}")
    s, b = call("POST", f"{GW}/api/users/auth/login", {"email": email, "password": PASSWORD})
    tok = js(b).get("accessToken")
    check(f"login {name} (JWT from user-service)", s == 200 and tok, f"HTTP {s}")
    s, b = call("PUT", f"{GW}/api/users/me/profile", {
        "bio": f"{name} from the CI robot. Loves weekend treks and filter coffee.",
        "gender": gender, "interestedIn": [likes], "lookingFor": ["LONG_TERM"],
        "interests": interests, "city": "Bengaluru", "latitude": 12.9716, "longitude": 77.5946,
        "ageMin": 21, "ageMax": 40, "maxDistanceKm": 50}, tok=tok)
    check(f"profile {name} (user-db)", s == 200, f"HTTP {s} {b[:120]!r}" if s != 200 else "")
    s, b = call("PUT", f"{GW}/api/discovery/me/location", {"latitude": 12.9716, "longitude": 77.5946}, tok=tok)
    check(f"location {name} (discovery → Redis GEO)", s in (200, 204), f"HTTP {s}")
    s, b = call("GET", f"{GW}/api/users/me", tok=tok)
    return tok, js(b).get("id")


# ---------- 0. wait for the gateway ----------
# (not with logins: the auth rate limiter allows ~10 per 2 min per IP, and this test needs 5 of them)
print(f"Smoke test against {GW} (run {RUN})", flush=True)
ready = eventually(lambda: call("GET", f"{GW}/api/users/me")[0] == 401, seconds=180)
if not check("gateway is up", ready):
    sys.exit(1)

# ---------- 1. users ----------
A, a_id = new_user("Asha", "1998-05-10", "WOMAN", "MAN", ["HIKING", "COFFEE", "MUSIC"])
B, b_id = new_user("Bharat", "1996-03-02", "MAN", "WOMAN", ["HIKING", "COFFEE", "GAMING"])
if not (A and B and a_id and b_id):
    print("Cannot continue without two users")
    sys.exit(1)

# ---------- 2. security basics ----------
s, _ = call("GET", f"{GW}/api/users/me")
check("no token → 401 at the gateway", s == 401, f"HTTP {s}")
s, _ = call("GET", f"{GW}/api/users/me", tok=A[:-4] + "AAAA")
check("tampered token → 401", s == 401, f"HTTP {s}")
s, _ = call("POST", f"{GW}/api/users/auth/login", {"email": f"asha-{RUN}@ci.test", "password": "not-the-password"})
check("wrong password → 401", s == 401, f"HTTP {s}")

# ---------- 3. feed ----------
def feed_has_bharat():
    s, b = call("GET", f"{GW}/api/discovery/feed", tok=A)
    cards = js(b) if s == 200 else []
    return next((c for c in cards if c.get("userId") == b_id), None) if isinstance(cards, list) else None
card = eventually(feed_has_bharat, seconds=60)
check("feed shows Bharat to Asha (discovery → gRPC match + Feign user)", card,
      f"score {card['score']}, shared {card['sharedInterests']}" if card else "not in feed")

# ---------- 4. match ----------
s, b = call("POST", f"{GW}/api/matches/swipes", {"targetUserId": b_id, "direction": "LIKE"}, tok=A)
check("Asha likes Bharat", s in (200, 201) and not js(b).get("matched"), f"HTTP {s}")
s, b = call("POST", f"{GW}/api/matches/swipes", {"targetUserId": a_id, "direction": "LIKE"}, tok=B)
match_id = js(b).get("matchId")
check("Bharat likes back → it's a match", s in (200, 201) and js(b).get("matched") and match_id, f"HTTP {s}")
if not match_id:
    sys.exit(1)

# ---------- 5. AI without Ollama: fallbacks, never errors ----------
s, b = call("GET", f"{GW}/api/ai/matches/{match_id}/openers", tok=A, timeout=40)
o = js(b)
IN_CI = os.environ.get("CI") == "true"   # GitHub sets CI=true; there is no Ollama there
check("AI openers " + ("fall back to templates (no Ollama in CI)" if IN_CI else "answer"),
      s == 200 and len(o.get("openers", [])) >= 1 and (o.get("source") != "ai" or not IN_CI), f"HTTP {s}, source={o.get('source')}")

# ---------- 6. chat + Kafka ----------
s, _ = call("POST", f"{GW}/api/chats/{match_id}/messages", {"text": "Hi Asha! Which trek is next? 🥾"}, tok=B)
check("chat message (Mongo)", s == 201, f"HTTP {s}")
s, b = call("GET", f"{GW}/api/chats/{match_id}/messages", tok=A)
check("Asha reads it", s == 200 and "trek" in json.dumps(js(b)), f"HTTP {s}")
s, _ = call("GET", f"{GW}/api/chats/{match_id}/messages", tok=None)
check("chat without token → 401", s == 401, f"HTTP {s}")

SCAM_FOUND = 'sum(softlaunch_scam_verdicts_total{verdict=~"scam_.*"})'   # scam_ai, scam_rules_only, scam_rules_override
before = prom(SCAM_FOUND)
s, _ = call("POST", f"{GW}/api/chats/{match_id}/messages",
            {"text": "Urgent!! I'm stuck at the hospital, please send 5000 to rahul.k@okaxis"}, tok=B)
check("scam-like message accepted by chat", s == 201, f"HTTP {s}")
caught = eventually(lambda: prom(SCAM_FOUND) > before, seconds=120)
check("chat → Kafka → ai-service flags the scam" + (" (rules only, LLM down)" if IN_CI else ""), caught)

notes = eventually(lambda: (lambda r: r[0] == 200 and len(js(r[1])) >= 1)(call("GET", f"{GW}/api/notifications", tok=A)), seconds=60)
check("match notification via Kafka (notification-service)", notes)

# ---------- 7. photos ----------
png = tiny_png()
s, b = call("POST", f"{GW}/api/users/me/photos/upload-url", {"contentType": "image/png", "sizeBytes": len(png)}, tok=A)
ticket = js(b)
check("photo upload link", s == 201 and ticket.get("uploadUrl"), f"HTTP {s}")
if ticket.get("uploadUrl"):
    s, _ = call("PUT", ticket["uploadUrl"], raw=png, headers={"Content-Type": "image/png"})
    check("upload straight to storage (RustFS)", s == 200, f"HTTP {s}")
    s, b = call("POST", f"{GW}/api/users/me/photos/{ticket['photoId']}/confirm", tok=A)
    view = js(b).get("url", "")
    check("confirm: decoded, cleaned, stored as JPEG", s == 200 and view, f"HTTP {s}")
    s, img = call("GET", view) if view else (0, b"")
    check("signed view link returns a JPEG", s == 200 and img[:2] == b"\xff\xd8", f"HTTP {s}")
s, b = call("POST", f"{GW}/api/users/me/photos/upload-url", {"contentType": "image/gif", "sizeBytes": 100}, tok=A)
check("GIF refused", s == 400, f"HTTP {s}")

# ---------- 8. account deletion (Play Store requirement) ----------
s, _ = call("DELETE", f"{GW}/api/users/me", {"password": "not-my-password"}, tok=B)
check("delete account with wrong password → 401", s == 401, f"HTTP {s}")
s, _ = call("DELETE", f"{GW}/api/users/me", {"password": PASSWORD}, tok=B)
check("Bharat deletes his account", s == 204, f"HTTP {s}")
s, _ = call("GET", f"{GW}/api/users/me", tok=B)
check("his token stops working immediately (gateway blocklist)", s == 401, f"HTTP {s}")
s, _ = call("POST", f"{GW}/api/users/auth/login", {"email": f"bharat-{RUN}@ci.test", "password": PASSWORD})
check("he can't log in any more", s == 401, f"HTTP {s}")
s, _ = call("GET", f"{GW}/api/users/{b_id}/profile", tok=A)
check("his profile is gone", s == 404, f"HTTP {s}")
gone = eventually(lambda: (lambda r: r[0] == 200 and all(m.get("matchId", m.get("id")) != match_id for m in js(r[1])))(
    call("GET", f"{GW}/api/matches", tok=A)), seconds=60)
check("user.deleted → match removed for Asha (match-service)", gone)
chat_gone = eventually(lambda: call("GET", f"{GW}/api/chats/{match_id}/messages", tok=A)[0] in (403, 404), seconds=60)
check("→ match.removed → chat closed (chat-service)", chat_gone)
notes_gone = eventually(lambda: (lambda r: r[0] == 200 and len(js(r[1])) == 0)(call("GET", f"{GW}/api/notifications", tok=A)), seconds=60)
check("→ match notification removed (notification-service)", notes_gone)
s, _ = call("GET", f"{GW}/api/users/me", tok=A)
check("Asha is unaffected", s == 200, f"HTTP {s}")

# ---------- 8b. deletion from the WEB page (for people without the app) ----------
with urllib.request.urlopen(f"{GW}/delete-account", timeout=30) as r:
    page, csp = r.read().decode(), r.headers.get("Content-Security-Policy", "")
check("deletion page is served by the gateway", "Delete your account" in page)
check("page has a strict Content-Security-Policy", "default-src 'none'" in csp and "frame-ancestors 'none'" in csp)

asha_email = f"asha-{RUN}@ci.test"
nobody_email = f"nobody-{RUN}@ci.test"
DEL = f"{GW}/api/users/auth/deletion-requests"
s1, b1 = call("POST", DEL, {"email": nobody_email})
s2, b2 = call("POST", DEL, {"email": asha_email.upper()})   # upper case: must be treated like at login
check("same answer for an unknown and a real email (no account guessing)", s1 == s2 == 202 and b1 == b2, f"HTTP {s1}/{s2}")


def inbox(to):
    q = urllib.parse.urlencode({"query": f'to:"{to}"'})
    s, b = call("GET", f"{MAILPIT}/api/v1/search?{q}")
    return js(b).get("messages", []) if s == 200 else []


mails = eventually(lambda: inbox(asha_email), seconds=30, every=2)
check("deletion link emailed to Asha (Mailpit)", len(mails or []) == 1, f"{len(mails or [])} email(s)")
token = None
if mails:
    s, b = call("GET", f"{MAILPIT}/api/v1/message/{mails[0]['ID']}")
    m = re.search(r"/delete-account#token=([A-Za-z0-9_-]{43})", js(b).get("Text", ""))
    token = m.group(1) if m else None
check("email holds a one-time link (token after #)", token is not None)
call("POST", DEL, {"email": asha_email})
time.sleep(3)
check("asking again within 10 min sends no 2nd email", len(inbox(asha_email)) == 1)
check("unknown email got nothing", len(inbox(nobody_email)) == 0)

s, _ = call("POST", f"{DEL}/confirm", {"token": "A" * 43})
check("made-up link → 400", s == 400, f"HTTP {s}")
s, _ = call("POST", f"{DEL}/confirm", {"token": token or "x"})
check("Asha confirms on the page → deleted", s == 204, f"HTTP {s}")
s, _ = call("POST", f"{DEL}/confirm", {"token": token or "x"})
check("the same link can't be used twice", s == 400, f"HTTP {s}")
s, _ = call("GET", f"{GW}/api/users/me", tok=A)
check("her app token stops working", s == 401, f"HTTP {s}")
s, _ = call("POST", f"{GW}/api/users/auth/login", {"email": asha_email, "password": PASSWORD})
check("she can't log in any more", s == 401, f"HTTP {s}")

# ---------- 9. monitoring ----------
def all_targets_up():
    s, b = call("GET", f"{PROM}/api/v1/targets?state=active")
    t = js(b).get("data", {}).get("activeTargets", [])
    return t if t and all(x["health"] == "up" for x in t) else None
targets = eventually(all_targets_up, seconds=60)
check("Prometheus scrapes every service", targets, f"{len(targets)} targets up" if targets else "some targets down")

print()
if failures:
    print(f"❌ {len(failures)} check(s) failed: {', '.join(failures)}")
    sys.exit(1)
print("✅ all smoke checks passed")
