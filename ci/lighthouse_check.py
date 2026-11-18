#!/usr/bin/env python3
"""
Lighthouse gate for the web app (CI). Runs Lighthouse 3x per page (mobile profile) and checks the MEDIAN
score of every category. Shared CI machines are noisy, so one slow run must not fail the build.

    python3 ci/lighthouse_check.py http://localhost:3031 / /login /signup

Needs `lighthouse` on PATH and Chrome installed (GitHub's ubuntu runners have both after `npm i -g lighthouse`).
Reports are written to web/lighthouse/ (uploaded as a CI artifact).
"""
import json
import os
import statistics
import subprocess
import sys

MIN_SCORE = float(os.environ.get("LIGHTHOUSE_MIN", "0.9"))
RUNS = int(os.environ.get("LIGHTHOUSE_RUNS", "3"))
CATEGORIES = ["performance", "accessibility", "best-practices", "seo"]
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "web", "lighthouse")


def run(url, path):
    subprocess.run(
        ["lighthouse", url, "--quiet", "--output=json", f"--output-path={path}",
         f"--only-categories={','.join(CATEGORIES)}",
         "--chrome-flags=--headless=new --no-sandbox --disable-gpu"],
        check=True, timeout=180)
    with open(path) as f:
        return {c: v["score"] for c, v in json.load(f)["categories"].items()}


def main():
    base, pages = sys.argv[1].rstrip("/"), sys.argv[2:] or ["/"]
    os.makedirs(OUT, exist_ok=True)
    failed = []
    for page in pages:
        name = page.strip("/") or "home"
        scores = [run(base + page, os.path.join(OUT, f"{name}-{i}.json")) for i in range(RUNS)]
        medians = {c: statistics.median(s[c] for s in scores) for c in CATEGORIES}
        line = "  ".join(f"{c} {round(m * 100)}" for c, m in medians.items())
        bad = [c for c, m in medians.items() if m < MIN_SCORE]
        print(f"{'✅' if not bad else '❌'} {page:<10} {line}", flush=True)
        failed += [f"{page} {c}" for c in bad]
    if failed:
        print(f"\nBelow {round(MIN_SCORE * 100)}: {', '.join(failed)}")
        sys.exit(1)
    print(f"\nAll pages ≥ {round(MIN_SCORE * 100)} in every category (median of {RUNS} runs).")


if __name__ == "__main__":
    main()
