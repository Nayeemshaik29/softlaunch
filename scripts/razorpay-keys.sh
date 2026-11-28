#!/usr/bin/env bash
# Puts Razorpay keys into an env file (git-ignored) and sets up the webhook secret.
#
#   scripts/razorpay-keys.sh            → .env (laptop: TEST keys; on the server: LIVE keys, confirm by typing LIVE)
#   scripts/razorpay-keys.sh <file>     → another git-ignored env file
#
# The secret is typed hidden and is never printed, logged or committed.
# Get the keys from: Razorpay Dashboard → Account & Settings → API Keys (Test or Live mode switch at the top).
set -euo pipefail

FILE="${1:-.env}"
cd "$(dirname "$0")/.."

if ! git check-ignore -q "$FILE"; then
  echo "✋ $FILE is not git-ignored. Refusing to write secrets into a file git could commit." >&2
  exit 1
fi

read -r -p "Razorpay Key ID (rzp_test_… or rzp_live_…): " KEY_ID
case "$KEY_ID" in
  rzp_test_*) MODE=TEST ;;
  rzp_live_*) MODE=LIVE ;;
  *) echo "✋ That doesn't look like a Razorpay Key ID." >&2; exit 1 ;;
esac

if [ "$MODE" = LIVE ] && [ "$FILE" = ".env" ]; then
  echo "⚠️  LIVE keys take real money. Only the production server should have them (your laptop uses TEST keys)."
  read -r -p "Is this the production server? Type LIVE to continue: " OK
  [ "$OK" = "LIVE" ] || { echo "Stopped. Nothing changed."; exit 1; }
fi

read -r -s -p "Razorpay Key Secret (hidden): " KEY_SECRET; echo
[ ${#KEY_SECRET} -ge 16 ] || { echo "✋ That secret looks too short." >&2; exit 1; }

touch "$FILE"
chmod 600 "$FILE"
# keep an existing webhook secret (it must match the one saved in Razorpay); otherwise make a new one
WEBHOOK_SECRET="$(grep -E '^RAZORPAY_WEBHOOK_SECRET=.+' "$FILE" | head -1 | cut -d= -f2- || true)"
NEW_WEBHOOK=0
if [ -z "$WEBHOOK_SECRET" ] || [ "${2:-}" = "--new-webhook-secret" ]; then
  WEBHOOK_SECRET="$(openssl rand -hex 24)"; NEW_WEBHOOK=1
fi

tmp="$(mktemp)"
grep -vE '^(BILLING_PROVIDER|RAZORPAY_KEY_ID|RAZORPAY_KEY_SECRET|RAZORPAY_WEBHOOK_SECRET)=' "$FILE" > "$tmp" || true
{
  cat "$tmp"
  echo "BILLING_PROVIDER=razorpay"
  echo "RAZORPAY_KEY_ID=$KEY_ID"
  echo "RAZORPAY_KEY_SECRET=$KEY_SECRET"
  echo "RAZORPAY_WEBHOOK_SECRET=$WEBHOOK_SECRET"
} > "$FILE"
rm -f "$tmp"
unset KEY_SECRET

echo "✅ $MODE keys saved in $FILE"
if [ "$NEW_WEBHOOK" = 1 ]; then
  if command -v pbcopy >/dev/null; then
    printf '%s' "$WEBHOOK_SECRET" | pbcopy
    echo "📋 New webhook secret copied to your clipboard. Paste it as the webhook's Secret in Razorpay ($MODE mode)."
  else
    echo "🔑 New webhook secret (paste it as the webhook's Secret in Razorpay, $MODE mode): $WEBHOOK_SECRET"
  fi
else
  echo "Kept the existing webhook secret (it already matches Razorpay). Pass --new-webhook-secret to replace it."
fi
cat <<EOF

Razorpay webhook ($MODE mode) → Account & Settings → Webhooks → Add new webhook:
  URL:     https://<your API domain>/api/users/billing/webhook/razorpay
  Secret:  the webhook secret above
  Events:  payment.captured, order.paid

Then restart user-service:  docker compose up -d --no-deps --force-recreate user-service
EOF
