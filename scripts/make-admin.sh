#!/usr/bin/env bash
# Give an existing account admin access (the /admin dashboard), or take it away.
#   scripts/make-admin.sh you@example.com        -> admin on
#   scripts/make-admin.sh you@example.com off    -> admin off
# Admin access can ONLY be granted here (database access), never through the app or the API.
set -euo pipefail
email="${1:?usage: scripts/make-admin.sh you@example.com [off]}"
value=true
[ "${2:-}" = "off" ] && value=false
docker exec -i softlaunch-user-db psql -U softlaunch -d user_db -v ON_ERROR_STOP=1 -v email="$email" -v value="$value" -tA <<'SQL'
UPDATE users SET admin = :value WHERE email = lower(:'email') RETURNING display_name || ': admin=' || admin;
SQL
