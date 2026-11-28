#!/usr/bin/env python3
"""
App icons + splash screens for the iOS and Android projects (Capacitor). Re-run after changing the design:

    python3 web/scripts/make_icons.py

- Main icon: the SoftLaunch mark (two circles, coral → violet) on near-black.
- Discreet icon (iOS alternate icon "discreet"): a plain neutral "SL". It must never imitate another app.
Needs Pillow (python3 -m pip install pillow).
"""
import json
import os
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
WEB = os.path.join(HERE, "..")
# Palette v2: Jet Black, Rich Amber Orange, Warm Peach Beige
BG = (11, 11, 12)
CORAL = (242, 140, 40)
VIOLET = (243, 217, 198)


def gradient(size, a, b):
    """Diagonal coral → violet gradient."""
    img = Image.new("RGB", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            t = (x + y) / (2 * (size - 1))
            px[x, y] = tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))
    return img


def mark(size, background=BG, scale=1.0):
    """The logo: a filled circle and an outlined circle, side by side."""
    s = size * 4  # draw big, shrink = smooth edges
    img = Image.new("RGBA", (s, s), background + (255,) if background else (0, 0, 0, 0))
    grad = gradient(s, CORAL, VIOLET)
    mask = Image.new("L", (s, s), 0)
    d = ImageDraw.Draw(mask)
    r = int(s * 0.19 * scale)
    cy = s // 2
    lx, rx = int(s * (0.5 - 0.155 * scale)), int(s * (0.5 + 0.155 * scale))
    d.ellipse([lx - r, cy - r, lx + r, cy + r], fill=255)                     # filled circle
    w = max(4, int(r * 0.22))
    d.ellipse([rx - r, cy - r, rx + r, cy + r], outline=255, width=w)         # ring
    img.paste(grad, (0, 0), mask)
    return img.resize((size, size), Image.LANCZOS)


BRAND = os.path.join(HERE, "brand")  # PNGs rendered from the SVGs by brand/render.sh


def brand(name, size):
    return Image.open(os.path.join(BRAND, name + ".png")).convert("RGBA").resize((size, size), Image.LANCZOS)


def ember(w, h, rays=24):
    """The 'Ember' sunburst: black rays on amber, centred a little above the middle."""
    s = 2  # supersample
    img = Image.new("RGB", (w * s, h * s), CORAL)
    d = ImageDraw.Draw(img)
    import math
    cx, cy, r = w * s / 2, h * s * 0.45, max(w, h) * s * 1.5
    for i in range(rays):
        a0 = 2 * math.pi * i / rays
        a1 = a0 + math.pi / rays
        d.polygon([(cx, cy), (cx + r * math.cos(a0), cy + r * math.sin(a0)), (cx + r * math.cos(a1), cy + r * math.sin(a1))], fill=BG)
    return img.resize((w, h), Image.LANCZOS)


def splash_img(w, h, logo_px):
    img = ember(w, h).convert("RGBA")
    logo = brand("peach-full", logo_px)  # full peach: the splash is inside the app
    img.alpha_composite(logo, (int((w - logo_px) / 2), int(h * 0.45 - logo_px / 2)))
    return img


def discreet(size):
    img = Image.new("RGB", (size, size), (46, 46, 50))
    d = ImageDraw.Draw(img)
    font = None
    for path in ("/System/Library/Fonts/Helvetica.ttc", "/System/Library/Fonts/SFNS.ttf", "/Library/Fonts/Arial.ttf"):
        if os.path.exists(path):
            font = ImageFont.truetype(path, int(size * 0.34))
            break
    font = font or ImageFont.load_default()
    d.text((size / 2, size / 2), "SL", fill=(205, 205, 210), font=font, anchor="mm")
    return img


def save(img, *parts):
    path = os.path.join(WEB, *parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.convert("RGB").save(path, "PNG")
    return path


def ios():
    base = os.path.join("ios", "App", "App", "Assets.xcassets")
    save(brand("icon-square", 1024), base, "AppIcon.appiconset", "AppIcon-512@2x.png")  # clean peach (store rule 2.3.8)
    save(discreet(1024), base, "discreet.appiconset", "discreet-1024.png")
    with open(os.path.join(WEB, base, "discreet.appiconset", "Contents.json"), "w") as f:
        json.dump({"images": [{"filename": "discreet-1024.png", "idiom": "universal", "platform": "ios", "size": "1024x1024"}],
                   "info": {"author": "xcode", "version": 1}}, f, indent=2)
    splash = splash_img(2732, 2732, 900)
    for name in ("splash-2732x2732.png", "splash-2732x2732-1.png", "splash-2732x2732-2.png"):
        save(splash, base, "Splash.imageset", name)


def android():
    res = os.path.join("android", "app", "src", "main", "res")
    for folder, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        icon = brand("icon-square", px)
        save(icon, res, f"mipmap-{folder}", "ic_launcher.png")
        round_icon = Image.new("RGBA", (px, px), (0, 0, 0, 0))
        m = Image.new("L", (px, px), 0)
        ImageDraw.Draw(m).ellipse([0, 0, px - 1, px - 1], fill=255)
        round_icon.paste(icon, (0, 0), m)
        round_icon.save(os.path.join(WEB, res, f"mipmap-{folder}", "ic_launcher_round.png"))
        fg = int(px * 108 / 48)  # adaptive icon foreground: 108dp canvas, logo in the middle 72dp
        fore = Image.new("RGBA", (fg, fg), (0, 0, 0, 0))
        p = int(fg * 0.56)  # peach inside the adaptive icon's safe zone
        fore.alpha_composite(brand("peach-clean", p), ((fg - p) // 2, (fg - p) // 2))
        fore.save(os.path.join(WEB, res, f"mipmap-{folder}", "ic_launcher_foreground.png"))
    with open(os.path.join(WEB, res, "values", "ic_launcher_background.xml"), "w") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_launcher_background">#F28C28</color>\n</resources>\n')
    splash = splash_img(1280, 1920, 560)
    land = splash_img(1920, 1280, 480)
    for density in ("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"):
        save(splash, res, f"drawable-port-{density}", "splash.png")
        save(land, res, f"drawable-land-{density}", "splash.png")
    save(splash, res, "drawable", "splash.png")


if __name__ == "__main__":
    ios()
    android()
    print("Icons and splash screens written for iOS and Android.")
