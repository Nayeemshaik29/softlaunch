#!/usr/bin/env bash
# Renders the brand SVGs in this folder to 1024px PNGs (used by make_icons.py). Uses the Chrome already installed.
set -euo pipefail
cd "$(dirname "$0")"
CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
for name in icon-square peach-clean peach-full; do
  printf '<!doctype html><style>html,body{margin:0;background:transparent}img{display:block;width:1024px;height:1024px}</style><img src="%s.svg">' "$name" > ".render-$name.html"
  "$CHROME" --headless=new --disable-gpu --allow-file-access-from-files --hide-scrollbars \
    --default-background-color=00000000 --window-size=1024,1024 --screenshot="$PWD/$name.png" "file://$PWD/.render-$name.html" 2>/dev/null
  rm ".render-$name.html"
done
echo "Rendered: icon-square.png peach-clean.png peach-full.png"
