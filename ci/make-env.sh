#!/bin/bash
# Prints a .env with brand-new random secrets. CI uses it for every run:
#   bash ci/make-env.sh > .env
# Nothing real is ever stored in GitHub, and each run's passwords die with the run's machine.
set -euo pipefail
rand() { openssl rand -hex "$1"; }   # hex: no characters that need escaping in YAML/URLs
cat <<EOF
JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')
S3_ACCESS_KEY=ci$(rand 8)
S3_SECRET_KEY=$(rand 24)
DB_PASSWORD=$(rand 24)
REDIS_PASSWORD=$(rand 24)
GRAFANA_ADMIN_PASSWORD=$(rand 16)
EOF
