#!/usr/bin/env python3
"""
Local demo data for the web app (NEVER run against production).

Creates one demo login + 8 people nearby in Bengaluru, each with 2 generated abstract photos
(no real faces). 3 of them have already liked the demo user, so liking them back shows the match moment.
The demo login is written to web/.demo-login (git-ignored), not printed.

    python3 web/scripts/seed_demo.py            # gateway at http://localhost:8080
"""
import json
import os
import random
import struct
import sys
import time
import urllib.error
import urllib.request
import uuid
import zlib

GW = os.environ.get("GATEWAY_URL", "http://localhost:8080")
RUN = uuid.uuid4().hex[:6]
HERE = os.path.dirname(os.path.abspath(__file__))


def call(method, url, body=None, tok=None, raw=None, headers=None):
    for attempt in range(40):
        h = dict(headers or {})
        if body is not None:
            h["Content-Type"] = "application/json"
        if tok:
            h["Authorization"] = "Bearer " + tok
        data = json.dumps(body).encode() if body is not None else raw
        req = urllib.request.Request(url, method=method, data=data, headers=h)
        try:
            with urllib.request.urlopen(req, timeout=30) as r:
                return r.status, r.read()
        except urllib.error.HTTPError as e:
            body = e.read()
            # retry only the gateway's rate limit (429 with no body) and Eureka warm-up (503);
            # a 429 WITH a body is a real answer (e.g. SWIPE_QUOTA / SUPER_LIKE_QUOTA)
            if url.startswith(GW) and ((e.code == 429 and not body) or e.code == 503):
                time.sleep(13 if e.code == 429 else 5)
                continue
            return e.code, body
    raise SystemExit(f"gave up on {url}")


def png(w, h, seed):
    """Abstract warm portrait: gradient + soft blobs. Pure stdlib PNG encoder."""
    rnd = random.Random(seed)
    pal = [(255, 138, 107), (183, 155, 255), (246, 111, 154), (108, 184, 255), (255, 179, 107), (79, 214, 156)]
    a, b = rnd.sample(pal, 2)
    blobs = [(rnd.uniform(0.2, 0.8) * w, rnd.uniform(0.2, 0.8) * h, rnd.uniform(0.15, 0.35) * w, rnd.choice(pal)) for _ in range(3)]
    rows = bytearray()
    for y in range(h):
        rows.append(0)
        t = y / h
        for x in range(w):
            r = a[0] * (1 - t) + b[0] * t * 0.6
            g = a[1] * (1 - t) + b[1] * t * 0.6
            bl = a[2] * (1 - t) + b[2] * t * 0.6
            for bx, by, br, c in blobs:
                d = ((x - bx) ** 2 + (y - by) ** 2) / (br * br)
                if d < 1:
                    k = (1 - d) ** 2 * 0.55
                    r, g, bl = r * (1 - k) + c[0] * k, g * (1 - k) + c[1] * k, bl * (1 - k) + c[2] * k
            rows += bytes((int(r) & 255, int(g) & 255, int(bl) & 255))

    def chunk(t, d):
        return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)

    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(bytes(rows), 6)) + chunk(b"IEND", b""))


def make_user(name, dob, gender, likes, interests, bio, lat, lon, photos=2):
    email = f"{name.lower()}-{RUN}@demo.test"
    password = "demo-" + uuid.uuid4().hex[:12]
    s, b = call("POST", f"{GW}/api/users/auth/signup", {"email": email, "password": password, "displayName": name, "dateOfBirth": dob})
    assert s in (200, 201), (s, b[:200])
    s, b = call("POST", f"{GW}/api/users/auth/login", {"email": email, "password": password})
    tok = json.loads(b)["accessToken"]
    s, b = call("PUT", f"{GW}/api/users/me/profile", {
        "bio": bio, "gender": gender, "interestedIn": [likes], "lookingFor": ["LONG_TERM", "LONG_TERM_OPEN_TO_SHORT"],
        "interests": interests, "city": "Bengaluru", "latitude": lat, "longitude": lon,
        "ageMin": 21, "ageMax": 40, "maxDistanceKm": 50, "hangoutPlaces": ["Cubbon Park"], "jobTitle": "Designer"}, tok=tok)
    assert s == 200, (s, b[:300])
    call("PUT", f"{GW}/api/discovery/me/location", {"latitude": lat, "longitude": lon}, tok=tok)
    for k in range(photos):
        img = png(240, 320, f"{name}-{k}")
        s, b = call("POST", f"{GW}/api/users/me/photos/upload-url", {"contentType": "image/png", "sizeBytes": len(img)}, tok=tok)
        ticket = json.loads(b)
        call("PUT", ticket["uploadUrl"], raw=img, headers=ticket["headers"])
        s, b = call("POST", f"{GW}/api/users/me/photos/{ticket['photoId']}/confirm", tok=tok)
        assert s == 200, (s, b[:200])
    s, b = call("GET", f"{GW}/api/users/me", tok=tok)
    print(f"  {name}: ready", flush=True)
    return {"email": email, "password": password, "token": tok, "id": json.loads(b)["id"]}


def main():
    print(f"Seeding demo users against {GW} (run {RUN}). Takes ~3 min (login rate limit).", flush=True)
    me = make_user("Meera", "1997-04-12", "WOMAN", "MAN", ["HIKING", "COFFEE", "READING", "MUSIC"],
                   "Weekend treks, filter coffee, and a book I'll never finish.", 12.9716, 77.5946)
    people = [
        ("Arjun", "1995-02-11", ["HIKING", "COFFEE", "PHOTOGRAPHY"], "Sunrise hikes and bad puns. I make a decent dosa."),
        ("Kabir", "1996-07-23", ["MUSIC", "READING", "COFFEE"], "Plays guitar badly, reads poetry well."),
        ("Rohan", "1994-11-02", ["CRICKET", "FOODIE", "TRAVEL"], "Will plan the trip. You pick the food."),
        ("Vikram", "1993-05-19", ["YOGA", "MEDITATION", "HIKING"], "Early mornings, slow evenings."),
        ("Dev", "1998-01-30", ["GAMING", "ANIME", "TECH"], "Builds keyboards. Will explain why. Briefly."),
        ("Aman", "1995-09-14", ["COOKING", "COFFEE", "ART"], "Home cook, gallery wanderer, Sunday market regular."),
        ("Ishaan", "1997-03-08", ["DANCING", "MUSIC", "TRAVEL"], "Salsa on Thursdays. Beginners welcome."),
        ("Nikhil", "1996-12-21", ["READING", "HIKING", "PETS"], "Two cats, one tent, too many books."),
    ]
    others = []
    for i, (name, dob, interests, bio) in enumerate(people):
        others.append(make_user(name, dob, "MAN", "WOMAN", interests, bio, 12.9716 + 0.004 * (i + 1), 77.5946 + 0.003 * (i % 3)))
    for o in others[:3]:  # these three already like Meera: liking them back = match
        s, b = call("POST", f"{GW}/api/matches/swipes", {"targetUserId": me["id"], "direction": "LIKE"}, tok=o["token"])
        assert s in (200, 201), (s, b[:200])
    with open(os.path.join(HERE, "..", ".demo-login"), "w") as f:
        f.write(f"email={me['email']}\npassword={me['password']}\nalready_like_you={','.join(p[0] for p in people[:3])}\n")
    os.chmod(os.path.join(HERE, "..", ".demo-login"), 0o600)
    print("Done. Demo login saved to web/.demo-login (git-ignored).")


if __name__ == "__main__":
    sys.exit(main())
